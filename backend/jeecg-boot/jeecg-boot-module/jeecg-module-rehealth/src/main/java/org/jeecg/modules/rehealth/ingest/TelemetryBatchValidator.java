package org.jeecg.modules.rehealth.ingest;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class TelemetryBatchValidator {
    private final ReHealthIngestProperties properties;

    public TelemetryBatchValidator(ReHealthIngestProperties properties) {
        this.properties = properties;
    }

    public TelemetryBatchValidationResult validate(TelemetryBatchRequestDto request) {
        TelemetryBatchValidationResult result = new TelemetryBatchValidationResult();
        if (request == null) {
            result.errors.add("request body is required");
            return result;
        }

        requireText(result, request.batchId, "batchId is required");
        requireText(result, request.userId, "userId is required until authenticated user binding is wired");
        requireText(result, request.deviceId, "deviceId is required");

        if (request.collectedFrom != null && request.collectedTo != null && request.collectedFrom > request.collectedTo) {
            result.errors.add("collectedFrom must be before collectedTo");
        }

        result.measurementCount = sizeOf(request.measurements);
        result.sleepSessionCount = sizeOf(request.sleepSessions);
        result.activitySessionCount = sizeOf(request.activitySessions);
        result.signalChunkCount = sizeOf(request.signalChunks);
        result.recordCount = result.measurementCount + result.sleepSessionCount + result.activitySessionCount + result.signalChunkCount;

        if (result.recordCount == 0) {
            result.errors.add("batch must contain at least one measurement, sleep session, activity session, or allowed signal metadata record");
        }
        if (result.recordCount > properties.getMaxRecordsPerBatch()) {
            result.errors.add("batch record count exceeds configured maxRecordsPerBatch");
        }

        if (!properties.isRawSignalUploadEnabled() && result.signalChunkCount > 0) {
            result.errors.add("raw signal chunk upload is disabled by default");
        }
        if (!properties.isRawSignalUploadEnabled() && containsRawSignalPayload(request)) {
            result.errors.add("raw signal payload fields are disabled by default");
        }
        if (properties.isRawSignalUploadEnabled()) {
            result.warnings.add("raw signal upload is enabled; confirm consent, retention, and storage controls before production use");
        }

        result.valid = result.errors.isEmpty();
        return result;
    }

    private void requireText(TelemetryBatchValidationResult result, String value, String error) {
        if (value == null || value.isBlank()) {
            result.errors.add(error);
        }
    }

    private int sizeOf(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private boolean containsRawSignalPayload(TelemetryBatchRequestDto request) {
        return containsRawSignalValue(request.measurements)
                || containsRawSignalValue(request.sleepSessions)
                || containsRawSignalValue(request.activitySessions)
                || containsRawSignalValue(request.quality);
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
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "_");
        return normalized.contains("raw")
                || normalized.contains("ppg")
                || normalized.contains("rri")
                || normalized.contains("waveform")
                || normalized.contains("signal_samples");
    }
}
