package com.rehealth.device.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ServiceCredentialProvider {
    private final String credential;

    public ServiceCredentialProvider(
            @Value("${rehealth.identity.service-credential:}") String inlineCredential,
            @Value("${rehealth.identity.service-credential-file:}") String credentialFile
    ) {
        this.credential = resolve(inlineCredential, credentialFile);
    }

    public String credential() {
        return credential;
    }

    private String resolve(String inlineCredential, String credentialFile) {
        if (inlineCredential != null && !inlineCredential.isBlank()) {
            return inlineCredential.trim();
        }
        if (credentialFile == null || credentialFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(credentialFile)).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read internal service credential file", exception);
        }
    }
}
