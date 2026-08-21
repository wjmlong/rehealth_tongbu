package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;

import java.time.ZoneId;
import java.util.List;

public record HealthAgentEngineRequest(
        String tenantId,
        String userId,
        ZoneId timeZone,
        HealthAgentPromptContext promptContext,
        List<HealthAgentHistoryMessageDto> history
) {
}
