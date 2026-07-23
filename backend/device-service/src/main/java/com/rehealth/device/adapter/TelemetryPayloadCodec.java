package com.rehealth.device.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehealth.device.application.DeviceRequestException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

final class TelemetryPayloadCodec {
    private final ObjectMapper objectMapper;

    TelemetryPayloadCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw unavailable();
        }
    }

    int rejectedCount(Map<String, Object> quality) {
        if (quality == null) {
            return 0;
        }
        Object value = quality.get("rejectedCount");
        return value instanceof Number number ? Math.max(number.intValue(), 0) : 0;
    }

    int rejectedCount(String qualityJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> quality = objectMapper.readValue(qualityJson, Map.class);
            return rejectedCount(quality);
        } catch (JsonProcessingException exception) {
            throw unavailable();
        }
    }

    String sourceId(String supplied, String batchId, String kind, int index) {
        if (supplied != null && !supplied.isBlank() && supplied.length() <= 128) {
            return supplied;
        }
        return digest((supplied == null ? "" : supplied) + ":" + batchId + ":" + kind + ":" + index);
    }

    String opaque(String value) {
        return "opaque_" + digest(value);
    }

    String digest(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    Timestamp timestamp(Long epochMillis) {
        return epochMillis == null ? null : Timestamp.from(Instant.ofEpochMilli(epochMillis));
    }

    Instant instant(Long epochMillis, Instant fallback) {
        return epochMillis == null ? fallback : Instant.ofEpochMilli(epochMillis);
    }

    int value(Integer value) {
        return value == null ? 0 : value;
    }

    double value(Double value) {
        return value == null ? 0 : value;
    }

    private DeviceRequestException unavailable() {
        return new DeviceRequestException(
                HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE");
    }
}
