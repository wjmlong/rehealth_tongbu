package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.model.HealthAgentModelClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyModelServiceHealthAgentEngine {
    private final HealthAgentModelClient modelClient;

    public LegacyModelServiceHealthAgentEngine(HealthAgentModelClient modelClient) {
        this.modelClient = modelClient;
    }

    public HealthAgentResponseDto respond(HealthAgentEngineRequest request) {
        return modelClient.respond(request.promptContext().legacyRequest());
    }
}
