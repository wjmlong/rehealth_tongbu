package com.rehealth.device.config;

public record IdentityServiceEndpoints(String authorizationBaseUrl, String readinessUrl) {
    public IdentityServiceEndpoints {
        if (authorizationBaseUrl == null || authorizationBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Identity authorization base URL is required");
        }
        if (readinessUrl == null || readinessUrl.isBlank()) {
            throw new IllegalArgumentException("Identity readiness URL is required");
        }
    }
}
