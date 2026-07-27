package org.jeecg.modules.rehealth.ingest.query;

import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;

public interface HardwareTelemetryQuery {
    RecentTelemetryResponseDto recentForUser(String userId, int limit);
}
