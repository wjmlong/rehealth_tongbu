package org.jeecg.modules.rehealth.miwi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jeecg.config.shiro.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Receives health data pushed by the Miwi (云米/MiwiTracker) vendor cloud for
 * 4G watches (S8/S9/GS20/GS17/A67/K9L).
 *
 * Vendor push protocol notes:
 * - body: {"DataType":"Health","ResultData":"<escaped JSON string>"}
 * - success response contract: {"code":1}
 * - the vendor protocol carries no signature, so this endpoint requires a
 *   pre-shared callback token configured as rehealth.miwi.callback-token and
 *   provided by the vendor as a query parameter on the callback URL.
 */
@Tag(name = "ReHealth Miwi 4G Watch Callback")
@RestController
@RequestMapping("/rehealth/miwi")
public class MiwiCallbackController {
    private static final Logger log = LoggerFactory.getLogger(MiwiCallbackController.class);

    private final MiwiProperties properties;
    private final MiwiPushService pushService;

    public MiwiCallbackController(MiwiProperties properties, MiwiPushService pushService) {
        this.properties = properties;
        this.pushService = pushService;
    }

    @IgnoreAuth
    @PostMapping("/push")
    @Operation(summary = "Miwi vendor-cloud health data push callback (token protected)")
    public ResponseEntity<Map<String, Object>> push(
            @RequestParam(value = "token", required = false) String token,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        if (!properties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(vendorResponse(0, "integration disabled"));
        }
        if (!tokenMatches(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(vendorResponse(0, "invalid token"));
        }

        try {
            MiwiPushService.MiwiPushResult result = pushService.handlePush(body);
            // Always ack with code=1 for processed/skipped payloads so the vendor
            // does not retry unbound-device or unsupported-metric messages forever.
            Map<String, Object> response = vendorResponse(1, result.status);
            response.put("accepted", result.accepted);
            response.put("measurementCount", result.measurementCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("miwi push processing failed: {}", e.getMessage());
            // code=0 signals the vendor to retry later (transient failure, e.g. DB down).
            return ResponseEntity.ok(vendorResponse(0, "temporary processing failure"));
        }
    }

    private boolean tokenMatches(String provided) {
        String expected = properties.getCallbackToken();
        if (expected == null || expected.isBlank()) {
            // Refuse to run an open callback endpoint without a configured token.
            return false;
        }
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8)
        );
    }

    private Map<String, Object> vendorResponse(int code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }
}
