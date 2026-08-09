package com.rehealth.device.application;

import com.rehealth.device.config.ServiceCredentialProvider;
import com.rehealth.device.port.TelemetryReadPort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;

@Service
public class InternalOperationsService {
    private final ServiceCredentialProvider credentialProvider;
    private final TelemetryReadPort telemetryReader;

    public InternalOperationsService(
            ServiceCredentialProvider credentialProvider,
            TelemetryReadPort telemetryReader
    ) {
        this.credentialProvider = credentialProvider;
        this.telemetryReader = telemetryReader;
    }

    public Map<String, String> status(String suppliedCredential) {
        validateCredential(suppliedCredential);
        return Map.of("service", "device-service", "persistence", "PORT");
    }

    public UserHealthSummary userHealth(String suppliedCredential, String tenantId, String userId) {
        validateCredential(suppliedCredential);
        if (tenantId == null || tenantId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "TENANT_ID_REQUIRED");
        }
        if (userId == null || userId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        return telemetryReader.healthSummaryForUser(tenantId, userId);
    }

    public InterventionTelemetryContext interventionContext(
            String suppliedCredential,
            String tenantId,
            String userId,
            String timeZone
    ) {
        validateCredential(suppliedCredential);
        if (tenantId == null || tenantId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "TENANT_ID_REQUIRED");
        }
        if (userId == null || userId.isBlank()) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "USER_ID_REQUIRED");
        }
        try {
            ZoneId zoneId = ZoneId.of(timeZone == null || timeZone.isBlank()
                    ? "Asia/Shanghai"
                    : timeZone);
            return telemetryReader.interventionContext(tenantId, userId, zoneId);
        } catch (DateTimeException invalidTimeZone) {
            throw new DeviceRequestException(HttpStatus.BAD_REQUEST, "INVALID_TIME_ZONE");
        }
    }

    private void validateCredential(String suppliedCredential) {
        byte[] expected = credentialProvider.credential().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedCredential == null
                ? new byte[0]
                : suppliedCredential.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new DeviceRequestException(HttpStatus.FORBIDDEN, "SERVICE_CREDENTIAL_REJECTED");
        }
    }
}
