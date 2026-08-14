package org.jeecg.modules.rehealth.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.mobile.dto.RhiDailySnapshotBatchDto;
import org.jeecg.modules.rehealth.mobile.dto.RhiDailySnapshotResponseDto;
import org.jeecg.modules.rehealth.mobile.service.RhiDailySnapshotService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth RHI Snapshot API")
@RestController
@RequestMapping("/rehealth/mobile/rhi")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class RhiDailySnapshotController {
    private final RhiDailySnapshotService service;

    public RhiDailySnapshotController(RhiDailySnapshotService service) {
        this.service = service;
    }

    @PostMapping("/daily-snapshot")
    @Operation(summary = "Persist authenticated user's locally calculated RHI daily snapshots")
    public Result<RhiDailySnapshotResponseDto> dailySnapshot(@RequestBody RhiDailySnapshotBatchDto request) {
        try {
            return Result.OK(service.persist(currentUserId(), request));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(503, "software_db persistence unavailable; retry RHI snapshot upload");
        }
    }

    private String currentUserId() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser user && user.getId() != null && !user.getId().isBlank()) {
            return user.getId();
        }
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }
}
