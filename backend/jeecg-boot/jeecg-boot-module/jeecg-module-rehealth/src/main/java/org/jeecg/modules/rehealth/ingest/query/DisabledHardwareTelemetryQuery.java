package org.jeecg.modules.rehealth.ingest.query;

import org.jeecg.modules.rehealth.ingest.writer.HardwarePersistenceUnavailableException;
import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "rehealth.hardware-db.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledHardwareTelemetryQuery implements HardwareTelemetryQuery {
    @Override
    public RecentTelemetryResponseDto recentForUser(String userId, int limit) {
        throw new HardwarePersistenceUnavailableException("hardware_db query is disabled");
    }
}
