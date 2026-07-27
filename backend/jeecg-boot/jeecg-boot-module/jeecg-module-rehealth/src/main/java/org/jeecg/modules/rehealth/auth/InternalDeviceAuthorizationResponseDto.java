package org.jeecg.modules.rehealth.auth;

public class InternalDeviceAuthorizationResponseDto {
    public boolean authorized;
    public String code;
    public String userId;
    public String tenantId;
    public String deviceId;

    static InternalDeviceAuthorizationResponseDto allowed(
            String userId,
            String tenantId,
            String deviceId
    ) {
        InternalDeviceAuthorizationResponseDto response = new InternalDeviceAuthorizationResponseDto();
        response.authorized = true;
        response.code = "AUTHORIZED";
        response.userId = userId;
        response.tenantId = tenantId;
        response.deviceId = deviceId;
        return response;
    }

    static InternalDeviceAuthorizationResponseDto denied(String code) {
        InternalDeviceAuthorizationResponseDto response = new InternalDeviceAuthorizationResponseDto();
        response.authorized = false;
        response.code = code;
        return response;
    }
}
