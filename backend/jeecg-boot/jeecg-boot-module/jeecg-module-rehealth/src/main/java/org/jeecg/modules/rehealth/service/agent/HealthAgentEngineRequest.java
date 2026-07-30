package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;

import java.util.List;

public record HealthAgentEngineRequest(
        HealthAgentPromptContext promptContext,
        List<HealthAgentHistoryMessageDto> history
) {
}
