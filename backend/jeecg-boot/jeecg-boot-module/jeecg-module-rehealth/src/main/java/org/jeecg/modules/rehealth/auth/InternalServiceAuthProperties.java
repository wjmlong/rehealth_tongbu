package org.jeecg.modules.rehealth.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceAuthProperties {
    private final String serviceCredential;

    public InternalServiceAuthProperties(
            @Value("${rehealth.internal-auth.service-credential:}") String serviceCredential
    ) {
        this.serviceCredential = serviceCredential == null ? "" : serviceCredential;
    }

    String serviceCredential() {
        return serviceCredential;
    }
}
