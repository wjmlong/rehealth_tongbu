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
import java.util.List;
import java.util.function.Supplier;

@Tag(name = "ReHealth Mobile Insurance Plan API")
@RestController
@RequestMapping("/rehealth/mobile/insurance")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobilePlanController {
    private final InsuranceMobilePlanService service;
    private final InsuranceMobileCarePlanService carePlanService;

    public InsuranceMobilePlanController(
            InsuranceMobilePlanService service,
            InsuranceMobileCarePlanService carePlanService
    ) {
        this.service = service;
        this.carePlanService = carePlanService;
    }

    @PostMapping("/plans/bind")
    @Operation(summary = "Grant insurance-program consent and bind the current user to a plan")
    public ResponseEntity<Result<InsuranceMobilePlanResponse>> bind(
            @RequestBody InsuranceMobilePlanRequest.Bind request
    ) {
        return respond(() -> service.bind(currentUserId(), request));
    }

    @GetMapping("/plans/bindable-policies")
    @Operation(summary = "List the current user's active policies available for zero-input binding")
    public ResponseEntity<Result<java.util.List<InsuranceMobileBindablePolicy>>> bindablePolicies() {
        return respond(() -> service.bindablePolicies(currentUserId()));
    }

    @GetMapping("/plans/current")
    @Operation(summary = "Get the current user's active insurance plan binding")
    public ResponseEntity<Result<InsuranceMobilePlanResponse>> current(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(() -> service.current(currentUserId(), tenantId));
    }

    @GetMapping("/plans/active")
    @Operation(summary = "List all active insurer plan bindings owned by the current APP user")
    public ResponseEntity<Result<List<InsuranceMobilePlanResponse>>> active() {
        return respond(() -> service.active(currentUserId()));
    }

    @PostMapping("/plans/{bindingId}/feedback")
    @Operation(summary = "Idempotently upload intervention completion and user feedback")
    public ResponseEntity<Result<Map<String, Object>>> feedback(
            @PathVariable String bindingId,
            @RequestBody InsuranceMobilePlanRequest.Feedback request
    ) {
        return respond(() -> service.feedback(currentUserId(), bindingId, request));
    }

    @GetMapping("/care-plans/current")
    @Operation(summary = "List current institution-authored care plans, today's tasks, and rolling 28-day adherence")
    public ResponseEntity<Result<List<InsuranceMobileCarePlanResponse.Plan>>> currentCarePlans() {
        return respond(() -> carePlanService.current(currentUserId()));
    }

    @PostMapping("/care-plan-occurrences/{occurrenceId}/feedback")
    @Operation(summary = "Idempotently score a versioned care-plan occurrence")
    public ResponseEntity<Result<Map<String, Object>>> occurrenceFeedback(
            @PathVariable String occurrenceId,
            @RequestBody InsuranceMobilePlanRequest.OccurrenceFeedback request
    ) {
        return respond(() -> carePlanService.feedback(currentUserId(), occurrenceId, request));
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
