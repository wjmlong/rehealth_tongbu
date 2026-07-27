package com.rehealth.device.application;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryContractValidator;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import com.rehealth.device.port.TelemetryReadPort;
import com.rehealth.device.port.TelemetryWritePort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DeviceTelemetryService {
    private final TelemetryContractValidator validator;
    private final IdentityAuthorizationPort identityAuthorization;
    private final TelemetryWritePort telemetryWriter;
    private final TelemetryReadPort telemetryReader;

    public DeviceTelemetryService(
            TelemetryContractValidator validator,
            IdentityAuthorizationPort identityAuthorization,
            TelemetryWritePort telemetryWriter,
            TelemetryReadPort telemetryReader
    ) {
        this.validator = validator;
        this.identityAuthorization = identityAuthorization;
        this.telemetryWriter = telemetryWriter;
        this.telemetryReader = telemetryReader;
    }

    public TelemetryBatchResponse accept(String accessToken, String tenantId, TelemetryBatchRequest request) {
        requireAuthenticationContext(accessToken, tenantId);
        TelemetryValidationResult validation = validator.validateClientRequest(request);
        if (!validation.valid()) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, validation.errors().get(0).code());
        }
        DeviceClaims claims = identityAuthorization.authorize(accessToken, tenantId, request.deviceId);
        return telemetryWriter.write(claims, request, validation);
    }

    public RecentTelemetryResponse recent(
            String accessToken,
            String tenantId,
            String deviceId,
            int limit
    ) {
        requireAuthenticationContext(accessToken, tenantId);
        if (deviceId == null || deviceId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.FORBIDDEN, "AUTHORIZATION_CONTEXT_REJECTED");
        }
        if (limit < 1 || limit > 500) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "LIMIT_INVALID");
        }
        DeviceClaims claims = identityAuthorization.authorize(accessToken, tenantId, deviceId);
        return telemetryReader.recent(claims, limit);
    }

    private void requireAuthenticationContext(String accessToken, String tenantId) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new DeviceRequestException(HttpStatus.UNAUTHORIZED, "USER_TOKEN_REQUIRED");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_REQUIRED");
        }
    }
}
