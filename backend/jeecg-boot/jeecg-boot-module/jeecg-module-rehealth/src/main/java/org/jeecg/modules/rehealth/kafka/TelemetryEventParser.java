package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class TelemetryEventParser {
    private static final String PERSISTED = "rehealth.telemetry.persisted.v1";
    private static final String QUALITY = "rehealth.telemetry.quality.v1";
    private static final Set<String> COMMON_FIELDS = Set.of(
            "event_type", "event_id", "batch_id", "schema_id", "tenant_ref", "user_ref",
            "device_ref", "window_started_at", "window_ended_at", "record_count",
            "quality_status", "persistence_status");

    private final ObjectMapper objectMapper;

    public TelemetryEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TelemetryEvent parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new TelemetryEventContractException("event must be a JSON object");
            }
            String type = text(root, "event_type");
            if (!PERSISTED.equals(type) && !QUALITY.equals(type)) {
                throw new TelemetryEventContractException("unsupported telemetry schema");
            }
            if (!type.equals(text(root, "schema_id"))) {
                throw new TelemetryEventContractException("schema_id does not match event_type");
            }
            Set<String> allowed = new HashSet<>(COMMON_FIELDS);
            if (QUALITY.equals(type)) {
                allowed.add("accepted_count");
                allowed.add("rejected_count");
            }
            Iterator<String> fields = root.fieldNames();
            while (fields.hasNext()) {
                if (!allowed.contains(fields.next())) {
                    throw new TelemetryEventContractException("event contains a forbidden field");
                }
            }
            return new TelemetryEvent(
                    type, text(root, "event_id"), text(root, "batch_id"), text(root, "schema_id"),
                    opaque(root, "tenant_ref"), opaque(root, "user_ref"), opaque(root, "device_ref"),
                    Instant.parse(text(root, "window_ended_at")), count(root, "record_count"),
                    QUALITY.equals(type) ? count(root, "accepted_count") : 0,
                    QUALITY.equals(type) ? count(root, "rejected_count") : 0,
                    text(root, "quality_status"), text(root, "persistence_status"));
        } catch (JsonProcessingException | DateTimeException exception) {
            throw new TelemetryEventContractException("event JSON or timestamp is invalid", exception);
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new TelemetryEventContractException("event field is missing or invalid: " + field);
        }
        return value.textValue();
    }

    private String opaque(JsonNode root, String field) {
        String value = text(root, field);
        if (!value.matches("^opaque_[A-Za-z0-9_-]{8,128}$")) {
            throw new TelemetryEventContractException("event identity is not opaque: " + field);
        }
        return value;
    }

    private int count(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (value == null || !value.canConvertToInt() || value.intValue() < 0) {
            throw new TelemetryEventContractException("event count is missing or invalid: " + field);
        }
        return value.intValue();
    }
}
