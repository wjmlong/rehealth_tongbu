package org.jeecg.modules.rehealth.viomi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViomiJwtVerifierTest {

    private static final String APP_KEY = "super-secret-app-key";

    private ViomiJwtVerifier verifier() {
        ViomiAdapterProperties properties = new ViomiAdapterProperties() {
            @Override
            public String getAppKey() {
                return APP_KEY;
            }

            @Override
            public String getAppId() {
                return "cfg-app-id";
            }
        };
        return new ViomiJwtVerifier(new ObjectMapper(), properties);
    }

    private String signedToken(Map<String, Object> claims) throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new ObjectMapper().writeValueAsBytes(claims));
        String signingInput = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));
        return signingInput + "." + sig;
    }

    @Test
    void verifiesValidTokenAndExtractsClaims() throws Exception {
        String token = signedToken(Map.of("appId", "viomi-app-1", "imei", "7809101598"));
        ViomiAuthResult result = verifier().verify(token);
        assertTrue(result.valid);
        assertEquals("viomi-app-1", result.appId);
        assertEquals("7809101598", result.imei);
    }

    @Test
    void rejectsTokenSignedWithWrongKey() throws Exception {
        String token = signedToken(Map.of("appId", "viomi-app-1"));
        ViomiJwtVerifier wrongKey = new ViomiJwtVerifier(new ObjectMapper(), new ViomiAdapterProperties() {
            @Override
            public String getAppKey() {
                return "different-key";
            }
        });
        assertFalse(wrongKey.verify(token).valid);
    }

    @Test
    void rejectsMalformedToken() {
        assertFalse(verifier().verify("not-a-jwt").valid);
        assertFalse(verifier().verify(null).valid);
        assertFalse(verifier().verify("").valid);
    }
}
