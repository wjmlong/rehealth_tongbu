package org.jeecg.modules.rehealth.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.mobile.dto.RdiDailySnapshotBatchDto;
import org.jeecg.modules.rehealth.mobile.dto.RdiDailySnapshotResponseDto;
import org.jeecg.modules.rehealth.mobile.service.RdiDailySnapshotService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth RDI Snapshot API")
@RestController
@RequestMapping("/rehealth/mobile/rdi")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class RdiDailySnapshotController {
    private final RdiDailySnapshotService service;

    public RdiDailySnapshotController(RdiDailySnapshotService service) {
        this.service = service;
    }

    @PostMapping("/daily-snapshot")
    @Operation(summary = "Persist authenticated user's locally calculated RDI daily snapshots")
    public Result<RdiDailySnapshotResponseDto> dailySnapshot(@RequestBody RdiDailySnapshotBatchDto request) {
        try {
            return Result.OK(service.persist(currentUserId(), request));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(503, "software_db persistence unavailable; retry RDI snapshot upload");
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
