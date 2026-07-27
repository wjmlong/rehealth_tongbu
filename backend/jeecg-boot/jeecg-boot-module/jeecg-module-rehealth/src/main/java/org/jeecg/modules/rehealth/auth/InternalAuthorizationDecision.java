package org.jeecg.modules.rehealth.auth;

record InternalAuthorizationDecision(
        Status status,
        InternalDeviceAuthorizationResponseDto response
) {
    enum Status {
        ALLOWED,
        UNAUTHORIZED,
        FORBIDDEN,
        UNAVAILABLE
    }
}
