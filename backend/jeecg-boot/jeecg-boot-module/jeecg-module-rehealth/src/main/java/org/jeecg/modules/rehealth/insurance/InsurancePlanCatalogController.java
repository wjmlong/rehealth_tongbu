package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePlanCatalogEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePlanCatalogMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Insurance-side plan catalog: product-level health programs referenced by
 * policy default_plan_id, with user-facing names shown in the App.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增计划目录接口-----------
@Tag(name = "ReHealth Insurance Plan Catalog API")
@RestController
@RequestMapping("/rehealth/insurance/v1/plans")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsurancePlanCatalogController {
    private static final String VIEW_PERMISSION = "rehealth:insurance:care-plan:view";
    private static final String MANAGE_PERMISSION = "rehealth:insurance:care-plan:manage";

    private final InsurancePlanCatalogMapper mapper;
    private final InsuranceTenantAccessGuard tenantAccessGuard;

    public InsurancePlanCatalogController(
            InsurancePlanCatalogMapper mapper,
            InsuranceTenantAccessGuard tenantAccessGuard
    ) {
        this.mapper = mapper;
        this.tenantAccessGuard = tenantAccessGuard;
    }

    @GetMapping
    @RequiresPermissions(VIEW_PERMISSION)
    @Operation(summary = "List the tenant plan catalog with display names")
    public ResponseEntity<Result<List<InsurancePlanCatalog.Plan>>> list(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId
    ) {
        return respond(() -> {
            int tenant = tenantAccessGuard.requireTenant(currentUser(), tenantId);
            return mapper.selectList(new LambdaQueryWrapper<InsurancePlanCatalogEntity>()
                            .eq(InsurancePlanCatalogEntity::getTenantId, tenant)
                            .eq(InsurancePlanCatalogEntity::getStatus, "active")
                            .orderByAsc(InsurancePlanCatalogEntity::getPlanId))
                    .stream()
                    .map(entity -> new InsurancePlanCatalog.Plan(
                            entity.getPlanId(), entity.getName(), entity.getDescription(), entity.getStatus()))
                    .toList();
        });
    }

    @PostMapping
    @RequiresPermissions(MANAGE_PERMISSION)
    @Operation(summary = "Create a tenant plan with a display name")
    public ResponseEntity<Result<InsurancePlanCatalog.Plan>> create(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestBody InsurancePlanCatalog.CreateRequest request
    ) {
        return respond(() -> {
            int tenant = tenantAccessGuard.requireTenant(currentUser(), tenantId);
            if (request.planId() == null || request.planId().isBlank() || request.planId().trim().length() > 128) {
                throw InsuranceApiException.badRequest("planId is required and must be at most 128 characters");
            }
            if (request.name() == null || request.name().isBlank() || request.name().trim().length() > 255) {
                throw InsuranceApiException.badRequest("name is required and must be at most 255 characters");
            }
            String planId = request.planId().trim();
            Long existing = mapper.selectCount(new LambdaQueryWrapper<InsurancePlanCatalogEntity>()
                    .eq(InsurancePlanCatalogEntity::getTenantId, tenant)
                    .eq(InsurancePlanCatalogEntity::getPlanId, planId));
            if (existing != null && existing > 0) {
                throw InsuranceApiException.conflict("该计划标识已存在");
            }
            InsurancePlanCatalogEntity entity = new InsurancePlanCatalogEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setTenantId(tenant);
            entity.setPlanId(planId);
            entity.setName(request.name().trim());
            entity.setDescription(request.description() == null ? null : request.description().trim());
            entity.setStatus("active");
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            mapper.insert(entity);
            return new InsurancePlanCatalog.Plan(entity.getPlanId(), entity.getName(), entity.getDescription(), entity.getStatus());
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
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增计划目录接口-----------
