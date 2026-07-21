package org.jeecg.modules.rehealth.ingest.writer;

import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;

public interface HardwareTelemetryWriter {
    HardwareWriteResult write(TelemetryBatchRequestDto request);

    boolean isDurable();

    String writerType();
}
