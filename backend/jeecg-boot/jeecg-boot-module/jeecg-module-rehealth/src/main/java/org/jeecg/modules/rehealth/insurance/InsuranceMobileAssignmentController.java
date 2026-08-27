package org.jeecg.modules.rehealth.insurance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

/**
 * Mobile-side service contact API. Phase 1 exposes the current service
 * contact; phase 2 implements the scan-association flow (scan → confirm),
 * while invite-code redemption stays reserved.
 */
//update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增移动端服务专员控制器-----------
@Tag(name = "ReHealth Mobile Insurance Assignment API")
@RestController
@RequestMapping("/rehealth/mobile/insurance")
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceMobileAssignmentController {
    private final InsuranceAssignmentService service;
    private final InsuranceScanLinkService scanLinkService;

    public InsuranceMobileAssignmentController(
            InsuranceAssignmentService service,
            InsuranceScanLinkService scanLinkService
    ) {
        this.service = service;
        this.scanLinkService = scanLinkService;
    }

    @GetMapping("/assignments/current")
    @Operation(summary = "Get the current APP user's active service contact")
    public ResponseEntity<Result<InsuranceAssignmentResponse.MobileContact>> current() {
        return respond(() -> service.mobileContact(currentUserId()));
    }

    @PostMapping("/assignments/redeem")
    @Operation(summary = "Reserved phase-2 contract: redeem an invite code")
    public ResponseEntity<Result<?>> redeem(@RequestBody InsuranceAssignmentRequest.InviteCode request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Result.error(501, "邀请码关联接口已预留，二期开放"));
    }

    //update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码 → 预览 → 确认-----------
    @PostMapping("/assignments/scan")
    @Operation(summary = "Scan an employee QR code and create a pending session")
    public ResponseEntity<Result<InsuranceScanLinkResponse.ScanPreview>> scan(
            @RequestBody InsuranceAssignmentRequest.Scan request
    ) {
        return respond(() -> scanLinkService.scan(request.employeeCode(), currentUserId()));
    }

    @PostMapping("/assignments/scan/{sessionId}/confirm")
    @Operation(summary = "Confirm the scan session; creates or replaces the service relationship")
    public ResponseEntity<Result<InsuranceScanLinkResponse.ConfirmResult>> confirm(
            @PathVariable String sessionId,
            @RequestBody InsuranceScanLinkResponse.ConfirmRequest request
    ) {
        return respond(() -> scanLinkService.confirm(sessionId, currentUserId(),
                request == null || request.replaceExisting()));
    }

    @PostMapping("/assignments/scan/{sessionId}/cancel")
    @Operation(summary = "Cancel a pending scan session")
    public ResponseEntity<Result<Boolean>> cancel(@PathVariable String sessionId) {
        return respond(() -> {
            scanLinkService.cancel(sessionId, currentUserId());
            return Boolean.TRUE;
        });
    }
    //update-end---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码 → 预览 → 确认-----------

    private String currentUserId() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser && loginUser.getId() != null && !loginUser.getId().isBlank()) {
            return loginUser.getId();
        }
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }

    private <T> ResponseEntity<Result<T>> respond(Supplier<T> action) {
        try {
            return ResponseEntity.ok(Result.OK(action.get()));
        } catch (InsuranceApiException e) {
            return ResponseEntity.status(e.status()).body(Result.error(e.status().value(), e.getMessage()));
        }
    }
}
//update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增移动端服务专员控制器-----------
