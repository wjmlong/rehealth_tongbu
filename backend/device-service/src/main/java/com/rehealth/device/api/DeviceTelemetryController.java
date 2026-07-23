package com.rehealth.device.api;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.device.application.DeviceTelemetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "ReHealth Device Telemetry")
@RestController
@RequestMapping("/rehealth/mobile/measurements")
public class DeviceTelemetryController {
    private final DeviceTelemetryService telemetryService;

    public DeviceTelemetryController(DeviceTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @PostMapping("/batch")
    @Operation(summary = "Accept an authenticated wearable telemetry batch")
    public ResponseEntity<ApiEnvelope<TelemetryBatchResponse>> upload(
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestBody(required = false) TelemetryBatchRequest request
    ) {
        return ResponseEntity.ok(ApiEnvelope.ok(telemetryService.accept(accessToken, tenantId, request)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Read recent telemetry scoped to the authenticated owner and device")
    public ResponseEntity<ApiEnvelope<RecentTelemetryResponse>> recent(
            @RequestHeader(value = "X-Access-Token", required = false) String accessToken,
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(value = "X-ReHealth-Device-Id", required = false) String deviceId,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        return ResponseEntity.ok(ApiEnvelope.ok(telemetryService.recent(accessToken, tenantId, deviceId, limit)));
    }
}
