package org.jeecg.modules.rehealth.ingest.writer;

import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledHardwareTelemetryWriter implements HardwareTelemetryWriter {
    @Override
    public HardwareWriteResult write(TelemetryBatchRequestDto request) {
        throw new HardwarePersistenceUnavailableException(
                "hardware_db persistence is disabled; telemetry was not accepted"
        );
    }

    @Override
    public boolean isDurable() {
        return false;
    }

    @Override
    public String writerType() {
        return "hardware-db-disabled";
    }
}
