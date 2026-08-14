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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

@Tag(name = "ReHealth Insurance PSM/RWE/Settlement API")
@RestController
@RequestMapping("/rehealth/insurance/v1")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceStudyController {
    private final InsuranceStudyService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceStudyController(InsuranceStudyService service, InsuranceTenantAccessGuard tenantAccessGuard) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping("/studies")
    @RequiresPermissions("rehealth:insurance:study:manage")
    @Operation(summary = "Create a tenant-scoped PSM study")
    public ResponseEntity<Result<InsuranceStudyResponse.Study>> createStudy(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceStudyRequest.CreateStudy request
    ) {
        return respond((tenant, user) -> service.createStudy(tenant, user.getId(), request), tenantId);
    }

    @GetMapping("/studies")
    @RequiresPermissions("rehealth:insurance:study:view")
    public ResponseEntity<Result<List<InsuranceStudyResponse.Study>>> studies(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond((tenant, user) -> service.studies(tenant), tenantId);
    }

    @PostMapping("/studies/{studyId}/snapshots")
    @RequiresPermissions("rehealth:insurance:study:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Snapshot>> freezeSnapshot(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String studyId
    ) {
        return respond((tenant, user) -> service.freezeSnapshot(tenant, user.getId(), studyId), tenantId);
    }

    @GetMapping("/study-snapshots/{snapshotId}")
    @RequiresPermissions("rehealth:insurance:study:view")
    public ResponseEntity<Result<InsuranceStudyResponse.Snapshot>> snapshot(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String snapshotId
    ) {
        return respond((tenant, user) -> service.snapshot(tenant, snapshotId), tenantId);
    }

    @PostMapping("/studies/{studyId}/jobs")
    @RequiresPermissions("rehealth:insurance:study:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Job>> queueJob(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String studyId,
            @RequestBody InsuranceStudyRequest.QueueJob request
    ) {
        return respond((tenant, user) -> service.queueJob(tenant, user.getId(), studyId, request), tenantId);
    }

    @GetMapping("/study-jobs/{jobId}")
    @RequiresPermissions("rehealth:insurance:study:view")
    public ResponseEntity<Result<InsuranceStudyResponse.Job>> job(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String jobId
    ) {
        return respond((tenant, user) -> service.job(tenant, jobId), tenantId);
    }

    @PutMapping("/study-jobs/{jobId}/result")
    @RequiresPermissions("rehealth:insurance:study:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Result>> completeJob(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String jobId,
            @RequestBody InsuranceStudyRequest.CompleteJob request
    ) {
        return respond((tenant, user) -> service.completeJob(tenant, user.getId(), jobId, request), tenantId);
    }

    @PostMapping("/study-results/{resultId}/review")
    @RequiresPermissions("rehealth:insurance:study:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Result>> reviewResult(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String resultId,
            @RequestBody InsuranceStudyRequest.Review request
    ) {
        return respond((tenant, user) -> service.reviewResult(tenant, user.getId(), resultId, request), tenantId);
    }

    @PostMapping("/studies/{studyId}/reports")
    @RequiresPermissions("rehealth:insurance:report:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Report>> createReport(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String studyId,
            @RequestBody InsuranceStudyRequest.CreateReport request
    ) {
        return respond((tenant, user) -> service.createReport(tenant, user.getId(), studyId, request), tenantId);
    }

    @GetMapping("/reports")
    @RequiresPermissions("rehealth:insurance:report:view")
    public ResponseEntity<Result<List<InsuranceStudyResponse.Report>>> reports(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond((tenant, user) -> service.reports(tenant), tenantId);
    }

    @PostMapping("/reports/{reportId}/review")
    @RequiresPermissions("rehealth:insurance:report:manage")
    public ResponseEntity<Result<InsuranceStudyResponse.Report>> reviewReport(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String reportId,
            @RequestBody InsuranceStudyRequest.Review request
    ) {
        return respond((tenant, user) -> service.reviewReport(tenant, user.getId(), reportId, request), tenantId);
    }

    @PostMapping("/studies/{studyId}/settlements")
    @RequiresPermissions("rehealth:insurance:settlement:operate")
    public ResponseEntity<Result<InsuranceStudyResponse.Settlement>> createSettlement(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String studyId,
            @RequestBody InsuranceStudyRequest.CreateSettlement request
    ) {
        return respond((tenant, user) -> service.createSettlement(tenant, user.getId(), studyId, request), tenantId);
    }

    @GetMapping("/settlements")
    @RequiresPermissions("rehealth:insurance:report:view")
    public ResponseEntity<Result<List<InsuranceStudyResponse.Settlement>>> settlements(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond((tenant, user) -> service.settlements(tenant), tenantId);
    }

    @PostMapping("/settlements/{packageId}/actions")
    @RequiresPermissions("rehealth:insurance:settlement:operate")
    public ResponseEntity<Result<InsuranceStudyResponse.Settlement>> settlementAction(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String packageId,
            @RequestBody InsuranceStudyRequest.SettlementAction request
    ) {
        return respond((tenant, user) -> service.settlementAction(tenant, user.getId(), packageId, request), tenantId);
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw InsuranceApiException.forbidden("authenticated service account is required");
    }

    private <T> ResponseEntity<Result<T>> respond(TenantAction<T> action, String requestedTenant) {
        try {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, requestedTenant);
            return ResponseEntity.ok(Result.OK(action.get(tenant, user)));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }

    @FunctionalInterface
    private interface TenantAction<T> {
        T get(int tenantId, LoginUser user);
    }
}
