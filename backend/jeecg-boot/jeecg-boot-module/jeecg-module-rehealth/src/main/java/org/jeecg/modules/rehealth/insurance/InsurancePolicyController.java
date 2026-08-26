package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

/**
 * Insurance-side policy dispatch interface: the scoped policy list and the
 * dispatchable subject candidates used by the website import dialog.
 * Both endpoints share the policy import permission.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人接口-----------
@Tag(name = "ReHealth Insurance Policy Dispatch API")
@RestController
@RequestMapping("/rehealth/insurance/v1/policies")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsurancePolicyController {
    private static final String IMPORT_PERMISSION = "rehealth:insurance:business:import";

    private final InsurancePolicyService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsurancePolicyController(
            InsurancePolicyService service,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping
    @RequiresPermissions(IMPORT_PERMISSION)
    @Operation(summary = "List tenant policies inside the current staff responsibility scope")
    public ResponseEntity<Result<InsurancePolicyResponse.PolicyPage>> list(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            InsuranceAssignmentScope scope = tenantAccessGuard.assignmentScopeOrNull(user, tenant);
            return service.list(tenant, scope, pageNo, pageSize, keyword);
        });
    }

    @GetMapping("/dispatchable-subjects")
    @RequiresPermissions(IMPORT_PERMISSION)
    @Operation(summary = "List subjects the current staff may dispatch policies to")
    public ResponseEntity<Result<List<InsurancePolicyResponse.DispatchableSubject>>> dispatchableSubjects(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(required = false) String keyword
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            InsuranceAssignmentScope scope = tenantAccessGuard.assignmentScopeOrNull(user, tenant);
            return service.dispatchableSubjects(tenant, scope, keyword);
        });
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw InsuranceApiException.forbidden("authenticated service account is required");
    }

    private <T> ResponseEntity<Result<T>> respond(Supplier<T> action) {
        try {
            return ResponseEntity.ok(Result.OK(action.get()));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人接口-----------
