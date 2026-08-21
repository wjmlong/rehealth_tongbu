package org.jeecg.modules.rehealth.service.agent;

import java.util.Map;

/** Reads bounded, user-scoped software-db projections for health-agent context. */
public interface HealthAgentLongitudinalContextReader {
    Map<String, Object> read(String authenticatedUserId);
}
