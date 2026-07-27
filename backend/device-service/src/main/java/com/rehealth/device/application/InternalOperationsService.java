package com.rehealth.device.application;

import com.rehealth.device.config.ServiceCredentialProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@Service
public class InternalOperationsService {
    private final ServiceCredentialProvider credentialProvider;

    public InternalOperationsService(ServiceCredentialProvider credentialProvider) {
        this.credentialProvider = credentialProvider;
    }

    public Map<String, String> status(String suppliedCredential) {
        byte[] expected = credentialProvider.credential().getBytes(StandardCharsets.UTF_8);
        byte[] supplied = suppliedCredential == null
                ? new byte[0]
                : suppliedCredential.getBytes(StandardCharsets.UTF_8);
        if (expected.length == 0 || !MessageDigest.isEqual(expected, supplied)) {
            throw new DeviceRequestException(HttpStatus.FORBIDDEN, "SERVICE_CREDENTIAL_REJECTED");
        }
        return Map.of("service", "device-service", "persistence", "PORT");
    }
}
