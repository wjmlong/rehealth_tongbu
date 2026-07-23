package org.jeecg.modules.rehealth.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jeecg.config.shiro.IgnoreAuth;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth Internal Identity API")
@RestController
@RequestMapping("/rehealth/internal/v1/identity")
public class InternalIdentityController {
    private final InternalIdentityAuthorizationService authorizationService;

    public InternalIdentityController(InternalIdentityAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @IgnoreAuth
    @PostMapping("/authorize-device")
    @Operation(summary = "Resolve a Jeecg user token and authorize an active device binding")
    public ResponseEntity<InternalDeviceAuthorizationResponseDto> authorizeDevice(
            @RequestHeader(value = "X-ReHealth-Service-Credential", required = false) String serviceCredential,
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @RequestHeader(value = "X-ReHealth-User-Id", required = false) String spoofedUserId,
            @RequestHeader(value = "X-ReHealth-Tenant-Id", required = false) String spoofedTenantId,
            @RequestHeader(value = "X-ReHealth-Device-Id", required = false) String spoofedDeviceId,
            @RequestBody(required = false) InternalDeviceAuthorizationRequestDto request
    ) {
        InternalAuthorizationDecision decision = authorizationService.authorize(
                serviceCredential,
                accessToken,
                spoofedUserId,
                spoofedTenantId,
                spoofedDeviceId,
                request
        );
        HttpStatus status = switch (decision.status()) {
            case ALLOWED -> HttpStatus.OK;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(decision.response());
    }
}
