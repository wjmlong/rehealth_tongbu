package org.jeecg.modules.rehealth.service.agent;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledHealthAgentLongitudinalContextReader implements HealthAgentLongitudinalContextReader {
    @Override
    public Map<String, Object> read(String authenticatedUserId) {
        return Map.of("coverage", Map.of(
                "rhi", "unavailable",
                "rdi", "unavailable",
                "latestAttribution", "unavailable",
                "recentInterventionFeedback", "unavailable"
        ));
    }
}
