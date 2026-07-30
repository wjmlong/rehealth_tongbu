package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;

public record HealthAgentPromptContext(
        HealthAgentModelRequestDto legacyRequest,
        String authorizedContextJson
) {
}
