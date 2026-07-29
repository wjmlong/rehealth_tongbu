package org.jeecg.modules.rehealth.miwi.pull;

import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.miwi.MiwiOpenApiClient;
import org.jeecg.modules.rehealth.miwi.MiwiProperties;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Pull connector: backend periodically fetches S8 (云米) history from the vendor
 * OpenAPI and writes it through the SAME {@link HardwareIngestionPort} pipeline the
 * app/BLE path uses. This is the recommended primary path for the 8 月初 launch
 * (the vendor push callback remains a supplementary realtime channel).
 *
 * Design notes (see REHEALTH_MIWI_4G_WATCH.md):
 * - one independent cursor per (device, metric) so a failing endpoint never blocks others;
 * - a backfill window re-covers late vendor uploads;
 * - each measurement carries a deterministic {@code client_record_id} so overlapping
 *   window re-pulls are idempotent at the row level (unique key on hardware_measurement).
 * - vendor API failures never affect the BLE upload path.
 */
@Service
@ConditionalOnProperty(name = "rehealth.miwi.pull.enabled", havingValue = "true")
public class S8PollingService {

    public static final String S8_SOURCE = "S8_CLOUD_PULL";
    private static final long FIRST_RUN_SEED_MILLIS = 24L * 60 * 60 * 1000; // backfill one day on first pull
    private static final ZoneId VENDOR_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Logger log = LoggerFactory.getLogger(S8PollingService.class);

    private final S8DeviceRegistry registry;
    private final S8SyncCursorRepository cursors;
    private final MiwiOpenApiClient openApi;
    private final S8Normalizer normalizer;
    private final MiwiProperties properties;
    private final ReHealthBusinessRepository businessRepository;
    private final HardwareIngestionPort ingestionPort;

    @Autowired
    public S8PollingService(
            S8DeviceRegistry registry,
            S8SyncCursorRepository cursors,
            MiwiOpenApiClient openApi,
            S8Normalizer normalizer,
            MiwiProperties properties,
            ReHealthBusinessRepository businessRepository,
            HardwareIngestionPort ingestionPort
    ) {
        this.registry = registry;
        this.cursors = cursors;
        this.openApi = openApi;
        this.normalizer = normalizer;
        this.properties = properties;
        this.businessRepository = businessRepository;
        this.ingestionPort = ingestionPort;
    }

    /** Pulls every active S8 device across every metric. Safe to call on a schedule. */
    public void pullAll() {
        List<S8DeviceRegistry.S8Device> devices = registry.findActiveDevices();
        if (devices.isEmpty()) {
            log.debug("miwi pull: no active S8 devices registered");
            return;
        }
        for (S8DeviceRegistry.S8Device device : devices) {
            try {
                pullDevice(device);
            } catch (Exception e) {
                log.error("miwi pull: device {} failed: {}", device.deviceId, e.getMessage());
            }
        }
    }

    /** Pulls a single metric across all bound active S8 devices (used by per-metric scheduler). */
    public void pullMetric(S8Metric metric) {
        for (S8DeviceRegistry.S8Device device : registry.findActiveDevices()) {
            try {
                Optional<String> userId = businessRepository.findActiveUserIdByDeviceId(device.deviceId);
                if (userId.isPresent()) {
                    pullMetric(device, userId.get(), metric);
                }
            } catch (Exception e) {
                log.error("miwi pull: {} for device {} failed: {}", metric.metricType(), device.deviceId, e.getMessage());
            }
        }
    }

    private void pullDevice(S8DeviceRegistry.S8Device device) {
        Optional<String> userId = businessRepository.findActiveUserIdByDeviceId(device.deviceId);
        if (userId.isEmpty()) {
            log.debug("miwi pull: device {} not bound to a user yet; skipping", device.deviceId);
            return;
        }
        for (S8Metric metric : S8Metric.all()) {
            pullMetric(device, userId.get(), metric);
        }
    }

