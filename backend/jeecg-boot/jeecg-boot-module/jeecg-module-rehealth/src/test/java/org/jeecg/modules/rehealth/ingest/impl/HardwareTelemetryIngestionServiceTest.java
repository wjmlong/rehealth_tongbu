package org.jeecg.modules.rehealth.ingest.impl;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.ingest.TelemetryBatchValidator;
import org.jeecg.modules.rehealth.ingest.writer.HardwareTelemetryWriter;
import org.jeecg.modules.rehealth.ingest.writer.HardwareWriteResult;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardwareTelemetryIngestionServiceTest {
    @Test
    void committedWriteReturnsTruthfulPersistenceResponse() {
        ReHealthIngestProperties properties = defaultProperties();
        HardwareTelemetryIngestionService service = service(properties, committedWriter(false));

        TelemetryBatchResponseDto response = service.acceptBatch(validBatch());

        assertEquals("ACCEPTED_PERSISTED", response.status);
        assertTrue(response.accepted);
        assertTrue(response.persisted);
        assertFalse(response.queued);
        assertFalse(response.durableQueue);
        assertEquals("direct-hardware-db", response.queueType);
        assertEquals("durable-direct", response.ingestMode);
        assertEquals("HARDWARE_DB_COMMITTED", response.ingestStage);
        assertEquals("receipt-001", response.receiptId);
    }

    @Test
    void duplicateWriteRemainsSuccessfulAndPersisted() {
        ReHealthIngestProperties properties = defaultProperties();
        HardwareTelemetryIngestionService service = service(properties, committedWriter(true));

        TelemetryBatchResponseDto response = service.acceptBatch(validBatch());

        assertEquals("ACCEPTED_DUPLICATE", response.status);
        assertTrue(response.accepted);
        assertTrue(response.persisted);
        assertEquals("HARDWARE_DB_IDEMPOTENT_REPLAY", response.ingestStage);
    }

    @Test
    void emptyBatchIsRejectedBeforeWriter() {
        ReHealthIngestProperties properties = defaultProperties();
        HardwareTelemetryIngestionService service = service(properties, failIfCalledWriter());
        TelemetryBatchRequestDto request = validBatch();
        request.measurements.clear();

        TelemetryBatchResponseDto response = service.acceptBatch(request);

        assertEquals("REJECTED_INVALID", response.status);
        assertFalse(response.accepted);
        assertFalse(response.persisted);
        assertEquals("VALIDATION_REJECTED", response.ingestStage);
    }

    @Test
    void rawSignalUploadIsRejectedBeforeWriter() {
        ReHealthIngestProperties properties = defaultProperties();
        HardwareTelemetryIngestionService service = service(properties, failIfCalledWriter());
        TelemetryBatchRequestDto request = validBatch();
        request.signalChunks.add(Map.of("signalType", "ppg", "sampleCount", 16));

        TelemetryBatchResponseDto response = service.acceptBatch(request);

        assertEquals("REJECTED_INVALID", response.status);
        assertFalse(response.accepted);
        assertTrue(response.warnings.stream().anyMatch(warning -> warning.contains("raw signal chunk upload is disabled")));
    }

    private HardwareTelemetryIngestionService service(
            ReHealthIngestProperties properties,
            HardwareTelemetryWriter writer
    ) {
        return new HardwareTelemetryIngestionService(new TelemetryBatchValidator(properties), writer, properties);
    }

    private HardwareTelemetryWriter committedWriter(boolean duplicate) {
        return new HardwareTelemetryWriter() {
            @Override
            public HardwareWriteResult write(TelemetryBatchRequestDto request) {
                HardwareWriteResult result = new HardwareWriteResult();
                result.persisted = true;
                result.duplicate = duplicate;
                result.receiptId = "receipt-001";
                result.status = duplicate ? "ACCEPTED_DUPLICATE" : "ACCEPTED_PERSISTED";
                result.stage = duplicate ? "HARDWARE_DB_IDEMPOTENT_REPLAY" : "HARDWARE_DB_COMMITTED";
                result.writerType = writerType();
                return result;
            }

            @Override
            public boolean isDurable() {
                return true;
            }

            @Override
            public String writerType() {
                return "test-hardware-db";
            }
        };
    }

    private HardwareTelemetryWriter failIfCalledWriter() {
        return new HardwareTelemetryWriter() {
            @Override
            public HardwareWriteResult write(TelemetryBatchRequestDto request) {
                throw new AssertionError("writer must not be called");
            }

            @Override
            public boolean isDurable() {
                return true;
            }

            @Override
            public String writerType() {
                return "fail-if-called";
            }
        };
    }

    private TelemetryBatchRequestDto validBatch() {
        TelemetryBatchRequestDto request = new TelemetryBatchRequestDto();
        request.batchId = "batch-001";
        request.userId = "user-001";
        request.deviceId = "ring-001";
        request.collectedFrom = 1720000000000L;
        request.collectedTo = 1720000300000L;
        request.source = "ANDROID_ROOM";
        request.measurements.add(Map.of(
                "id", "measurement-001",
                "metricType", "HEART_RATE",
                "measuredAt", 1720000010000L,
                "primaryValue", 72,
                "unit", "bpm",
                "source", "MRD"
        ));
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
