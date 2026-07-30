package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class RoutingHealthAgentEngine implements HealthAgentEngine {
    private final String engine;
    private final LegacyModelServiceHealthAgentEngine legacy;
    private final LangChain4jHealthAgentEngine langChain4j;

    public RoutingHealthAgentEngine(
            @Value("${rehealth.health-agent.engine:model-service}") String engine,
            LegacyModelServiceHealthAgentEngine legacy,
            LangChain4jHealthAgentEngine langChain4j
    ) {
        this.engine = engine == null ? "model-service" : engine.trim().toLowerCase(Locale.ROOT);
        this.legacy = legacy;
        this.langChain4j = langChain4j;
    }

    @Override
    public HealthAgentResponseDto respond(HealthAgentEngineRequest request) {
        return switch (engine) {
            case "langchain4j" -> langChain4j.respond(request);
            case "model-service", "legacy" -> legacy.respond(request);
            default -> throw new IllegalStateException("unsupported ReHealth health-agent engine: " + engine);
        };
    }
}
