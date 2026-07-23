package com.rehealth.contracts.telemetry.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TelemetryContractValidator {
    private final TelemetryValidationPolicy policy;

    public TelemetryContractValidator(TelemetryValidationPolicy policy) {
        this.policy = policy;
    }

    public TelemetryValidationResult validateClientRequest(TelemetryBatchRequest request) {
        List<TelemetryValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(error("request.required", "$", "request body is required"));
            return result(errors, 0, 0, 0, 0);
        }

        requireText(errors, request.batchId, "batchId");
        requireText(errors, request.deviceId, "deviceId");
        requireText(errors, request.source, "source");
        if (request.userId != null) {
            errors.add(error("owner.client_supplied", "userId", "authenticated ownership must not be supplied by the client"));
        }
        if (!TelemetryContractVersions.SUPPORTED.contains(request.resolvedSchemaVersion())) {
            errors.add(error("schema.unsupported", "schemaVersion", "unsupported telemetry schema version"));
        }
        validateRange(errors, request.collectedFrom, request.collectedTo, "collectedFrom", "collectedTo");

        int measurementCount = sizeOf(request.measurements);
        int sleepCount = sizeOf(request.sleepSessions);
        int activityCount = sizeOf(request.activitySessions);
        int signalCount = sizeOf(request.signalChunks);
        int recordCount = measurementCount + sleepCount + activityCount + signalCount;
        if (recordCount == 0) {
            errors.add(error("batch.empty", "$", "batch must contain normalized telemetry records"));
        }
        if (recordCount > policy.maxRecordsPerBatch()) {
            errors.add(error("batch.oversized", "$", "batch record count exceeds maxRecordsPerBatch"));
        }
        if (!policy.rawSignalUploadEnabled() && signalCount > 0) {
            errors.add(error("raw_signal.disabled", "signalChunks", "raw signal chunk upload is disabled"));
        }

        validateMeasurements(errors, values(request.measurements));
        validateSleep(errors, values(request.sleepSessions));
        validateActivities(errors, values(request.activitySessions));
        if (!policy.rawSignalUploadEnabled() && containsRawSignalValue(request.quality)) {
            errors.add(error("raw_signal.disabled", "quality", "raw signal payload fields are disabled"));
        }
        return result(errors, measurementCount, sleepCount, activityCount, signalCount);
    }

    private void validateMeasurements(List<TelemetryValidationError> errors, List<MeasurementRecord> records) {
        for (int index = 0; index < records.size(); index++) {
            MeasurementRecord record = records.get(index);
            String path = "measurements[" + index + "]";
            if (record == null) {
                errors.add(error("record.required", path, "measurement must not be null"));
                continue;
            }
            requireText(errors, record.metricType, path + ".metricType");
            requirePositive(errors, record.measuredAt, path + ".measuredAt");
            requireFinite(errors, record.primaryValue, path + ".primaryValue");
            requireText(errors, record.unit, path + ".unit");
            rejectRawExtensions(errors, record, path);
        }
    }

    private void validateSleep(List<TelemetryValidationError> errors, List<SleepSessionRecord> records) {
        for (int index = 0; index < records.size(); index++) {
            SleepSessionRecord record = records.get(index);
            String path = "sleepSessions[" + index + "]";
            if (record == null) {
                errors.add(error("record.required", path, "sleep session must not be null"));
                continue;
            }
            validateRange(errors, record.startedAt, record.endedAt, path + ".startedAt", path + ".endedAt");
            requirePositive(errors, record.startedAt, path + ".startedAt");
            requirePositive(errors, record.endedAt, path + ".endedAt");
            requireNonNegative(errors, record.deepMinutes, path + ".deepMinutes");
            requireNonNegative(errors, record.lightMinutes, path + ".lightMinutes");
            requireNonNegative(errors, record.awakeMinutes, path + ".awakeMinutes");
            requireNonNegative(errors, record.remMinutes, path + ".remMinutes");
            requireNonNegative(errors, record.interruptionMinutes, path + ".interruptionMinutes");
            rejectRawExtensions(errors, record, path);
        }
    }

    private void validateActivities(List<TelemetryValidationError> errors, List<ActivitySessionRecord> records) {
        for (int index = 0; index < records.size(); index++) {
            ActivitySessionRecord record = records.get(index);
            String path = "activitySessions[" + index + "]";
            if (record == null) {
                errors.add(error("record.required", path, "activity session must not be null"));
                continue;
            }
            requirePositive(errors, record.startedAt, path + ".startedAt");
            if (record.endedAt != null) {
                requirePositive(errors, record.endedAt, path + ".endedAt");
                validateRange(errors, record.startedAt, record.endedAt, path + ".startedAt", path + ".endedAt");
            }
            requireText(errors, record.activityType, path + ".activityType");
            requireNonNegative(errors, record.steps, path + ".steps");
            requireNonNegative(errors, record.durationMinutes, path + ".durationMinutes");
            requireNonNegative(errors, record.distanceMeters, path + ".distanceMeters");
            requireNonNegative(errors, record.caloriesKcal, path + ".caloriesKcal");
            rejectRawExtensions(errors, record, path);
        }
    }

    private void rejectRawExtensions(List<TelemetryValidationError> errors, TelemetryRecord record, String path) {
        if (!policy.rawSignalUploadEnabled() && containsRawSignalValue(record.extensions())) {
            errors.add(error("raw_signal.disabled", path, "raw signal payload fields are disabled"));
        }
    }

    private boolean containsRawSignalValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (isRawSignalKey(String.valueOf(entry.getKey())) || containsRawSignalValue(entry.getValue())) {
                    return true;
                }
            }
        } else if (value instanceof Iterable<?> values) {
            for (Object item : values) {
                if (containsRawSignalValue(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRawSignalKey(String key) {
        String normalized = key
                .replace("-", "_")
                .replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .toLowerCase(Locale.ROOT);
        if (normalized.equals("raw_signal_excluded")) {
            return false;
        }
        return normalized.equals("raw")
                || normalized.equals("raw_signal")
                || normalized.equals("raw_data")
                || normalized.equals("raw_ppg")
                || normalized.equals("raw_signal_samples")
                || normalized.equals("raw_bytes")
                || normalized.equals("ppg")
                || normalized.equals("ppg_samples")
                || normalized.equals("rri")
                || normalized.equals("rri_samples")
                || normalized.equals("waveform")
                || normalized.equals("signal_samples");
    }

    private void requireText(List<TelemetryValidationError> errors, String value, String path) {
        if (value == null || value.isBlank()) {
            errors.add(error("field.required", path, path + " is required"));
        }
    }

    private void requirePositive(List<TelemetryValidationError> errors, Long value, String path) {
        if (value == null || value <= 0) {
            errors.add(error("timestamp.invalid", path, path + " must be a positive epoch-millisecond timestamp"));
        }
    }

    private void requireFinite(List<TelemetryValidationError> errors, Double value, String path) {
        if (value == null || !Double.isFinite(value)) {
            errors.add(error("number.invalid", path, path + " must be a finite number"));
        }
    }

    private void requireNonNegative(List<TelemetryValidationError> errors, Number value, String path) {
        if (value != null && value.doubleValue() < 0) {
            errors.add(error("number.negative", path, path + " must not be negative"));
        }
    }

    private void validateRange(List<TelemetryValidationError> errors, Long from, Long to, String fromPath, String toPath) {
        if (from != null && to != null && from > to) {
            errors.add(error("timestamp.range", toPath, fromPath + " must be before " + toPath));
        }
    }

    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private <T> List<T> values(List<T> values) {
        return values == null ? List.of() : values;
    }

    private TelemetryValidationError error(String code, String path, String message) {
        return new TelemetryValidationError(code, path, message);
    }

    private TelemetryValidationResult result(
            List<TelemetryValidationError> errors,
            int measurements,
            int sleep,
            int activities,
            int signals
    ) {
        return new TelemetryValidationResult(errors, measurements, sleep, activities, signals);
    }
}
