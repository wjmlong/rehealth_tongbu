package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a Viomi active-report payload to the canonical {@link TelemetryBatchRequestDto}.
 *
 * <p>Mapping summary (Viomi field -> ReHealth metricType):
 * <ul>
 *   <li>heartRate -> HEART_RATE (bpm)</li>
 *   <li>bloodOxygen -> SPO2 (%)</li>
 *   <li>bloodPressureMax/bloodPressureMin -> BLOOD_PRESSURE (mmHg, primary=systolic, secondary=diastolic)</li>
 *   <li>steps -> STEPS</li>
 *   <li>distance -> DISTANCE (m)</li>
 *   <li>calorie -> CALORIE (kcal)</li>
 *   <li>temperature -> BODY_TEMPERATURE (°C)</li>
 *   <li>step/roll (StepRoll) -> STEPS / ROLL</li>
 *   <li>battery (Location) -> DEVICE_BATTERY (%)</li>
 *   <li>deepSleep/lighSleep/totalSleep/sleepTime (Health) -> hardware_sleep_session</li>
 * </ul>
 * Empty/blank Viomi values are skipped so a partial payload still persists the available metrics.
 * </p>
 */
@Component
public class ViomiTelemetryMapper {

    static final String SOURCE = "viomi";

    private static final DateTimeFormatter VIOMI_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId VIOMI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {};

    private final ViomiAdapterProperties properties;
    private final ObjectMapper objectMapper;

