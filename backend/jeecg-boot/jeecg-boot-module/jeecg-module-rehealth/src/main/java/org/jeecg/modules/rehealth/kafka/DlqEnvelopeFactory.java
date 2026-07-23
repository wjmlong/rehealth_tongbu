package org.jeecg.modules.rehealth.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DlqEnvelopeFactory {
    private static final String PERSISTED = "rehealth.telemetry.persisted.v1";
    private static final String QUALITY = "rehealth.telemetry.quality.v1";
    private final ObjectMapper objectMapper;

    public DlqEnvelopeFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String redacted(String payload) {
        JsonNode root = parse(payload);
        String sourceType = safeText(root, "event_type", PERSISTED);
        if (!PERSISTED.equals(sourceType) && !QUALITY.equals(sourceType)) {
            sourceType = PERSISTED;
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("event_type", "rehealth.telemetry.dlq.v1");
        envelope.put("event_id", "dlq_" + digest(payload));
        envelope.put("batch_id", safeText(root, "batch_id", "batch_unknown1"));
        envelope.put("schema_id", "rehealth.telemetry.dlq.v1");
        envelope.put("tenant_ref", safeOpaque(root, "tenant_ref", "opaque_unknown1"));
        envelope.put("user_ref", safeOpaque(root, "user_ref", "opaque_unknown1"));
        envelope.put("device_ref", safeOpaque(root, "device_ref", "opaque_unknown1"));
        envelope.put("record_count", safeCount(root));
        envelope.put("source_event_type", sourceType);
        envelope.put("failure_code", "retry_exhausted");
        envelope.put("attempt_count", 3);
        envelope.put("persistence_status", "persisted");
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("DLQ metadata serialization failed", exception);
        }
    }

    private JsonNode parse(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String safeText(JsonNode root, String field, String fallback) {
        JsonNode value = root == null ? null : root.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()
                || value.textValue().length() > 128) {
            return fallback;
        }
        return value.textValue();
    }

    private String safeOpaque(JsonNode root, String field, String fallback) {
        String value = safeText(root, field, fallback);
        return value.matches("^opaque_[A-Za-z0-9_-]{8,128}$") ? value : fallback;
    }

    private int safeCount(JsonNode root) {
        JsonNode value = root == null ? null : root.get("record_count");
        return value != null && value.canConvertToInt() && value.intValue() >= 0
                ? value.intValue()
                : 0;
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
