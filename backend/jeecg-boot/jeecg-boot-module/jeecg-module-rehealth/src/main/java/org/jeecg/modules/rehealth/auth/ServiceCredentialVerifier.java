package org.jeecg.modules.rehealth.auth;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class ServiceCredentialVerifier {
    private final byte[] expectedCredential;

    public ServiceCredentialVerifier(InternalServiceAuthProperties properties) {
        this.expectedCredential = properties.serviceCredential().getBytes(StandardCharsets.UTF_8);
    }

    boolean matches(String providedCredential) {
        if (expectedCredential.length == 0 || providedCredential == null || providedCredential.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedCredential,
                providedCredential.getBytes(StandardCharsets.UTF_8)
        );
    }
}
