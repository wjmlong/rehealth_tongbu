package org.jeecg.modules.rehealth.service.intervention;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZoneId;

@Component
public class HttpDeviceInterventionContextClient implements DeviceInterventionContextClient {
    private final String baseUrl;
    private final String credential;
    private final Duration timeout;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public HttpDeviceInterventionContextClient(
            @Value("${rehealth.device-service.base-url:}") String baseUrl,
            @Value("${rehealth.device-service.credential:}") String credential,
            @Value("${rehealth.device-service.internal-token-file:}") String credentialFile,
            @Value("${rehealth.device-service.timeout-seconds:5}") long timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                baseUrl,
                resolveCredential(credential, credentialFile),
                Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, 30))),
                objectMapper,
                null
        );
    }

    HttpDeviceInterventionContextClient(
            String baseUrl,
            String credential,
            Duration timeout,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.credential = credential == null ? "" : credential.strip();
        this.timeout = timeout;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient == null
                ? HttpClient.newBuilder().connectTimeout(timeout).build()
                : httpClient;
    }

    @Override
    public DeviceInterventionContext fetch(String tenantId, String userId, ZoneId timeZone) {
        requireText(tenantId, "tenantId");
        requireText(userId, "userId");
        if (baseUrl.isBlank() || credential.isBlank()) {
            throw new IllegalStateException("device-service intervention context is not configured");
        }
        URI uri = URI.create(
                baseUrl
                        + "/rehealth/internal/v1/operations/users/"
                        + encode(userId)
                        + "/intervention-context?tenantId="
                        + encode(tenantId)
                        + "&timeZone="
                        + encode(timeZone.getId())
        );
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("X-ReHealth-Service-Credential", credential)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("device-service intervention context request failed");
            }
            JavaType type = objectMapper.getTypeFactory().constructParametricType(
                    DeviceEnvelope.class,
                    DeviceInterventionContext.class
            );
            DeviceEnvelope<DeviceInterventionContext> envelope =
                    objectMapper.readValue(response.body(), type);
            if (envelope == null || !envelope.success || envelope.result == null) {
                throw new IllegalStateException("device-service returned no intervention context");
            }
            return envelope.result;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("device-service intervention context request interrupted", interrupted);
        } catch (IOException failure) {
            throw new IllegalStateException("device-service intervention context is unavailable", failure);
        }
    }

    private static String resolveCredential(String configured, String credentialFile) {
        if (configured != null && !configured.isBlank()) {
            return configured.strip();
        }
        if (credentialFile == null || credentialFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(credentialFile.strip())).strip();
        } catch (IOException failure) {
            throw new IllegalStateException("device-service credential file is unreadable", failure);
        }
    }

    private static String normalizeBaseUrl(String value) {
        return value == null ? "" : value.strip().replaceAll("/+$", "");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DeviceEnvelope<T> {
        public boolean success;
        public T result;
    }
}
