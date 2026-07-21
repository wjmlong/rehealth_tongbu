package org.jeecg.modules.rehealth.ingest.writer;

import java.util.Map;

final class TelemetryRecordReader {
    private TelemetryRecordReader() {
    }

    static String requiredString(Map<String, Object> record, String... keys) {
        String value = optionalString(record, keys);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("required telemetry field is missing: " + keys[0]);
        }
        return value;
    }

    static String optionalString(Map<String, Object> record, String... keys) {
        Object value = value(record, keys);
        return value == null ? null : String.valueOf(value);
    }

    static long requiredLong(Map<String, Object> record, String... keys) {
        Number value = requiredNumber(record, keys);
        return value.longValue();
    }

    static int integerOrDefault(Map<String, Object> record, int defaultValue, String... keys) {
        Object value = value(record, keys);
        return value == null ? defaultValue : number(value, keys[0]).intValue();
    }

    static Double optionalDouble(Map<String, Object> record, String... keys) {
        Object value = value(record, keys);
        return value == null ? null : number(value, keys[0]).doubleValue();
    }

    static double requiredDouble(Map<String, Object> record, String... keys) {
        return requiredNumber(record, keys).doubleValue();
    }

    private static Number requiredNumber(Map<String, Object> record, String... keys) {
        Object value = value(record, keys);
        if (value == null) {
            throw new IllegalArgumentException("required telemetry field is missing: " + keys[0]);
        }
        return number(value, keys[0]);
    }

    private static Number number(Object value, String fieldName) {
        if (value instanceof Number number) {
            return number;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("telemetry field must be numeric: " + fieldName, e);
        }
    }

    private static Object value(Map<String, Object> record, String... keys) {
        if (record == null) {
            throw new IllegalArgumentException("telemetry record must not be null");
        }
        for (String key : keys) {
            if (record.containsKey(key)) {
                return record.get(key);
            }
        }
        return null;
    }
}
