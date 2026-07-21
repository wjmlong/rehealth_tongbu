package org.jeecg.modules.rehealth.ingest;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelemetryBatchValidatorTest {
    @Test
    void validBatchIsAccepted() {
        TelemetryBatchValidationResult result = validator(defaultProperties()).validate(validBatch());

        assertTrue(result.valid);
        assertEquals(1, result.recordCount);
        assertEquals(1, result.measurementCount);
        assertTrue(result.errors.isEmpty());
    }

    @Test
    void emptyBatchIsRejected() {
        TelemetryBatchRequestDto request = validBatch();
        request.measurements.clear();

        TelemetryBatchValidationResult result = validator(defaultProperties()).validate(request);

        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(error -> error.contains("at least one")));
    }

    @Test
    void rawSignalChunksAreRejectedByDefault() {
        TelemetryBatchRequestDto request = validBatch();
        request.signalChunks.add(Map.of("signal_type", "ppg", "sample_count", 16));

        TelemetryBatchValidationResult result = validator(defaultProperties()).validate(request);

        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(error -> error.contains("raw signal chunk upload is disabled")));
    }

    @Test
    void rawPayloadKeysAreRejectedByDefault() {
        TelemetryBatchRequestDto request = validBatch();
        request.measurements.get(0).put("rawPayload", "hidden-sample");

        TelemetryBatchValidationResult result = validator(defaultProperties()).validate(request);

        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(error -> error.contains("raw signal payload fields")));
    }

    @Test
    void nestedRawPayloadMetadataIsRejectedByDefault() {
        TelemetryBatchRequestDto request = validBatch();
        request.quality.put("device", Map.of("ppgPayload", "hidden-sample"));

        TelemetryBatchValidationResult result = validator(defaultProperties()).validate(request);

        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(error -> error.contains("raw signal payload fields")));
    }

    @Test
    void oversizedBatchIsRejected() {
        ReHealthIngestProperties properties = defaultProperties();
        ReflectionTestUtils.setField(properties, "maxRecordsPerBatch", 1);
        TelemetryBatchRequestDto request = validBatch();
        request.sleepSessions.add(Map.of("started_at", 1720000000000L, "ended_at", 1720000300000L));

        TelemetryBatchValidationResult result = validator(properties).validate(request);

        assertFalse(result.valid);
        assertTrue(result.errors.stream().anyMatch(error -> error.contains("maxRecordsPerBatch")));
    }

    private TelemetryBatchValidator validator(ReHealthIngestProperties properties) {
        return new TelemetryBatchValidator(properties);
    }

    private TelemetryBatchRequestDto validBatch() {
        TelemetryBatchRequestDto request = new TelemetryBatchRequestDto();
        request.batchId = "batch-001";
        request.userId = "user-001";
        request.deviceId = "ring-001";
        request.collectedFrom = 1720000000000L;
        request.collectedTo = 1720000300000L;
        request.source = "android-mrd";
        Map<String, Object> measurement = new LinkedHashMap<>();
        measurement.put("metric_type", "heart_rate");
        measurement.put("measured_at", 1720000010000L);
        measurement.put("value", 72);
        measurement.put("unit", "bpm");
        request.measurements.add(measurement);
        return request;
    }

    private ReHealthIngestProperties defaultProperties() {
        ReHealthIngestProperties properties = new ReHealthIngestProperties();
        ReflectionTestUtils.setField(properties, "ingestMode", "durable-direct");
        ReflectionTestUtils.setField(properties, "hardwareDbEnabled", true);
        ReflectionTestUtils.setField(properties, "rawSignalUploadEnabled", false);
        ReflectionTestUtils.setField(properties, "queueType", "direct-hardware-db");
        ReflectionTestUtils.setField(properties, "maxRecordsPerBatch", 5000);
        return properties;
    }
}
