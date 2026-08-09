package org.jeecg.modules.rehealth.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

/** Reads the internal credential from a mounted file and never exposes it. */
@Component
public class RehealthDeviceHealthClient {
    private final String baseUrl;
    private final String credential;
    private final Duration timeout;
    private final HttpClient httpClient;

    @Autowired
    public RehealthDeviceHealthClient(
            @Value("${rehealth.device-service.base-url:}") String baseUrl,
            @Value("${rehealth.device-service.internal-token-file:}") String credentialFile,
            @Value("${rehealth.device-service.timeout-seconds:5}") long timeoutSeconds
    ) {
        this(baseUrl, readCredentialFile(credentialFile),
                Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, 30))), null);
    }

    RehealthDeviceHealthClient(
            String baseUrl,
            String credential,
            Duration timeout,
            HttpClient httpClient
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip().replaceAll("/+$", "");
        this.credential = credential == null ? "" : credential.strip();
        this.timeout = timeout;
        this.httpClient = httpClient == null
                ? HttpClient.newBuilder().connectTimeout(timeout).build()
                : httpClient;
    }

    public JSONObject fetch(String tenantId, String userId) {
        if (baseUrl.isBlank() || credential.isBlank()) {
            throw unavailable("DEVICE_SERVICE_NOT_CONFIGURED", null);
        }
        URI uri = URI.create(baseUrl
                + "/rehealth/internal/v1/operations/users/" + encode(userId)
                + "/health?tenantId=" + encode(tenantId));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("X-ReHealth-Service-Credential", credential)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw unavailable("DEVICE_SERVICE_UNAVAILABLE", null);
            }
            JSONObject envelope = JSONObject.parseObject(response.body());
            if (envelope == null || !envelope.getBooleanValue("success")
                    || envelope.getJSONObject("result") == null) {
                throw unavailable("DEVICE_SERVICE_INVALID_RESPONSE", null);
            }
            return envelope.getJSONObject("result");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable("DEVICE_SERVICE_UNAVAILABLE", interrupted);
        } catch (IOException unavailable) {
            throw unavailable("DEVICE_SERVICE_UNAVAILABLE", unavailable);
        } catch (ResponseStatusException controlled) {
            throw controlled;
        } catch (RuntimeException invalidResponse) {
            throw unavailable("DEVICE_SERVICE_INVALID_RESPONSE", invalidResponse);
        }
    }

    private static ResponseStatusException unavailable(String reason, Throwable cause) {
        return cause == null
                ? new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason)
                : new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason, cause);
    }

    private static String readCredentialFile(String credentialFile) {
        if (credentialFile == null || credentialFile.isBlank()) {
            return "";
        }
        try {
            return Files.readString(Path.of(credentialFile.strip())).strip();
        } catch (IOException failure) {
            throw new IllegalStateException("device-service credential file is unreadable", failure);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
