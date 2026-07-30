package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Verifies the Viomi access token using HMAC-SHA256 (JWT HS256) with the shared AppKey.
 *
 * <p>Implemented with the JDK crypto APIs only (no external JWT dependency). The token is delivered
 * either in the {@code Authorization: Bearer <jwt>} header or in the request body {@code AccessToken}
 * field. On success the AppId (and optional Imei) are extracted from the JWT claims.</p>
 */
@Component
public class ViomiJwtVerifier {

    private final ObjectMapper objectMapper;
    private final String appKey;
    private final String configuredAppId;

    public ViomiJwtVerifier(ObjectMapper objectMapper, ViomiAdapterProperties properties) {
        this.objectMapper = objectMapper;
        this.appKey = properties.getAppKey();
        this.configuredAppId = properties.getAppId();
    }

    public ViomiAuthResult verify(String token) {
        if (token == null || token.isBlank() || appKey == null || appKey.isBlank()) {
            return ViomiAuthResult.failed();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return ViomiAuthResult.failed();
        }
        String signingInput = parts[0] + "." + parts[1];
        String expectedSignature = base64Url(hmacSha256(signingInput.getBytes(StandardCharsets.UTF_8), appKey));
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return ViomiAuthResult.failed();
        }
        try {
            JsonNode payload = objectMapper.readTree(decodeBase64Url(parts[1]));
            String appId = firstText(payload, "appId", "AppId", "userId", "UserId");
            if (appId == null) {
                appId = configuredAppId;
            }
            String imei = firstText(payload, "imei", "Imei");
            return ViomiAuthResult.success(appId, imei);
        } catch (Exception e) {
            return ViomiAuthResult.failed();
        }
    }

    private byte[] hmacSha256(byte[] data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HS256 signature computation failed", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String decodeBase64Url(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        if (ab.length != bb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
            if (value.isNumber()) {
                return value.asText();
            }
        }
        return null;
    }
}
