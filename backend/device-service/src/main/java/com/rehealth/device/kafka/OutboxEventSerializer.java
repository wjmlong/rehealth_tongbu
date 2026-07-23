package com.rehealth.device.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class OutboxEventSerializer {
    private static final String PERSISTED = "rehealth.telemetry.persisted.v1";
    private static final String QUALITY = "rehealth.telemetry.quality.v1";
    private static final Set<String> COMMON_FIELDS = Set.of(
            "event_type", "event_id", "batch_id", "schema_id", "tenant_ref", "user_ref",
            "device_ref", "window_started_at", "window_ended_at", "record_count",
            "quality_status", "persistence_status");
    private static final Set<String> QUALITY_FIELDS = Set.of("accepted_count", "rejected_count");

    private final ObjectMapper objectMapper;

    public OutboxEventSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SerializedOutboxEvent serialize(OutboxEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.metadataJson());
            if (root == null || !root.isObject()) {
                throw new OutboxContractException("outbox metadata must be a JSON object");
            }
            String eventType = requiredText(root, "event_type");
            if (!eventType.equals(event.eventType())
                    || !requiredText(root, "schema_id").equals(eventType)
                    || !requiredText(root, "event_id").equals(event.eventId().toString())
                    || event.eventVersion() != 1
                    || (!PERSISTED.equals(eventType) && !QUALITY.equals(eventType))) {
                throw new OutboxContractException("outbox event identity or schema is invalid");
            }
            Set<String> allowed = new HashSet<>(COMMON_FIELDS);
            if (QUALITY.equals(eventType)) {
                allowed.addAll(QUALITY_FIELDS);
                requiredCount(root, "accepted_count");
                requiredCount(root, "rejected_count");
            }
            Iterator<String> fields = root.fieldNames();
            while (fields.hasNext()) {
                if (!allowed.contains(fields.next())) {
                    throw new OutboxContractException("outbox event contains a forbidden field");
                }
            }
            requiredOpaque(root, "tenant_ref");
            requiredOpaque(root, "user_ref");
            String deviceKey = requiredOpaque(root, "device_ref");
            requiredText(root, "batch_id");
            requiredText(root, "window_started_at");
            requiredText(root, "window_ended_at");
            requiredText(root, "quality_status");
            requiredText(root, "persistence_status");
            requiredCount(root, "record_count");
            return new SerializedOutboxEvent(deviceKey, objectMapper.writeValueAsString(root));
        } catch (JsonProcessingException exception) {
            throw new OutboxContractException("outbox metadata is not valid JSON", exception);
        }
    }

    private String requiredText(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new OutboxContractException("outbox metadata field is missing or invalid: " + field);
        }
        return value.textValue();
    }

    private String requiredOpaque(JsonNode root, String field) {
        String value = requiredText(root, field);
        if (!value.matches("^opaque_[A-Za-z0-9_-]{8,128}$")) {
            throw new OutboxContractException("outbox reference is not opaque: " + field);
        }
        return value;
    }

    private void requiredCount(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isIntegralNumber() || value.longValue() < 0) {
            throw new OutboxContractException("outbox count is missing or invalid: " + field);
        }
    }
}
