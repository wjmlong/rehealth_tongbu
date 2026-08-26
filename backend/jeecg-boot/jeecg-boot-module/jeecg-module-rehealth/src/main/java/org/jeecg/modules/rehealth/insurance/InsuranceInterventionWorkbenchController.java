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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@Tag(name = "ReHealth Insurance Intervention Workbench API")
@RestController
@RequestMapping("/rehealth/insurance/v1")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceInterventionWorkbenchController {
    private static final String VIEW_PERMISSION = "rehealth:insurance:risk:view";
    private static final String MANAGE_PERMISSION = "rehealth:insurance:intervention:manage";

    private final InsuranceInterventionWorkbenchService service;
    private final InsuranceInterventionReportService reportService;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsuranceInterventionWorkbenchController(
            InsuranceInterventionWorkbenchService service,
            InsuranceInterventionReportService reportService,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.service = service;
        this.reportService = reportService;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping("/interventions/dashboard")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get assigned-subject insurer intervention dashboard")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.Dashboard>> dashboard(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.dashboard(context.tenantId(), context.scope());
        });
    }

    @GetMapping("/interventions")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List assigned subjects in the intervention workflow")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.SubjectPage>> subjects(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @RequestParam(value = "pageNo", defaultValue = "1") int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "workflowStatus", required = false) String workflowStatus
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.subjects(context.tenantId(), context.scope(), pageNo, pageSize, keyword, workflowStatus);
        });
    }

    @GetMapping("/interventions/{subjectId}")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Get risk, RHI, plan, feedback, owner, action and attribution evidence")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.SubjectDetail>> subject(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String subjectId
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.subject(context.tenantId(), context.scope(), subjectId);
        });
    }

    //update-begin---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】人群级干预效果评估报告数据聚合端点------------
    @GetMapping("/interventions/report-data")
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "Aggregate population-level intervention effect report data for the assigned subject scope")
    public ResponseEntity<Result<InsuranceInterventionReportResponse.ReportData>> interventionReportData(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @RequestParam(value = "periodDays", defaultValue = "30") int periodDays
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return reportService.reportData(context.tenantId(), context.scope(), periodDays);
        });
    }
    //update-end---author:rehealth ---date:2026-08-24  for：【需求:干预效果评估报告】人群级干预效果评估报告数据聚合端点------------

    @PostMapping("/interventions/{subjectId}/actions")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Create an audited action for an assigned insurance subject")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.Action>> createAction(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String subjectId,
            @RequestBody InsuranceInterventionWorkbenchRequest.CreateAction request
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.createAction(context.tenantId(), context.scope(), context.user().getId(), subjectId, request);
        });
    }

    @PostMapping("/interventions/actions/batch")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Create audited actions for up to 100 assigned insurance subjects atomically")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.BatchActions>> createActions(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @RequestBody InsuranceInterventionWorkbenchRequest.BatchCreateAction request
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.createActions(context.tenantId(), context.scope(), context.user().getId(), request);
        });
    }

    @PutMapping("/intervention-actions/{actionId}")
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Update an audited action within the current employee's responsibility scope")
    public ResponseEntity<Result<InsuranceInterventionWorkbenchResponse.Action>> updateAction(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantHeader,
            @PathVariable String actionId,
            @RequestBody InsuranceInterventionWorkbenchRequest.UpdateAction request
    ) {
        return respond(() -> {
            Context context = context(tenantHeader);
            return service.updateAction(context.tenantId(), context.scope(), context.user().getId(), actionId, request);
        });
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
        }
    }

    private record Context(int tenantId, LoginUser user, InsuranceAssignmentScope scope) {}
}
