package org.jeecg.modules.rehealth.model;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;

public interface HealthAgentModelClient {
    HealthAgentResponseDto respond(HealthAgentModelRequestDto request);
}
