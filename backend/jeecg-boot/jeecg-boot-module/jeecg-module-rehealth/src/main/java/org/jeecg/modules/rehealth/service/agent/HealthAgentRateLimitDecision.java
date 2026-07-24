package org.jeecg.modules.rehealth.service.agent;

public record HealthAgentRateLimitDecision(boolean allowed, boolean available) {
    public static HealthAgentRateLimitDecision allowedDecision() {
        return new HealthAgentRateLimitDecision(true, true);
    }

    public static HealthAgentRateLimitDecision exceeded() {
        return new HealthAgentRateLimitDecision(false, true);
    }

    public static HealthAgentRateLimitDecision unavailable() {
        return new HealthAgentRateLimitDecision(false, false);
    }
}
