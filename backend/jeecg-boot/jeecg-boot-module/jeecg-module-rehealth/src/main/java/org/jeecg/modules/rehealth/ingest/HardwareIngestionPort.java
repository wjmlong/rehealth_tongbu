package org.jeecg.modules.rehealth.ingest;

import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;

public interface HardwareIngestionPort {
    TelemetryBatchResponseDto acceptBatch(TelemetryBatchRequestDto request);
}
