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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "ReHealth Insurance Organization Settings API")
@RestController
@RequestMapping("/rehealth/insurance/v1/settings")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceSettingsController {
    private final InsuranceSettingsService service;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceSettingsController(InsuranceSettingsService service, InsuranceTenantAccessGuard tenantAccessGuard) {
        this.service = service;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping("/organization")
    @RequiresPermissions("rehealth:insurance:organization:view")
    @Operation(summary = "Get current insurance organization settings")
    public ResponseEntity<Result<InsuranceSettingsResponse.Organization>> organization(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId) {
        return respond(() -> service.organization(tenantAccessGuard.requireTenant(currentUser(), tenantId)));
    }

    @PutMapping("/organization")
    @RequiresPermissions("rehealth:insurance:organization:edit")
    @Operation(summary = "Update current insurance organization settings")
    public ResponseEntity<Result<InsuranceSettingsResponse.Organization>> updateOrganization(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsuranceSettingsRequest.Organization request) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.updateOrganization(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), request);
        });
    }

    @GetMapping("/departments")
    @RequiresPermissions("rehealth:insurance:member:view")
    public ResponseEntity<Result<java.util.List<InsuranceSettingsResponse.Department>>> departments(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId) {
        return respond(() -> {
            LoginUser user = currentUser();
            int requestedTenant = tenantAccessGuard.requireTenant(user, tenantId);
            return service.departments(requestedTenant, tenantAccessGuard.managerScope(user, requestedTenant));
        });
    }

    @GetMapping("/members")
    @RequiresPermissions("rehealth:insurance:member:view")
    public ResponseEntity<Result<java.util.List<InsuranceSettingsResponse.Member>>> members(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId) {
        return respond(() -> {
            LoginUser user = currentUser();
            int requestedTenant = tenantAccessGuard.requireTenant(user, tenantId);
            return service.members(requestedTenant, tenantAccessGuard.managerScope(user, requestedTenant));
        });
    }

    @GetMapping("/assignments")
    @RequiresPermissions("rehealth:insurance:member:view")
    public ResponseEntity<Result<java.util.List<InsuranceSettingsResponse.Assignment>>> assignments(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId) {
        return respond(() -> {
            LoginUser user = currentUser();
            int requestedTenant = tenantAccessGuard.requireTenant(user, tenantId);
            return service.assignments(requestedTenant, tenantAccessGuard.managerScope(user, requestedTenant));
        });
    }

    @PutMapping("/members/{userId}/status")
    @RequiresPermissions("rehealth:insurance:member:manage")
    public ResponseEntity<Result<InsuranceSettingsResponse.Member>> memberStatus(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String userId,
            @RequestBody InsuranceSettingsRequest.MemberStatus request) {
        return respond(() -> service.updateMemberStatus(tenantAccessGuard.requireTenant(currentUser(), tenantId), userId, request.status()));
    }

    @PutMapping("/members/{userId}/department")
    @RequiresPermissions("rehealth:insurance:member:manage")
    public ResponseEntity<Result<InsuranceSettingsResponse.Member>> memberDepartment(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String userId,
            @RequestBody InsuranceSettingsRequest.MemberDepartment request) {
        return respond(() -> service.updateMemberDepartment(tenantAccessGuard.requireTenant(currentUser(), tenantId), userId, request.departmentId()));
    }

    @PutMapping("/members/{userId}/role")
    @RequiresPermissions("rehealth:insurance:role:assign")
    public ResponseEntity<Result<InsuranceSettingsResponse.Member>> memberRole(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String userId,
            @RequestBody InsuranceSettingsRequest.MemberRole request) {
        return respond(() -> service.updateMemberRole(tenantAccessGuard.requireTenant(currentUser(), tenantId), userId, request.roleCode()));
    }

    @PutMapping("/assignments/{subjectRef}")
    @RequiresPermissions("rehealth:insurance:assignment:manage")
    public ResponseEntity<Result<InsuranceSettingsResponse.Assignment>> assignment(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @PathVariable String subjectRef,
            @RequestBody InsuranceSettingsResponse.AssignmentRequest request) {
        return respond(() -> {
            LoginUser user = currentUser();
            return service.upsertAssignment(tenantAccessGuard.requireTenant(user, tenantId), user.getId(), subjectRef, request);
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
