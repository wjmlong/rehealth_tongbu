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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "ReHealth Insurance Risk API")
@RestController
@RequestMapping("/rehealth/insurance/v1")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceRiskController {
    private static final String VIEW_PERMISSION = "rehealth:insurance:risk:view";

    private final InsuranceRiskService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceRiskController(
            InsuranceRiskService service,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping("/dashboard/risk")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get tenant-scoped insurer risk dashboard")
    public ResponseEntity<Result<InsuranceRiskResponse.Dashboard>> dashboard(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(() -> service.dashboard(tenantAccessGuard.requireTenant(currentUser(), tenantId)));
    }

    @GetMapping("/insureds")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List tenant-scoped insured subjects and their latest verified risk")
    public ResponseEntity<Result<InsuranceRiskResponse.InsuredPage>> insureds(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "riskLevel", required = false) String riskLevel
    ) {
        return respond(() -> service.insureds(
                tenantAccessGuard.requireTenant(currentUser(), tenantId),
                pageNo,
                pageSize,
                keyword,
                riskLevel
        ));
    }

    @GetMapping("/insureds/{subjectId}")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get one tenant-scoped insured subject without direct identifiers")
    public ResponseEntity<Result<InsuranceRiskResponse.InsuredDetail>> insured(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable("subjectId") String subjectId
    ) {
        return respond(() -> service.insured(
                tenantAccessGuard.requireTenant(currentUser(), tenantId),
                subjectId
        ));
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
            return ResponseEntity.status(e.status())
                    .body(Result.error(e.status().value(), e.getMessage()));
        }
    }
}
