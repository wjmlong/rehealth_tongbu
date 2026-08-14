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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "ReHealth Insurance Business Import API")
@RestController
@RequestMapping("/rehealth/insurance/v1/imports")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceImportController {
    private static final String IMPORT_PERMISSION = "rehealth:insurance:business:import";

    private final InsuranceImportService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceImportController(
            InsuranceImportService service,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping("/subjects")
    @RequiresPermissions(IMPORT_PERMISSION)
    @Operation(summary = "Idempotently bind ReHealth users to insurer subject references")
    public ResponseEntity<Result<InsuranceImportResponse.BatchResult>> subjects(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceImportRequest.SubjectBatch request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.importSubjects(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
        });
    }

    @PostMapping("/policies")
    @RequiresPermissions(IMPORT_PERMISSION)
    @Operation(summary = "Idempotently import tenant-scoped insurance policies")
    public ResponseEntity<Result<InsuranceImportResponse.BatchResult>> policies(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceImportRequest.PolicyBatch request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.importPolicies(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
        });
    }

    @PostMapping("/claims")
    @RequiresPermissions(IMPORT_PERMISSION)
    @Operation(summary = "Idempotently import claims and validate policy/insured relations")
    public ResponseEntity<Result<InsuranceImportResponse.BatchResult>> claims(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceImportRequest.ClaimBatch request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.importClaims(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
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
