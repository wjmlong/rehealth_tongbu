package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.careplan.CarePlanVersionException;
import org.jeecg.modules.rehealth.careplan.CarePlanVersionRequest;
import org.jeecg.modules.rehealth.careplan.CarePlanVersionResponse;
import org.jeecg.modules.rehealth.careplan.CarePlanVersionService;
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

@Tag(name = "ReHealth Insurance Versioned Care Plan API")
@RestController
@RequestMapping("/rehealth/insurance/v1")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceCarePlanController {
    private static final String OWNER_TYPE = "insurance";
    private static final String VIEW_PERMISSION = "rehealth:insurance:care-plan:view";
    private static final String MANAGE_PERMISSION = "rehealth:insurance:care-plan:manage";
    private static final String PUBLISH_PERMISSION = "rehealth:insurance:care-plan:publish";

    private final CarePlanVersionService versionService;
    private final InsuranceCarePlanSubjectAccess subjectAccess;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceCarePlanController(
            CarePlanVersionService versionService,
            InsuranceCarePlanSubjectAccess subjectAccess,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.versionService = versionService;
        this.subjectAccess = subjectAccess;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping("/interventions/{subjectId}/care-plans")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List versioned institution care plans for an assigned insurance subject")
    public ResponseEntity<Result<List<CarePlanVersionResponse.Plan>>> list(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String subjectId
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            InsuranceCarePlanSubjectAccess.Subject subject = subjectAccess.requireAssigned(
                    context.tenantId(), context.scope(), subjectId);
            return versionService.list(context.tenantId(), OWNER_TYPE, subject.subjectRef());
        });
    }

    @PostMapping("/interventions/{subjectId}/care-plans")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Create a mutable version 1 draft for an assigned insurance subject")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> createDraft(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String subjectId,
            @RequestBody CarePlanVersionRequest.CreateDraft request
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            InsuranceCarePlanSubjectAccess.Subject subject = subjectAccess.requireAssigned(
                    context.tenantId(), context.scope(), subjectId);
            return versionService.createDraft(
                    context.tenantId(), OWNER_TYPE, Integer.toString(context.tenantId()),
                    subject.subjectRef(), subject.rehealthUserId(), context.user().getId(), request);
        });
    }

    @GetMapping("/care-plans/{planId}")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get a care plan, immutable published revisions, and the current draft")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> detail(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.get(context.tenantId(), OWNER_TYPE, planId);
        });
    }

    @PutMapping("/care-plans/{planId}/draft")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Replace the mutable draft content using optimistic locking")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> updateDraft(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId,
            @RequestBody CarePlanVersionRequest.UpdateDraft request
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.updateDraft(
                    context.tenantId(), OWNER_TYPE, planId, context.user().getId(), request);
        });
    }

    @PostMapping("/care-plans/{planId}/revisions")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Clone the latest published revision into a new mutable draft")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> createRevision(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId,
            @RequestBody CarePlanVersionRequest.CreateRevision request
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.cloneRevision(
                    context.tenantId(), OWNER_TYPE, planId, context.user().getId(), request);
        });
    }

    @PostMapping("/care-plans/{planId}/draft/discard")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Discard the mutable draft while preserving its audit snapshot")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> discardDraft(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId,
            @RequestBody CarePlanVersionRequest.DiscardDraft request
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.discardDraft(
                    context.tenantId(), OWNER_TYPE, planId, context.user().getId(), request);
        });
    }

    @PostMapping("/care-plans/{planId}/publish")
    @RequiresPermissions(PUBLISH_PERMISSION)
    @Operation(summary = "Publish and freeze the current draft at a non-retroactive effective time")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> publish(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId,
            @RequestBody CarePlanVersionRequest.Publish request
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.publish(
                    context.tenantId(), OWNER_TYPE, planId, context.user().getId(), request);
        });
    }

    @PostMapping("/care-plans/{planId}/withdraw")
    @RequiresPermissions(PUBLISH_PERMISSION)
    @Operation(summary = "Withdraw the latest published revision and cancel its future occurrences")
    public ResponseEntity<Result<CarePlanVersionResponse.Plan>> withdraw(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String planId,
            @RequestBody CarePlanVersionRequest.Withdraw request
    ) {
        return respond(() -> {
            Context context = authorizePlan(tenantHeader, planId);
            return versionService.withdraw(
                    context.tenantId(), OWNER_TYPE, planId, context.user().getId(), request);
        });
    }

    private Context authorizePlan(String tenantHeader, String planId) {
        Context context = context(tenantHeader);
        String subjectRef = versionService.subjectRef(context.tenantId(), OWNER_TYPE, planId);
        subjectAccess.requireAssigned(context.tenantId(), context.scope(), subjectRef);
        return context;
    }

    private Context context(String tenantHeader) {
        LoginUser user = currentUser();
        int tenantId = tenantAccessGuard.requireTenant(user, tenantHeader);
        InsuranceAssignmentScope scope = tenantAccessGuard.assignmentScope(user, tenantId);
        return new Context(tenantId, user, scope);
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser) return loginUser;
        throw InsuranceApiException.forbidden("authenticated insurance staff account is required");
    }

    private <T> ResponseEntity<Result<T>> respond(Supplier<T> action) {
        try {
            return ResponseEntity.ok(Result.OK(action.get()));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        } catch (CarePlanVersionException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }

    private record Context(int tenantId, LoginUser user, InsuranceAssignmentScope scope) {
    }
}
