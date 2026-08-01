package org.jeecg.modules.rehealth.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.service.behavior.BehaviorPhotoAnalysisException;
import org.jeecg.modules.rehealth.service.behavior.BehaviorRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Tag(name = "ReHealth Behavior Records")
@RestController
@RequestMapping("/rehealth/mobile/behavior-records")
public class BehaviorRecordController {
    private final BehaviorRecordService service;
    private final AuthenticatedTenantResolver tenantResolver;

    public BehaviorRecordController(BehaviorRecordService service, AuthenticatedTenantResolver tenantResolver) {
        this.service = service;
        this.tenantResolver = tenantResolver;
    }

    @PostMapping(value = "/analyze-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze a camera photo and persist a structured behavior record")
    public Result<BehaviorRecordDto> analyzePhoto(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam("requestId") String requestId,
            @RequestParam("occurredAt") long occurredAt,
            @RequestPart("image") MultipartFile image
    ) {
        LoginUser user = currentUser();
        String authorizedTenant = tenantResolver.resolve(user, tenantId);
        try {
            return Result.OK(service.analyzeAndSave(authorizedTenant, user.getId(), requestId, occurredAt, image));
        } catch (IllegalArgumentException failure) {
            return Result.error(400, failure.getMessage());
        } catch (BehaviorPhotoAnalysisException failure) {
            return Result.error(503, "photo analysis is temporarily unavailable; retry later");
        } catch (IllegalStateException failure) {
            return Result.error(503, "software_db persistence unavailable; retry behavior record");
        }
    }

    @GetMapping("/today")
    @Operation(summary = "List current authenticated user's behavior records for a local calendar day")
    public Result<List<BehaviorRecordDto>> today(
            @RequestHeader(value = CommonConstant.TENANT_ID, required = false) String tenantId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(value = "zoneOffsetMinutes", defaultValue = "480") int zoneOffsetMinutes
    ) {
        if (zoneOffsetMinutes < -1080 || zoneOffsetMinutes > 1080) {
            return Result.error(400, "zoneOffsetMinutes must be between -1080 and 1080");
        }
        LoginUser user = currentUser();
        String authorizedTenant = tenantResolver.resolve(user, tenantId);
        ZoneOffset offset = ZoneOffset.ofTotalSeconds(zoneOffsetMinutes * 60);
        Instant start = date.atStartOfDay().toInstant(offset);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(offset);
        try {
            return Result.OK(service.inWindow(authorizedTenant, user.getId(), start, end));
        } catch (IllegalStateException failure) {
            return Result.error(503, "software_db persistence unavailable; retry behavior record read");
        }
    }

    private LoginUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser user && user.getId() != null && !user.getId().isBlank()) return user;
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }
}
