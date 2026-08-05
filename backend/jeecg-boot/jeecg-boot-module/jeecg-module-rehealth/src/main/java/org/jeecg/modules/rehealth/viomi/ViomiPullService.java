package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.ViomiSyncResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ViomiPullService {
    private static final Set<String> ALLOWED_METRICS = Set.of("HEART_RATE", "BLOOD_PRESSURE", "BLOOD_OXYGEN");
    private static final Duration MAX_WINDOW = Duration.ofDays(31);
    private static final DateTimeFormatter API_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId VIOMI_ZONE = ZoneId.of("Asia/Shanghai");
    private final ViomiOpenApiGateway gateway;
    private final ReHealthBusinessRepository businessRepository;
    private final HardwareIngestionPort hardwareIngestionPort;

    public ViomiPullService(
            ViomiOpenApiGateway gateway,
            ReHealthBusinessRepository businessRepository,
            HardwareIngestionPort hardwareIngestionPort
    ) {
        this.gateway = gateway;
        this.businessRepository = businessRepository;
        this.hardwareIngestionPort = hardwareIngestionPort;
    }

    public ViomiBindResponseDto bind(String userId, ViomiBindRequestDto request) {
        String imei = requireImei(request == null ? null : request.imei);
        if (!gateway.deviceExists(imei)) throw new IllegalArgumentException("IMEI is not available to the configured Viomi account");
        String deviceId = deviceId(imei);
        DeviceBindRequestDto binding = new DeviceBindRequestDto();
        binding.deviceId = deviceId;
        binding.deviceName = "Viomi cloud watch";
        binding.manufacturer = "VIOMI";
        binding.model = request.productCode;
        binding.hardwareAddressHash = sha256(imei);
        DeviceBindResponseDto saved = businessRepository.recordDeviceBinding(userId, binding);
        ViomiBindResponseDto response = new ViomiBindResponseDto();
        response.deviceId = deviceId;
        response.status = saved.status;
        response.persisted = saved.persisted;
        response.persistenceStage = saved.persistenceStage;
        return response;
    }

    public ViomiSyncResponseDto sync(String userId, ViomiSyncRequestDto request) {
        String imei = requireImei(request == null ? null : request.imei);
        String deviceId = deviceId(imei);
        if (!businessRepository.hasActiveDeviceBinding(userId, deviceId)) {
            throw new SecurityException("Viomi device is not bound to the current user");
        }
        Instant end = request.endAt == null ? Instant.now() : Instant.ofEpochMilli(request.endAt);
        Instant begin = request.beginAt == null ? end.minus(Duration.ofDays(7)) : Instant.ofEpochMilli(request.beginAt);
        if (!begin.isBefore(end) || Duration.between(begin, end).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException("Viomi sync window must be positive and no longer than 31 days");
        }
        Set<String> metrics = request.metrics == null || request.metrics.isEmpty()
                ? new LinkedHashSet<>(ALLOWED_METRICS) : new LinkedHashSet<>(request.metrics);
        if (!ALLOWED_METRICS.containsAll(metrics)) throw new IllegalArgumentException("unsupported Viomi metric requested");

        ViomiSyncResponseDto response = new ViomiSyncResponseDto();
        response.deviceId = deviceId;
        for (String metric : metrics) {
            for (JsonNode row : gateway.history(metric, imei, begin, end)) {
                ViomiSyncResponseDto.Measurement measurement = normalize(deviceId, metric, row);
                if (measurement != null) response.measurements.add(measurement);
            }
        }
        response.recordCount = response.measurements.size();
        if (response.measurements.isEmpty()) {
            response.status = "NO_NEW_DATA";
            response.persisted = true;
            return response;
        }

        TelemetryBatchRequestDto batch = new TelemetryBatchRequestDto();
        batch.schemaVersion = "telemetry-v2";
        batch.batchId = "viomi-" + sha256(deviceId + "|" + begin + "|" + end + "|" + metrics).substring(0, 32);
        batch.userId = userId;
        batch.deviceId = deviceId;
        batch.collectedFrom = begin.toEpochMilli();
        batch.collectedTo = end.toEpochMilli();
        batch.source = "viomi_cloud";
        for (ViomiSyncResponseDto.Measurement item : response.measurements) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("id", item.id);
            record.put("metricType", item.metricType);
            record.put("measuredAt", item.measuredAt);
            record.put("primaryValue", item.primaryValue);
            record.put("secondaryValue", item.secondaryValue);
            record.put("unit", item.unit);
            record.put("source", item.source);
            batch.measurements.add(record);
        }
        TelemetryBatchResponseDto receipt = hardwareIngestionPort.acceptBatch(batch);
        if (!receipt.accepted || !receipt.persisted) throw new IllegalStateException("Viomi telemetry was not durably persisted");
        response.status = receipt.status;
        response.persisted = true;
        return response;
    }

    private static ViomiSyncResponseDto.Measurement normalize(String deviceId, String metric, JsonNode row) {
        String timeField = switch (metric) {
            case "HEART_RATE" -> "HrTime";
            case "BLOOD_PRESSURE" -> "BpTime";
            default -> "BloodOxygenTime";
        };
        Instant measuredAt = parseTime(row.path(timeField).asText(null));
        if (measuredAt == null) return null;
        double primary;
        Double secondary = null;
        String unit;
        if ("HEART_RATE".equals(metric)) {
            primary = row.path("HeartRate").asDouble(Double.NaN);
            unit = "bpm";
        } else if ("BLOOD_PRESSURE".equals(metric)) {
            primary = row.path("Systolic").asDouble(Double.NaN);
            secondary = row.path("Diastolic").asDouble(Double.NaN);
            unit = "mmHg";
        } else {
            primary = row.path("BloodOxygen").asDouble(Double.NaN);
            unit = "%";
        }
        if (!Double.isFinite(primary) || (secondary != null && !Double.isFinite(secondary))) return null;
        if (!isPhysiologicallyPlausible(metric, primary, secondary)) return null;
        ViomiSyncResponseDto.Measurement out = new ViomiSyncResponseDto.Measurement();
        out.metricType = metric;
        out.measuredAt = measuredAt.toEpochMilli();
        out.primaryValue = primary;
        out.secondaryValue = secondary;
        out.unit = unit;
        out.source = "viomi_cloud";
        out.id = "viomi-" + sha256(deviceId + "|" + metric + "|" + out.measuredAt + "|" + primary + "|" + secondary).substring(0, 32);
        return out;
    }

    private static Instant parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            if (value.chars().allMatch(Character::isDigit)) {
                long numeric = Long.parseLong(value);
                return numeric > 10_000_000_000L ? Instant.ofEpochMilli(numeric) : Instant.ofEpochSecond(numeric);
            }
            return LocalDateTime.parse(value.replace('/', '-'), API_TIME).atZone(VIOMI_ZONE).toInstant();
        } catch (DateTimeParseException | NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean isPhysiologicallyPlausible(String metric, double primary, Double secondary) {
        return switch (metric) {
            case "HEART_RATE" -> primary >= 20.0 && primary <= 250.0;
            case "BLOOD_OXYGEN" -> primary >= 50.0 && primary <= 100.0;
            case "BLOOD_PRESSURE" -> secondary != null
                    && primary >= 50.0 && primary <= 260.0
                    && secondary >= 30.0 && secondary <= 180.0
                    && primary > secondary;
            default -> false;
        };
    }

    private static String requireImei(String value) {
        String imei = value == null ? "" : value.trim();
        if (!imei.matches("[0-9]{8,32}")) throw new IllegalArgumentException("IMEI must contain 8 to 32 digits");
        return imei;
    }

    private static String deviceId(String imei) {
        return "viomi-" + sha256(imei).substring(0, 24);
    }

    private static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
