package org.jeecg.modules.rehealth.service.agent;

public class HealthAgentRequestException extends RuntimeException {
    private final int statusCode;

    public HealthAgentRequestException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
