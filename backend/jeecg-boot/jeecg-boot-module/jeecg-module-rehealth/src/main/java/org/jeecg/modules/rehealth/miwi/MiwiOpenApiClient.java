package org.jeecg.modules.rehealth.miwi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal client for the Miwi (云米) OpenAPI.
 *
 * Token contract per vendor OpenAPI doc V1.6.5: {@code password = MD5(AppKey + AppId + Timestamp)},
 * exchanged through {@code /api/token/get_token} for an AccessToken. Every subsequent OpenAPI
 * call sends the token in the {@code Authorization} request header and uses {@code POST [json]}.
 * The OpenAPI uses {@code Code == 0} for success (unlike the vendor push channel where code == 1).
 *
 * MD5 here is a vendor protocol requirement, not our security choice; see
 * REHEALTH_MIWI_4G_WATCH.md for the hardening asks we sent back to the vendor.
 */
@Component
public class MiwiOpenApiClient {
    private static final Logger log = LoggerFactory.getLogger(MiwiOpenApiClient.class);
    private static final String TOKEN_PATH = "/api/token/get_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final MiwiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile String cachedToken;
    private volatile Instant cachedTokenExpiresAt = Instant.EPOCH;

    public MiwiOpenApiClient(MiwiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /** Returns a cached AccessToken, refreshing through get_token when expired. */
    public synchronized String getAccessToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiresAt)) {
            return cachedToken;
        }
        requireConfigured();
        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("AppId", properties.getAppId());
        request.put("Timestamp", timestamp);
        request.put("Password", md5Hex(properties.getAppKey() + properties.getAppId() + timestamp));

        Map<String, Object> response = postJson(properties.getApiBaseUrl() + TOKEN_PATH, request, null, AUTHORIZATION_HEADER);
        Object code = response.get("Code");
        if (!(code instanceof Number numberValue) || numberValue.intValue() != 0) {
            throw new IllegalStateException("miwi get_token failed with Code=" + code);
        }
        Object token = firstNonNull(response, "AccessToken", "accessToken", "Token");
        if (token == null) {
            throw new IllegalStateException("miwi get_token returned no AccessToken");
        }
        cachedToken = String.valueOf(token);
        cachedTokenExpiresAt = Instant.now().plusSeconds(properties.getTokenTtlSeconds());
        return cachedToken;
    }

    /**
     * Generic OpenAPI POST with AccessToken, for pull-based queries such as
     * latest health data by IMEI. Path must come from the vendor document.
     */
    public Map<String, Object> post(String path, Map<String, Object> body) {
        requireConfigured();
        return postJson(properties.getApiBaseUrl() + path, body, getAccessToken(), AUTHORIZATION_HEADER);
    }

    private Map<String, Object> postJson(String url, Map<String, Object> body, String accessToken, String authHeaderName) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body == null ? Map.of() : body),
                            StandardCharsets.UTF_8
                    ));
            if (accessToken != null && authHeaderName != null) {
                builder.header(authHeaderName, accessToken);
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("miwi openapi http status " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
            });
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("miwi openapi call failed: {}", e.getMessage());
            throw new IllegalStateException("miwi openapi call failed", e);
        }
    }

    private void requireConfigured() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("miwi integration disabled (rehealth.miwi.enabled=false)");
        }
        if (isBlank(properties.getApiBaseUrl()) || isBlank(properties.getAppId()) || isBlank(properties.getAppKey())) {
            throw new IllegalStateException("miwi api-base-url/app-id/app-key are not configured");
        }
    }

    private Object firstNonNull(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String md5Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("md5 unavailable", e);
        }
    }
}