    public ViomiTelemetryMapper(ViomiAdapterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    TelemetryBatchRequestDto toBatch(ViomiReportEnvelope envelope, String jwtImei) {
        String dataType = envelope.DataType == null ? "" : envelope.DataType.trim();
        Map<String, Object> payload = parseResultData(envelope.ResultData);

        TelemetryBatchRequestDto dto = new TelemetryBatchRequestDto();
        dto.userId = properties.getUserId();
        dto.deviceId = resolveDeviceId(envelope, payload, jwtImei);
        dto.source = properties.getSource();
        dto.batchId = buildBatchId(dto.deviceId, dataType, envelope, payload);
        dto.quality = buildQuality(envelope, payload, dataType);

        List<Map<String, Object>> measurements = new ArrayList<>();
        List<Map<String, Object>> sleepSessions = new ArrayList<>();
        long fallback = resolveEnvelopeTime(envelope);

        switch (dataType) {
            case "Health":
                mapHealth(payload, measurements, sleepSessions, fallback);
                break;
            case "StepRoll":
            case "StepRolls":
                mapStepRoll(payload, measurements, fallback);
                break;
            case "Temperature":
                mapTemperature(payload, measurements, fallback);
                break;
            case "Location":
                mapLocation(payload, measurements, fallback);
                break;
            default:
                break;
        }

        dto.measurements = measurements;
        dto.sleepSessions = sleepSessions;
        dto.activitySessions = new ArrayList<>();
        dto.signalChunks = new ArrayList<>();
        return dto;
    }

    private Map<String, Object> parseResultData(String resultData) {
        if (resultData == null || resultData.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(resultData, MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : map;
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private String resolveDeviceId(ViomiReportEnvelope envelope, Map<String, Object> payload, String jwtImei) {
        String imei = firstNonBlank(jwtImei, envelope.Imei, asText(payload.get("imei")));
        return imei == null ? "viomi-unknown" : imei;
    }

    private String buildBatchId(String deviceId, String dataType, ViomiReportEnvelope envelope,
                                Map<String, Object> payload) {
        String reqId = envelope.ReqId == null ? "" : envelope.ReqId.trim();
        String discriminator;
        if (!reqId.isEmpty()) {
            discriminator = reqId;
        } else {
            String normalized = deviceId + "|" + dataType + "|"
                    + (envelope.Time == null ? "" : envelope.Time) + "|" + safeString(envelope.ResultData);
            discriminator = shortHash(normalized);
        }
        return "viomi-" + deviceId + "-" + dataType + "-" + discriminator;
    }

    private Map<String, Object> buildQuality(ViomiReportEnvelope envelope, Map<String, Object> payload,
                                             String dataType) {
        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("adapter", "viomi");
        quality.put("dataType", dataType);
        putIfPresent(quality, "appId", properties.getAppId());
        putIfPresent(quality, "imei", firstNonBlank(envelope.Imei, asText(payload.get("imei"))));
        putIfPresent(quality, "reqId", envelope.ReqId);
        putIfPresent(quality, "commandCode", envelope.CommandCode);
        return quality;
    }

    private void mapHealth(Map<String, Object> p, List<Map<String, Object>> measurements,
                           List<Map<String, Object>> sleepSessions, long fallback) {
        addMetric(measurements, "HEART_RATE", "bpm", num(p.get("heartRate")),
                parseTime(asText(p.get("hrTime")), fallback));
        addMetric(measurements, "SPO2", "%", num(p.get("bloodOxygen")),
                parseTime(asText(p.get("bloodOxygenTime")), fallback));

        Double systolic = num(p.get("bloodPressureMax"));
        if (systolic != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("metricType", "BLOOD_PRESSURE");
            m.put("measuredAt", parseTime(asText(p.get("bpTime")), fallback));
            m.put("primaryValue", systolic);
            Double diastolic = num(p.get("bloodPressureMin"));
            if (diastolic != null) {
                m.put("secondaryValue", diastolic);
            }
            m.put("unit", "mmHg");
            m.put("source", SOURCE);
            measurements.add(m);
        }

        addMetric(measurements, "STEPS", "steps", num(p.get("steps")), fallback);
        addMetric(measurements, "DISTANCE", "m", num(p.get("distance")), fallback);
        addMetric(measurements, "CALORIE", "kcal", num(p.get("calorie")), fallback);

        String sleepTime = asText(p.get("sleepTime"));
        Double totalSleep = num(p.get("totalSleep"));
        if (sleepTime != null && !sleepTime.isBlank() && totalSleep != null) {
            Long start = parseTime(sleepTime, null);
            if (start != null) {
                long end = start + (long) (totalSleep * 1000L);
                int deep = (int) Math.round(orZero(num(p.get("deepSleep"))) / 60.0);
                int light = (int) Math.round(orZero(num(p.get("lighSleep"))) / 60.0);
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("startedAt", start);
                s.put("endedAt", end);
                s.put("deepMinutes", deep);
                s.put("lightMinutes", light);
                s.put("source", SOURCE);
                sleepSessions.add(s);
            }
        }
    }

    private void mapStepRoll(Map<String, Object> p, List<Map<String, Object>> measurements, long fallback) {
        Long when = parseTime(asText(p.get("dataTime")), fallback);
        addMetric(measurements, "STEPS", "steps", num(p.get("step")), when);
        addMetric(measurements, "ROLL", "count", num(p.get("roll")), when);
        addMetric(measurements, "DISTANCE", "m", num(p.get("distance")), when);
        addMetric(measurements, "CALORIE", "kcal", num(p.get("calorie")), when);
    }

    private void mapTemperature(Map<String, Object> p, List<Map<String, Object>> measurements, long fallback) {
        addMetric(measurements, "BODY_TEMPERATURE", "°C", num(p.get("temperature")),
                parseTime(asText(p.get("temperatureTime")), fallback));
    }

    private void mapLocation(Map<String, Object> p, List<Map<String, Object>> measurements, long fallback) {
        addMetric(measurements, "DEVICE_BATTERY", "%", num(p.get("battery")), fallback);
    }

    private void addMetric(List<Map<String, Object>> measurements, String metricType, String unit,
                           Double value, Long measuredAt) {
        if (value == null) {
            return;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("metricType", metricType);
        m.put("measuredAt", measuredAt == null ? Instant.now().toEpochMilli() : measuredAt);
        m.put("primaryValue", value);
        m.put("unit", unit);
        m.put("source", SOURCE);
        measurements.add(m);
    }

    private static Long parseTime(String raw, Long fallback) {
        Long parsed = parseTime(raw);
        return parsed == null ? fallback : parsed;
    }

    private static Long parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        if (s.chars().allMatch(Character::isDigit)) {
            return s.length() <= 10 ? Long.parseLong(s) * 1000L : Long.parseLong(s);
        }
        try {
            return LocalDateTime.parse(s, VIOMI_TIME_FMT).atZone(VIOMI_ZONE).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static double orZero(Double v) {
        return v == null ? 0d : v;
    }

    private static Double num(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String asText(Object v) {
        if (v == null) {
            return null;
        }
        String s = String.valueOf(v);
        return s.isBlank() ? null : s;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static long resolveEnvelopeTime(ViomiReportEnvelope envelope) {
        Long t = parseTime(envelope == null ? null : envelope.Time);
        return t == null ? Instant.now().toEpochMilli() : t;
    }

    private static String safeString(String s) {
        return s == null ? "" : s;
    }

    private static String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 12);
        } catch (Exception e) {
            return String.valueOf(Math.abs((long) input.hashCode()));
        }
    }
}
