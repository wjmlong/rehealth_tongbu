package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;

public interface HealthAgentEngine {
    HealthAgentResponseDto respond(HealthAgentEngineRequest request);
}
