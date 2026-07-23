package com.rehealth.device.adapter;

import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.config.IdentityServiceEndpoints;
import com.rehealth.device.config.ServiceCredentialProvider;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

public final class HttpIdentityAuthorizationAdapter implements IdentityAuthorizationPort {
    private final RestClient authorizationClient;
    private final RestClient readinessClient;
    private final ServiceCredentialProvider credentialProvider;

    public HttpIdentityAuthorizationAdapter(
            RestClient.Builder builder,
            IdentityServiceEndpoints endpoints,
            ServiceCredentialProvider credentialProvider
    ) {
        this.authorizationClient = builder.clone().baseUrl(endpoints.authorizationBaseUrl()).build();
        this.readinessClient = builder.clone().baseUrl(endpoints.readinessUrl()).build();
        this.credentialProvider = credentialProvider;
    }

    @Override
    public DeviceClaims authorize(String accessToken, String tenantId, String deviceId) {
        try {
            AuthorizationResponse response = authorizationClient.post()
                    .uri("/authorize-device")
                    .header("X-ReHealth-Service-Credential", credentialProvider.credential())
                    .header("X-Access-Token", accessToken)
                    .body(Map.of("tenantId", tenantId, "deviceId", deviceId))
                    .retrieve()
                    .body(AuthorizationResponse.class);
            if (response == null || !response.authorized()) {
                throw unavailable();
            }
            return new DeviceClaims(response.userId(), response.tenantId(), response.deviceId());
        } catch (RestClientResponseException exception) {
            throw switch (exception.getStatusCode().value()) {
                case 401 -> new DeviceRequestException(HttpStatus.UNAUTHORIZED, "USER_TOKEN_REJECTED");
                case 403 -> new DeviceRequestException(HttpStatus.FORBIDDEN, "DEVICE_AUTHORIZATION_REJECTED");
                default -> unavailable();
            };
        } catch (DeviceRequestException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw unavailable();
        }
    }

    @Override
    public boolean ready() {
        if (credentialProvider.credential().isBlank()) {
            return false;
        }
        try {
            return readinessClient.get()
                    .uri("")
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .is2xxSuccessful();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private DeviceRequestException unavailable() {
        return new DeviceRequestException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_PROVIDER_UNAVAILABLE");
    }

    private record AuthorizationResponse(
            boolean authorized,
            String code,
            String userId,
            String tenantId,
            String deviceId
    ) {
    }
}
