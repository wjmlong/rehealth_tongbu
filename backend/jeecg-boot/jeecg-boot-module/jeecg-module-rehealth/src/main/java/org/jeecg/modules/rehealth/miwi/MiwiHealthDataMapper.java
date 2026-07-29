package org.jeecg.modules.rehealth.miwi;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Maps Miwi (云米) push payloads into the normalized ReHealth telemetry measurement maps
 * consumed by {@code HardwareIngestionPort}.
 *
 * Vendor payload example (already unwrapped from the escaped ResultData string):
 * <pre>
 * {"imei":"7809101598","heartRate":67,"bloodPressureMax":108,"bloodPressureMin":67,
 *  "bloodOxygen":95,"temperature":36.5,"step":5230,"hrv":42,"breathRate":16,
 *  "timestamp":1720000010}
 * </pre>
 *
 * All timestamps are normalized to UTC epoch milliseconds because the vendor mixes
 * UTC, local time strings, second-level and millisecond-level timestamps.
 */
@Component
public class MiwiHealthDataMapper {
    private static final DateTimeFormatter[] LOCAL_DATE_TIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };
    private static final DateTimeFormatter[] LOCAL_DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
    };

    /** Millis threshold: values above this are treated as epoch millis, below as epoch seconds. */
    private static final long MILLIS_THRESHOLD = 100_000_000_000L;

    public List<Map<String, Object>> toMeasurements(Map<String, Object> payload, long measuredAtUtcMillis) {
        java.util.ArrayList<Map<String, Object>> measurements = new java.util.ArrayList<>();

        Double heartRate = number(payload, "heartRate", "heartrate", "hr");
        if (isPlausible(heartRate, 25, 250)) {
            measurements.add(measurement("HEART_RATE", heartRate, null, "bpm", measuredAtUtcMillis));
        }

        Double systolic = number(payload, "bloodPressureMax", "sbp", "systolic");
        Double diastolic = number(payload, "bloodPressureMin", "dbp", "diastolic");
        if (isPlausible(systolic, 60, 260) && isPlausible(diastolic, 30, 200)) {
            measurements.add(measurement("BLOOD_PRESSURE", systolic, diastolic, "mmHg", measuredAtUtcMillis));
        }

        Double spo2 = number(payload, "bloodOxygen", "spo2", "oxygen");
        if (isPlausible(spo2, 50, 100)) {
            measurements.add(measurement("BLOOD_OXYGEN", spo2, null, "%", measuredAtUtcMillis));
        }

        Double temperature = number(payload, "temperature", "bodyTemperature", "skinTemperature", "temp");
        if (isPlausible(temperature, 30, 45)) {
            measurements.add(measurement("BODY_TEMPERATURE", temperature, null, "celsius", measuredAtUtcMillis));
        }

        Double steps = number(payload, "step", "steps", "stepCount");
        if (isPlausible(steps, 0, 200_000)) {
            measurements.add(measurement("STEPS", steps, null, "count", measuredAtUtcMillis));
        }

        Double calories = number(payload, "calorie", "calories", "kcal");
        if (isPlausible(calories, 0, 50_000)) {
            measurements.add(measurement("CALORIES", calories, null, "kcal", measuredAtUtcMillis));
        }

        Double distance = number(payload, "distance", "distanceMeters");
        if (isPlausible(distance, 0, 500_000)) {
            measurements.add(measurement("DISTANCE", distance, null, "m", measuredAtUtcMillis));
        }

        Double hrv = number(payload, "hrv", "hrvValue");
        if (isPlausible(hrv, 1, 300)) {
            measurements.add(measurement("HRV", hrv, null, "ms", measuredAtUtcMillis));
        }

        Double breathRate = number(payload, "breathRate", "respiratoryRate", "breathe");
        if (isPlausible(breathRate, 4, 60)) {
            measurements.add(measurement("RESPIRATORY_RATE", breathRate, null, "rpm", measuredAtUtcMillis));
        }

        Double fatigue = number(payload, "fatigue", "fatigueValue");
        if (isPlausible(fatigue, 0, 100)) {
            measurements.add(measurement("FATIGUE", fatigue, null, "score", measuredAtUtcMillis));
        }

        Double pressure = number(payload, "pressure", "stress", "mentalPressure");
        if (isPlausible(pressure, 0, 100)) {
            measurements.add(measurement("STRESS", pressure, null, "score", measuredAtUtcMillis));
        }

        Double battery = number(payload, "battery", "batteryLevel", "power");
        if (isPlausible(battery, 0, 100)) {
            measurements.add(measurement("DEVICE_BATTERY", battery, null, "%", measuredAtUtcMillis));
        }

        return measurements;
    }

    public Map<String, Object> measurement(
            String metricType,
            Double primaryValue,
            Double secondaryValue,
            String unit,
            long measuredAtUtcMillis
    ) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", "miwi-" + UUID.randomUUID());
        record.put("metricType", metricType);
        record.put("measuredAt", measuredAtUtcMillis);
        record.put("primaryValue", primaryValue);
        if (secondaryValue != null) {
            record.put("secondaryValue", secondaryValue);
        }
        record.put("unit", unit);
        record.put("source", "MIWI_4G_CLOUD");
        return record;
    }

    /**
     * Extracts the vendor measurement time and normalizes to UTC epoch millis.
     * Falls back to {@code fallbackUtcMillis} (usually the callback receive time)
     * when the payload carries no usable timestamp.
     */
    public long resolveMeasuredAt(Map<String, Object> payload, long fallbackUtcMillis) {
        Object raw = firstNonNull(payload, "timestamp", "time", "measureTime", "dataTime", "uploadTime", "date");
        if (raw == null) {
            return fallbackUtcMillis;
        }
        if (raw instanceof Number numberValue) {
            return normalizeEpoch(numberValue.longValue(), fallbackUtcMillis);
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty()) {
            return fallbackUtcMillis;
        }
        if (text.chars().allMatch(Character::isDigit)) {
            try {
                return normalizeEpoch(Long.parseLong(text), fallbackUtcMillis);
            } catch (NumberFormatException ignored) {
                return fallbackUtcMillis;
            }
        }
        for (DateTimeFormatter format : LOCAL_DATE_TIME_FORMATS) {
            try {
                // Vendor local-time strings are documented as Beijing time (UTC+8).
                return LocalDateTime.parse(text, format).toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            } catch (Exception ignored) {
                // try next format
            }
        }
        for (DateTimeFormatter format : LOCAL_DATE_FORMATS) {
            try {
                return LocalDate.parse(text, format).atStartOfDay().toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            } catch (Exception ignored) {
                // try next format
            }
        }
        return fallbackUtcMillis;
    }

    public String extractImei(Map<String, Object> payload) {
        Object raw = firstNonNull(payload, "imei", "IMEI", "Imei", "deviceImei");
        if (raw == null) {
            return null;
        }
        String imei = String.valueOf(raw).trim();
        return imei.isEmpty() ? null : imei.toLowerCase(Locale.ROOT);
    }

    private long normalizeEpoch(long value, long fallbackUtcMillis) {
        if (value <= 0) {
            return fallbackUtcMillis;
        }
        return value >= MILLIS_THRESHOLD ? value : value * 1000L;
    }

    private Double number(Map<String, Object> payload, String... keys) {
        Object raw = firstNonNull(payload, keys);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number numberValue) {
            return numberValue.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Object firstNonNull(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isPlausible(Double value, double min, double max) {
        return value != null && value >= min && value <= max;
    }
}
