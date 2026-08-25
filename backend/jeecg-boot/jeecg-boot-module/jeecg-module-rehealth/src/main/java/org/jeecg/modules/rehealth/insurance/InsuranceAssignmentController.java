package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

/**
 * Insurance-side user service assignment API.
 *
 * <p>Phase 1 implements phone-based claiming, transfer, end, and scope-aware
 * listing/history. The invite-code and scan endpoints are reserved contracts
 * that return {@code 501} until phase 2.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系控制器-----------
@Tag(name = "ReHealth Insurance User Assignment API")
@RestController
@RequestMapping("/rehealth/insurance/v1/assignments")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceAssignmentController {
    private static final String MANAGE_PERMISSION = "rehealth:insurance:assignment:manage";
    private static final String TRANSFER_PERMISSION = "rehealth:insurance:assignment:transfer";
    private static final String VIEW_PERMISSION = "rehealth:insurance:assignment:view";

    private final InsuranceAssignmentService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceAssignmentController(
            InsuranceAssignmentService service,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @PostMapping("/claim")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Claim an enrolled user by phone number as the current employee")
    public ResponseEntity<Result<InsuranceAssignmentResponse.Claimed>> claim(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceAssignmentRequest.Claim request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.claim(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
        });
    }

    @PostMapping("/invite-codes")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Reserved phase-2 contract: generate a one-time invite code")
    public ResponseEntity<Result<?>> inviteCodes(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceAssignmentRequest.InviteCode request
    ) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Result.error(501, "邀请码关联接口已预留，二期开放"));
    }

    @PostMapping("/transfer")
    @RequiresPermissions(TRANSFER_PERMISSION)
    @Operation(summary = "Transfer one or more enrollments between employees, preserving history")
    public ResponseEntity<Result<InsuranceAssignmentResponse.TransferResult>> transfer(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceAssignmentRequest.Transfer request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.transfer(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
        });
    }

    @PostMapping("/{assignmentId}/end")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "End a single active service relationship")
    public ResponseEntity<Result<InsuranceAssignmentResponse.EndResult>> end(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String assignmentId,
            @RequestBody InsuranceAssignmentRequest.End request
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            return service.end(tenant, user.getId(), assignmentId,
                    tenantAccessGuard.assignmentScope(user, tenant), request.reason());
        });
    }

    @GetMapping("/mine")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List the current employee's active service relationships")
    public ResponseEntity<Result<InsuranceAssignmentResponse.Page>> mine(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            tenantAccessGuard.assignmentScope(user, tenant);
            return service.mine(tenant, user.getId(), pageNo, pageSize);
        });
    }

    @GetMapping("/department")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List the department team's active service relationships (manager only)")
    public ResponseEntity<Result<InsuranceAssignmentResponse.Page>> department(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            InsuranceAssignmentScope scope = tenantAccessGuard.assignmentScope(user, tenant);
            return service.department(tenant, scope, pageNo, pageSize);
        });
    }

    @GetMapping("/enrollments")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List the tenant enrollment pool with current PRIMARY owners for claiming")
    public ResponseEntity<Result<InsuranceAssignmentResponse.EnrollmentPage>> enrollments(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            tenantAccessGuard.assignmentScope(user, tenant);
            return service.enrollmentPool(tenant, pageNo, pageSize, keyword);
        });
    }

    @GetMapping("/{enrollmentId}/history")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get one enrollment's responsibility chain and change log")
    public ResponseEntity<Result<InsuranceAssignmentResponse.History>> history(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String enrollmentId
    ) {
        return respond(() -> {
            LoginUser user = currentUser();
            int tenant = tenantAccessGuard.requireTenant(user, tenantId);
            return service.history(tenant, enrollmentId, tenantAccessGuard.assignmentScope(user, tenant));
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
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增服务关系控制器-----------
