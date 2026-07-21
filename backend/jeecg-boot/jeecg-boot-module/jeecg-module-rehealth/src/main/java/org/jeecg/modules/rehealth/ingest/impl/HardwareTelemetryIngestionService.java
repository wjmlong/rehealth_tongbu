package org.jeecg.modules.rehealth.ingest.impl;

import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.ingest.TelemetryBatchValidationResult;
import org.jeecg.modules.rehealth.ingest.TelemetryBatchValidator;
import org.jeecg.modules.rehealth.ingest.writer.HardwareTelemetryWriter;
import org.jeecg.modules.rehealth.ingest.writer.HardwareWriteResult;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.springframework.stereotype.Service;

@Service
public class HardwareTelemetryIngestionService implements HardwareIngestionPort {
    private final TelemetryBatchValidator validator;
    private final HardwareTelemetryWriter telemetryWriter;
    private final ReHealthIngestProperties properties;

    public HardwareTelemetryIngestionService(
            TelemetryBatchValidator validator,
            HardwareTelemetryWriter telemetryWriter,
            ReHealthIngestProperties properties
    ) {
        this.validator = validator;
        this.telemetryWriter = telemetryWriter;
        this.properties = properties;
    }

    @Override
    public TelemetryBatchResponseDto acceptBatch(TelemetryBatchRequestDto request) {
        TelemetryBatchValidationResult validation = validator.validate(request);
        if (!validation.valid) {
            return rejectedResponse(request, validation);
        }

        HardwareWriteResult writeResult = telemetryWriter.write(request);
        TelemetryBatchResponseDto response = baseResponse(request, validation);
        response.receiptId = writeResult.receiptId;
        response.status = writeResult.status;
        response.accepted = writeResult.persisted;
        response.persisted = writeResult.persisted;
        response.queued = false;
        response.queueType = "direct-hardware-db";
        response.durableQueue = false;
        response.ingestMode = properties.getIngestMode();
        response.hardwareDbEnabled = properties.isHardwareDbEnabled();
        response.rawSignalUploadEnabled = properties.isRawSignalUploadEnabled();
        response.ingestStage = writeResult.stage;
        response.warnings.addAll(validation.warnings);
        response.warnings.addAll(writeResult.warnings);
        return response;
    }

    private TelemetryBatchResponseDto rejectedResponse(TelemetryBatchRequestDto request, TelemetryBatchValidationResult validation) {
        TelemetryBatchResponseDto response = baseResponse(request, validation);
        response.status = "REJECTED_INVALID";
        response.accepted = false;
        response.persisted = false;
        response.queued = false;
        response.queueType = "direct-hardware-db";
        response.durableQueue = false;
        response.ingestMode = properties.getIngestMode();
        response.hardwareDbEnabled = properties.isHardwareDbEnabled();
        response.rawSignalUploadEnabled = properties.isRawSignalUploadEnabled();
        response.ingestStage = "VALIDATION_REJECTED";
        response.rejectedCount = validation.recordCount;
        response.warnings.addAll(validation.errors);
        response.warnings.addAll(validation.warnings);
        return response;
    }

    private TelemetryBatchResponseDto baseResponse(TelemetryBatchRequestDto request, TelemetryBatchValidationResult validation) {
        TelemetryBatchResponseDto response = new TelemetryBatchResponseDto();
        response.batchId = request == null ? null : request.batchId;
        response.measurementCount = validation.measurementCount;
        response.sleepSessionCount = validation.sleepSessionCount;
        response.activitySessionCount = validation.activitySessionCount;
        response.signalChunkCount = validation.signalChunkCount;
        response.recordCount = validation.recordCount;
        return response;
    }
}
