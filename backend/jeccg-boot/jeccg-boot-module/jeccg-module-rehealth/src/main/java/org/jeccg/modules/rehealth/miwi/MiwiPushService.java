package org.jeecg.modules.rehealth.miwi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles Miwi (云米) vendor-cloud push messages:
 * unwraps the double-encoded JSON, resolves the bound ReHealth user by IMEI,
 * normalizes measurements, and feeds them through the standard hardware ingestion port.
 */
@Service
public class MiwiPushService {
    private static final Logger log = LoggerFactory.getLogger(MiwiPushService.class);

    private final MiwiProperties properties;
    private final MiwiHealthDataMapper mapper;
    private final HardwareIngestionPort ingestionPort;
    private final ReHealthBusinessRepository businessRepository;
    private final ObjectMapper objectMapper;

    public MiwiPushService(
            MiwiProperties properties,
            MiwiHealthDataMapper mapper,
            HardwareIngestionPort ingestionPort,
            ReHealthBusinessRepository businessRepository,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.mapper = mapper;
        this.ingestionPort = ingestionPort;
        this.businessRepository = businessRepository;
        this.objectMapper = objectMapper;
    }

    public MiwiPushResult handlePush(Map<String, Object> envelope) {
        if (envelope == null || envelope.isEmpty()) {
            return MiwiPushResult.rejected("empty push body");
        }
        String dataType = text(envelope, "DataType", "dataType");
        Map<String, Object> payload = unwrapResultData(envelope);
        if (payload == null) {
            return MiwiPushResult.rejected("ResultData missing or not parseable");
        }

        String imei = mapper.extractImei(payload);
        if (imei == null) {
            return MiwiPushResult.rejected("imei missing in push payload");
        }

        String deviceId = buildDeviceId(imei);
        Optional<String> userId = businessRepository.findActiveUserIdByDeviceId(deviceId);
        if (userId.isEmpty()) {
            // Ack to the vendor (avoid endless retries) but skip persistence for unbound devices.
            log.info("miwi push skipped: no BOUND user for deviceId hash suffix {}", suffix(deviceId));
            return MiwiPushResult.skippedUnbound(deviceId);
        }

        long receivedAt = Instant.now().toEpochMilli();
        long measuredAt = mapper.resolveMeasuredAt(payload, receivedAt);
        List<Map<String, Object>> measurements = mapper.toMeasurements(payload, measuredAt);
        if (measurements.isEmpty()) {
            return MiwiPushResult.skippedEmpty(deviceId, dataType);
        }

        TelemetryBatchRequestDto batch = new TelemetryBatchRequestDto();
        batch.batchId = "miwi-" + UUID.randomUUID();
        batch.userId = userId.get();
        batch.deviceId = deviceId;
        batch.source = "MIWI_4G_CLOUD";
        batch.collectedFrom = measuredAt;
        batch.collectedTo = measuredAt;
        batch.measurements = measurements;
        assignDeterministicIds(measurements, deviceId);
        batch.quality.put("vendor", "miwi");
        batch.quality.put("vendorDataType", dataType == null ? "unknown" : dataType);
        batch.quality.put("receivedAt", receivedAt);

        TelemetryBatchResponseDto response = ingestionPort.acceptBatch(batch);
        return MiwiPushResult.accepted(deviceId, measurements.size(), response);
    }

    /**
     * The vendor wraps the business payload as an escaped JSON string:
     * {@code {"DataType":"Health","ResultData":"{\"heartRate\":67,...}"}}.
     * Also tolerates the payload arriving as a proper JSON object.
     */
    private Map<String, Object> unwrapResultData(Map<String, Object> envelope) {
        Object resultData = envelope.get("ResultData");
        if (resultData == null) {
            resultData = envelope.get("resultData");
        }
        if (resultData == null) {
            // Some vendor messages put fields at top level directly.
            return envelope.containsKey("imei") || envelope.containsKey("IMEI") ? envelope : null;
        }
        if (resultData instanceof Map<?, ?> mapValue) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) mapValue;
            return typed;
        }
        String text = String.valueOf(resultData).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            log.warn("miwi push ResultData parse failed: {}", e.getMessage());
            return null;
        }
    }

    private String text(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }

    /**
     * Builds the ReHealth deviceId from the watch IMEI using the same rule as the
     * Android app ({@code RingCloudRepository}): vendor prefix + first 24 hex chars
     * of SHA-256(imei). This keeps callback lookups consistent with app-side binding.
     */
    private String buildDeviceId(String imei) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imei.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return properties.getDeviceIdPrefix() + hex.substring(0, 24);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }

    /** Only log a short suffix; never log full IMEI/identifiers in production logs. */
    private String suffix(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4).toLowerCase(Locale.ROOT);
    }

    /**
     * Assigns a deterministic, transport-scoped {@code client_record_id} to every measurement so that
     * a re-delivered vendor push (retry) collapses onto the same row via the
     * {@code UNIQUE(client_record_id)} index. The id is scoped by the push source
     * ({@code MIWI_4G_CLOUD}); pull vs push are intentionally kept as separate rows
     * ("分传输方式存"), so this segment differs from S8_CLOUD_PULL.
     */
    private void assignDeterministicIds(List<Map<String, Object>> measurements, String deviceId) {
        String transport = "MIWI_4G_CLOUD";
        for (Map<String, Object> m : measurements) {
            if (m == null || m.get("id") != null) {
                continue;
            }
            Object metricType = m.get("metricType");
            Object measuredAt = m.get("measuredAt");
            Object primary = m.get("primary");
            Object secondary = m.get("secondary");
            String raw = deviceId + "|" + transport + "|"
                    + metricType + "|" + measuredAt + "|" + primary + "|" + secondary;
            String id = "s8-" + com.google.common.hash.Hashing.sha256()
                    .hashString(raw, java.nio.charset.StandardCharsets.UTF_8).toString();
            m.put("id", id);
        }
    }

    public static final class MiwiPushResult {
        public final boolean accepted;
        public final String status;
        public final String deviceId;
        public final int measurementCount;
        public final TelemetryBatchResponseDto ingestResponse;

        private MiwiPushResult(
                boolean accepted,
                String status,
                String deviceId,
                int measurementCount,
                TelemetryBatchResponseDto ingestResponse
        ) {
            this.accepted = accepted;
            this.status = status;
            this.deviceId = deviceId;
            this.measurementCount = measurementCount;
            this.ingestResponse = ingestResponse;
        }

        public static MiwiPushResult accepted(String deviceId, int count, TelemetryBatchResponseDto response) {
            return new MiwiPushResult(true, "ACCEPTED", deviceId, count, response);
        }

        public static MiwiPushResult skippedUnbound(String deviceId) {
            return new MiwiPushResult(false, "SKIPPED_UNBOUND_DEVICE", deviceId, 0, null);
        }

        public static MiwiPushResult skippedEmpty(String deviceId, String dataType) {
            return new MiwiPushResult(false, "SKIPPED_NO_SUPPORTED_METRICS", deviceId, 0, null);
        }

        public static MiwiPushResult rejected(String reason) {
            return new MiwiPushResult(false, "REJECTED_" + reason.replace(' ', '_').toUpperCase(Locale.ROOT), null, 0, null);
        }
    }
}
