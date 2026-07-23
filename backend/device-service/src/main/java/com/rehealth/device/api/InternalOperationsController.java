package com.rehealth.device.api;

import com.rehealth.device.application.InternalOperationsService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
@RequestMapping("/rehealth/internal/v1/operations")
public class InternalOperationsController {
    private final InternalOperationsService operationsService;

    public InternalOperationsController(InternalOperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiEnvelope<Map<String, String>>> status(
            @RequestHeader(value = "X-ReHealth-Service-Credential", required = false) String credential
    ) {
        return ResponseEntity.ok(ApiEnvelope.ok(operationsService.status(credential)));
    }
}
