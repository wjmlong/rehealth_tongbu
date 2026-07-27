package com.rehealth.device.adapter;

import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import org.springframework.http.HttpStatus;

public class UnavailableIdentityAuthorizationAdapter implements IdentityAuthorizationPort {
    @Override
    public DeviceClaims authorize(String accessToken, String tenantId, String deviceId) {
        throw new DeviceRequestException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_PROVIDER_UNAVAILABLE");
    }

    @Override
    public boolean ready() {
        return false;
    }
}
