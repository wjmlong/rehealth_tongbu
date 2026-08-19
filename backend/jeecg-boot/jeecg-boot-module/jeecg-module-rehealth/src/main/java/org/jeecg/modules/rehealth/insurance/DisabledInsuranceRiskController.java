package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth Insurance Risk API")
@RestController
@RequestMapping("/rehealth/insurance/v1")
@ConditionalOnProperty(
        name = "rehealth.software-db.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class DisabledInsuranceRiskController {
    private static final String VIEW_PERMISSION = "rehealth:insurance:risk:view";
    private static final String MESSAGE = "insurance risk data source is not enabled";

    @GetMapping("/dashboard/risk")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get tenant-scoped insurer risk dashboard")
    public ResponseEntity<Result<InsuranceRiskResponse.Dashboard>> dashboard(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return unavailable();
    }

    @GetMapping("/insureds")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List tenant-scoped insured subjects and their latest verified risk")
    public ResponseEntity<Result<InsuranceRiskResponse.InsuredPage>> insureds(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "riskLevel", required = false) String riskLevel,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "minAge", required = false) Integer minAge,
            @RequestParam(value = "maxAge", required = false) Integer maxAge
    ) {
        return unavailable();
    }

    @GetMapping("/insureds/filter-options")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get tenant-scoped channel and age filter options")
    public ResponseEntity<Result<InsuranceRiskResponse.InsuredFilterOptions>> filterOptions(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return unavailable();
    }

    @GetMapping("/insureds/{subjectId}")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get one tenant-scoped insured subject without direct identifiers")
    public ResponseEntity<Result<InsuranceRiskResponse.InsuredDetail>> insured(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable("subjectId") String subjectId
    ) {
        return unavailable();
    }

    private <T> ResponseEntity<Result<T>> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(HttpStatus.SERVICE_UNAVAILABLE.value(), MESSAGE));
    }
}