    private void pullMetric(S8DeviceRegistry.S8Device device, String userId, S8Metric metric) {
        long now = System.currentTimeMillis();
        S8SyncCursorRepository.S8SyncCursor cursor =
                cursors.findByDeviceAndMetric(device.deviceId, metric.metricType())
                        .orElseGet(() -> S8SyncCursorRepository.S8SyncCursor.initial(
                                device.deviceId, metric.metricType()))
                        .withCursor(Math.min(now - FIRST_RUN_SEED_MILLIS, now));

        long from = cursor.cursorUtcMillis - (long) properties.getPullBackfillMinutes() * 60_000L;
        long to = now;

        try {
            Map<String, Object> response = openApi.post(metric.endpoint(), buildBody(device.imei, from, to));
            if (!isSuccess(response)) {
                String err = String.valueOf(response.getOrDefault("Msg", response.getOrDefault("msg", "vendor error")));
                markFailure(device, metric, err);
                return;
            }
            List<Map<String, Object>> measurements = normalizer.normalizeBytime(metric, response, to);
            if (measurements.isEmpty()) {
                advanceCursor(device, metric, to, 0, null);
                return;
            }
            assignDeterministicIds(device.deviceId, measurements);
            TelemetryBatchRequestDto batch = new TelemetryBatchRequestDto();
            batch.batchId = "s8-" + device.deviceId + "-" + metric.metricType() + "-" + to;
            batch.userId = userId;
            batch.deviceId = device.deviceId;
            batch.source = S8_SOURCE;
            batch.collectedFrom = from;
            batch.collectedTo = to;
            batch.measurements = measurements;
            ingestionPort.acceptBatch(batch);

            long maxMeasured = measurements.stream()
                    .mapToLong(m -> ((Number) m.get("measuredAt")).longValue())
                    .max().orElse(to);
            advanceCursor(device, metric, Math.max(maxMeasured, to), 0, null);
            log.info("miwi pull: {} {} -> {} measurements for {}", device.deviceId, metric.metricType(),
                    measurements.size(), userId);
        } catch (Exception e) {
            log.warn("miwi pull: {} {} failed: {}", device.deviceId, metric.metricType(), e.getMessage());
            markFailure(device, metric, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Map<String, Object> buildBody(String imei, long from, long to) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("imei", imei);
        body.put("startTime", formatVendorTime(from));
        body.put("endTime", formatVendorTime(to));
        return body;
    }

    private String formatVendorTime(long epochMillis) {
        String fmt = properties.getPullTimeFormat();
        if ("epoch_millis".equalsIgnoreCase(fmt)) {
            return String.valueOf(epochMillis);
        }
        if ("epoch_seconds".equalsIgnoreCase(fmt)) {
            return String.valueOf(epochMillis / 1000L);
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(fmt);
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), VENDOR_ZONE).format(formatter);
        } catch (RuntimeException e) {
            log.warn("miwi pull: invalid time-format '{}', falling back to epoch seconds", fmt);
            return String.valueOf(epochMillis / 1000L);
        }
    }

    private boolean isSuccess(Map<String, Object> response) {
        Object code = response.get("Code");
        if (code == null) {
            code = response.get("code");
        }
        if (code instanceof Number n) {
            return n.intValue() == 0;
        }
        return code != null && "0".equals(String.valueOf(code).trim());
    }

    private void assignDeterministicIds(String deviceId, List<Map<String, Object>> measurements) {
        for (Map<String, Object> m : measurements) {
            String metricType = String.valueOf(m.get("metricType"));
            long measuredAt = ((Number) m.get("measuredAt")).longValue();
            Object primary = m.get("primaryValue");
            Object secondary = m.get("secondaryValue");
            String raw = deviceId + "|" + S8_SOURCE + "|" + metricType + "|" + measuredAt
                    + "|" + primary + "|" + secondary;
            String hash = sha256Hex(raw);
            m.put("id", "s8-" + hash);
        }
    }

    private void advanceCursor(
            S8DeviceRegistry.S8Device device, S8Metric metric, long cursorUtc, int failureCount, String error
    ) {
        cursors.save(new S8SyncCursorRepository.S8SyncCursor(
                device.deviceId, metric.metricType(), cursorUtc, System.currentTimeMillis(), failureCount, error
        ));
    }

    private void markFailure(S8DeviceRegistry.S8Device device, S8Metric metric, String error) {
        S8SyncCursorRepository.S8SyncCursor existing =
                cursors.findByDeviceAndMetric(device.deviceId, metric.metricType())
                        .orElseGet(() -> S8SyncCursorRepository.S8SyncCursor.initial(device.deviceId, metric.metricType()));
        advanceCursor(device, metric, existing.cursorUtcMillis, existing.failureCount + 1,
                error == null ? null : error.substring(0, Math.min(error.length(), 255)));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("sha-256 unavailable", e);
        }
    }
}
