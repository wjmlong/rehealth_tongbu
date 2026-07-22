package org.jeecg.modules.rehealth.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.UnauthenticatedException;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.MobileConfigResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RecentTelemetryResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.ingest.writer.HardwarePersistenceUnavailableException;
import org.jeecg.modules.rehealth.service.ReHealthMobileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "ReHealth Mobile API")
@RestController
@RequestMapping("/rehealth/mobile")
public class ReHealthMobileController {
    private final ReHealthMobileService mobileService;

    public ReHealthMobileController(ReHealthMobileService mobileService) {
        this.mobileService = mobileService;
    }

    @IgnoreAuth
    @GetMapping("/health")
    @Operation(summary = "ReHealth mobile API health")
    public Result<HealthResponseDto> health() {
        return Result.OK(mobileService.health());
    }

    @GetMapping("/config")
    @Operation(summary = "ReHealth mobile API config")
    public Result<MobileConfigResponseDto> config() {
        return Result.OK(mobileService.config());
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated user's health profile")
    public Result<PatientProfileDto> profile() {
        return Result.OK(mobileService.profile(currentUserId()));
    }

    @PutMapping("/profile")
    @Operation(summary = "Create or update current authenticated user's health profile")
    public Result<PatientProfileDto> updateProfile(@RequestBody PatientProfileDto profile) {
        try {
            return Result.OK(mobileService.saveProfile(currentUserId(), profile));
        } catch (IllegalStateException e) {
            return Result.error(503, "software_db persistence unavailable; retry profile update");
        }
    }

    @PostMapping("/interviews")
    @Operation(summary = "Persist current authenticated user's health interview")
    public Result<HealthInterviewSubmitRequestDto> submitInterview(
            @RequestBody HealthInterviewSubmitRequestDto request
    ) {
        try {
            return Result.OK(mobileService.submitInterview(currentUserId(), request));
        } catch (IllegalStateException e) {
            return Result.error(503, "software_db persistence unavailable; retry health interview");
        }
    }

    @GetMapping("/interviews/latest")
    @Operation(summary = "Get current authenticated user's latest health interview")
    public Result<HealthInterviewSubmitRequestDto> latestInterview() {
        return Result.OK(mobileService.latestInterview(currentUserId()));
    }

    @PostMapping("/devices/bind")
    @Operation(summary = "Bind wearable device to current ReHealth user")
    public Result<DeviceBindResponseDto> bindDevice(@RequestBody DeviceBindRequestDto request) {
        return Result.OK(mobileService.bindDevice(currentUserId(), request));
    }

    @PostMapping("/measurements/batch")
    @Operation(summary = "Accept wearable telemetry batch through hardware ingestion port")
    public Result<TelemetryBatchResponseDto> uploadMeasurements(@RequestBody TelemetryBatchRequestDto request) {
        if (request != null) {
            request.userId = currentUserId();
        }
        try {
            return Result.OK(mobileService.acceptTelemetryBatch(request));
        } catch (HardwarePersistenceUnavailableException e) {
            return Result.error(503, "hardware telemetry persistence unavailable; retry the same batchId");
        }
    }

    @GetMapping("/measurements/recent")
    @Operation(summary = "Read current authenticated user's recent normalized telemetry")
    public Result<RecentTelemetryResponseDto> recentTelemetry(
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        try {
            return Result.OK(mobileService.recentTelemetry(currentUserId(), limit));
        } catch (HardwarePersistenceUnavailableException e) {
            return Result.error(503, "hardware telemetry query unavailable; retry after hardware_db is enabled");
        }
    }

    private String currentUserId() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof LoginUser loginUser && loginUser.getId() != null && !loginUser.getId().isBlank()) {
            return loginUser.getId();
        }
        throw new UnauthenticatedException("authenticated ReHealth user is required");
    }

    @PostMapping("/features/evaluate")
    @Operation(summary = "Evaluate canonical CVD feature vector through model-service")
    public Result<RiskEvaluateResponseDto> evaluateFeatures(@RequestBody RiskEvaluateRequestDto request) {
        try {
            return Result.OK(mobileService.evaluateFeatures(currentUserId(), request));
        } catch (IllegalStateException e) {
            return Result.error("model-service risk evaluation unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/risk/latest")
    @Operation(summary = "Get latest persisted CVD risk result when software_db persistence is enabled")
    public Result<RiskEvaluateResponseDto> latestRisk() {
        return Result.OK(mobileService.latestRisk(currentUserId()));
    }

    @PostMapping("/interventions/generate")
    @Operation(summary = "Generate conservative intervention through model-service")
    public Result<InterventionGenerateResponseDto> generateIntervention(@RequestBody InterventionGenerateRequestDto request) {
        try {
            return Result.OK(mobileService.generateIntervention(currentUserId(), request));
        } catch (IllegalStateException e) {
            return Result.error("model-service intervention generation unavailable: " + e.getMessage());
        }
    }

    @GetMapping("/interventions/today")
    @Operation(summary = "Get latest persisted intervention plan when software_db persistence is enabled")
    public Result<InterventionGenerateResponseDto> todayIntervention() {
        return Result.OK(mobileService.latestIntervention(currentUserId()));
    }

    @PostMapping("/interventions/{id}/feedback")
    @Operation(summary = "Record intervention feedback through software_db business repository port")
    public Result<Map<String, Object>> feedback(
            @PathVariable("id") String interventionId,
            @RequestBody FeedbackRequestDto request
    ) {
        return Result.OK(mobileService.submitFeedback(currentUserId(), interventionId, request));
    }

    @PostMapping("/attribution/events")
    @Operation(summary = "Evaluate individual attribution event history through model-service")
    public Result<AttributionResponseDto> attributionEvents(@RequestBody AttributionEventsRequestDto request) {
        try {
            return Result.OK(mobileService.recordAttributionEvents(currentUserId(), request));
        } catch (IllegalStateException e) {
            return Result.error("model-service attribution evaluation unavailable: " + e.getMessage());
        }
    }
}
