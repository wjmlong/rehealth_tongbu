package org.jeecg.modules.rehealth.auth;

import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

@Service
public class InternalIdentityAuthorizationService {
    private final ServiceCredentialVerifier serviceCredentialVerifier;
    private final UserTokenIdentityProvider identityProvider;
    private final ReHealthBusinessRepository businessRepository;

    public InternalIdentityAuthorizationService(
            ServiceCredentialVerifier serviceCredentialVerifier,
            UserTokenIdentityProvider identityProvider,
            ReHealthBusinessRepository businessRepository
    ) {
        this.serviceCredentialVerifier = serviceCredentialVerifier;
        this.identityProvider = identityProvider;
        this.businessRepository = businessRepository;
    }

    InternalAuthorizationDecision authorize(
            String serviceCredential,
            String accessToken,
            String spoofedUserId,
            String spoofedTenantId,
            String spoofedDeviceId,
            InternalDeviceAuthorizationRequestDto request
    ) {
        if (!serviceCredentialVerifier.matches(serviceCredential)) {
            return forbidden("SERVICE_CREDENTIAL_REJECTED");
        }
        if (hasText(spoofedUserId) || hasText(spoofedTenantId) || hasText(spoofedDeviceId)) {
            return forbidden("SPOOFED_IDENTITY_HEADER");
        }
        if (request == null || !hasText(request.tenantId) || !hasText(request.deviceId)) {
            return forbidden("AUTHORIZATION_CONTEXT_REJECTED");
        }

        ResolvedUserIdentity identity;
        try {
            identity = identityProvider.resolve(accessToken);
        } catch (IdentityResolutionException exception) {
            if (exception.kind() == IdentityResolutionException.Kind.TOKEN_REJECTED) {
                return new InternalAuthorizationDecision(
                        InternalAuthorizationDecision.Status.UNAUTHORIZED,
                        InternalDeviceAuthorizationResponseDto.denied("USER_TOKEN_REJECTED")
                );
            }
            return unavailable();
        } catch (RuntimeException exception) {
            return unavailable();
        }

        if (!identity.tenantIds().contains(request.tenantId)) {
            return forbidden("TENANT_MEMBERSHIP_REJECTED");
        }
        try {
            if (!businessRepository.hasActiveDeviceBinding(identity.userId(), request.deviceId)) {
                return forbidden("DEVICE_BINDING_REJECTED");
            }
        } catch (RuntimeException exception) {
            return unavailable();
        }
        return new InternalAuthorizationDecision(
                InternalAuthorizationDecision.Status.ALLOWED,
                InternalDeviceAuthorizationResponseDto.allowed(
                        identity.userId(),
                        request.tenantId,
                        request.deviceId
                )
        );
    }

    private InternalAuthorizationDecision forbidden(String code) {
        return new InternalAuthorizationDecision(
                InternalAuthorizationDecision.Status.FORBIDDEN,
                InternalDeviceAuthorizationResponseDto.denied(code)
        );
    }

    private InternalAuthorizationDecision unavailable() {
        return new InternalAuthorizationDecision(
                InternalAuthorizationDecision.Status.UNAVAILABLE,
                InternalDeviceAuthorizationResponseDto.denied("IDENTITY_PROVIDER_UNAVAILABLE")
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
