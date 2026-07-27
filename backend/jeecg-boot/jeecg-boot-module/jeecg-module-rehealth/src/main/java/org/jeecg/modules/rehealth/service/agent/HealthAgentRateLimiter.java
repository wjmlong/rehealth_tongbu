package org.jeecg.modules.rehealth.service.agent;

public interface HealthAgentRateLimiter {
    HealthAgentRateLimitDecision acquire(String tenantId, String userId);
}
