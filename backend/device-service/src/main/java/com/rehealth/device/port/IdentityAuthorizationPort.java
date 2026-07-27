package com.rehealth.device.port;

import com.rehealth.device.domain.DeviceClaims;

public interface IdentityAuthorizationPort {
    DeviceClaims authorize(String accessToken, String tenantId, String deviceId);

    default boolean ready() {
        return true;
    }
}
