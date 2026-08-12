package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

@Tag(name = "ReHealth Mobile Insurance Plan API")
@RestController
@RequestMapping("/rehealth/mobile/insurance")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobilePlanController {
    private final InsuranceMobilePlanService service;

    public InsuranceMobilePlanController(InsuranceMobilePlanService service) {
        this.service = service;
    }

    @PostMapping("/plans/bind")
    @Operation(summary = "Grant insurance-program consent and bind the current user to a plan")
    public ResponseEntity<Result<InsuranceMobilePlanResponse>> bind(
            @RequestBody InsuranceMobilePlanRequest.Bind request
    ) {
        return respond(() -> service.bind(currentUserId(), request));
    }

    @GetMapping("/plans/current")
    @Operation(summary = "Get the current user's active insurance plan binding")
    public ResponseEntity<Result<InsuranceMobilePlanResponse>> current(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(() -> service.current(currentUserId(), tenantId));
    }

    @PostMapping("/plans/{bindingId}/feedback")
    @Operation(summary = "Idempotently upload intervention completion and user feedback")
    public ResponseEntity<Result<Map<String, Object>>> feedback(
            @PathVariable String bindingId,
            @RequestBody InsuranceMobilePlanRequest.Feedback request
    ) {
        return respond(() -> service.feedback(currentUserId(), bindingId, request));
    }

    private String currentUserId() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser && loginUser.getId() != null && !loginUser.getId().isBlank()) {
            return loginUser.getId();
        }
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }

    private <T> ResponseEntity<Result<T>> respond(Supplier<T> action) {
        try {
            return ResponseEntity.ok(Result.OK(action.get()));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }
}
