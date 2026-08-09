package org.jeecg.modules.rehealth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.service.RehealthUserHealthService;
import org.jeecg.modules.rehealth.vo.RehealthPatientPageVO;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "ReHealth 租户患者健康聚合")
@RestController
@RequestMapping("/rehealth/admin/v1")
public class RehealthUserHealthAdminController {
    private final RehealthUserHealthService service;

    public RehealthUserHealthAdminController(RehealthUserHealthService service) {
        this.service = service;
    }

    @GetMapping("/patients")
    @RequiresPermissions("rehealth:admin:patient:view")
    public Result<RehealthPatientPageVO> list(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String riskLevel
    ) {
        return Result.ok(service.listPatients(
                tenantId, currentUser().getId(), pageNo, pageSize, keyword, riskLevel));
    }

    @GetMapping("/patients/{patientId}")
    @RequiresPermissions("rehealth:admin:patient:view")
    public Result<RehealthUserHealthVO> detail(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String patientId
    ) {
        return Result.ok(service.patientDetail(tenantId, currentUser().getId(), patientId));
    }

    @GetMapping("/users")
    @RequiresPermissions("rehealth:admin:patient:view")
    public Result<?> disabledLegacyUsers() {
        Result<Object> result = Result.error("DEPRECATED_USE_REHEALTH_ADMIN_V1_PATIENTS");
        result.setCode(HttpStatus.GONE.value());
        return result;
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (!(principal instanceof LoginUser loginUser) || loginUser.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED");
        }
        return loginUser;
    }
}
