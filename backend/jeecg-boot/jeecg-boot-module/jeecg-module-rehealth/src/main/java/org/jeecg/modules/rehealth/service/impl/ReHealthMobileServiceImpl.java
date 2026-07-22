package org.jeecg.modules.rehealth.service.impl;

import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.MobileConfigResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.model.ModelServiceClient;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.service.ReHealthMobileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReHealthMobileServiceImpl implements ReHealthMobileService {
    private final ModelServiceClient modelServiceClient;
    private final HardwareIngestionPort hardwareIngestionPort;
    private final ReHealthBusinessRepository businessRepository;
    private final ReHealthIngestProperties ingestProperties;
    private final boolean softwareDbEnabled;

    public ReHealthMobileServiceImpl(
            ModelServiceClient modelServiceClient,
            HardwareIngestionPort hardwareIngestionPort,
            ReHealthBusinessRepository businessRepository,
            ReHealthIngestProperties ingestProperties,
            @Value("${rehealth.software-db.enabled:false}") boolean softwareDbEnabled
    ) {
        this.modelServiceClient = modelServiceClient;
        this.hardwareIngestionPort = hardwareIngestionPort;
        this.businessRepository = businessRepository;
        this.ingestProperties = ingestProperties;
        this.softwareDbEnabled = softwareDbEnabled;
    }

    @Override
    public HealthResponseDto health() {
        HealthResponseDto response = new HealthResponseDto();
        response.status = "ok";
        response.service = "rehealth-mobile-api";
        response.module = "jeecg-module-rehealth";
        response.modelServiceConfigured = modelServiceClient.isConfigured();
        response.dependencies.put("software_db", softwareDbEnabled ? "enabled" : "disabled_pending_repository");
        response.dependencies.put("hardware_ingest", "e2_1_durable_direct_write");
        response.dependencies.put("hardware_db", ingestProperties.isHardwareDbEnabled() ? "enabled" : "disabled_returns_503");
        response.dependencies.put("raw_signal_upload", ingestProperties.isRawSignalUploadEnabled() ? "enabled" : "disabled_default");
        response.dependencies.put("ingest_queue", ingestProperties.getQueueType());
        response.dependencies.put("model_service", modelServiceClient.isConfigured() ? "configured" : "base_url_missing");
        return response;
    }

    @Override
    public MobileConfigResponseDto config() {
        MobileConfigResponseDto response = new MobileConfigResponseDto();
        response.apiVersion = "e2.1";
        response.modelContract = "model-service /v1/cvd risk, intervention, attribution";
        response.softwareDbPersistenceEnabled = softwareDbEnabled;
        response.hardwareIngestEnabled = true;
        response.ingestMode = ingestProperties.getIngestMode();
        response.ingestQueueType = ingestProperties.getQueueType();
        response.hardwareDbEnabled = ingestProperties.isHardwareDbEnabled();
        response.rawSignalUploadEnabled = ingestProperties.isRawSignalUploadEnabled();
        response.endpoints = List.of(
                "GET /rehealth/mobile/health",
                "GET /rehealth/mobile/config",
                "POST /rehealth/mobile/devices/bind",
                "POST /rehealth/mobile/measurements/batch",
                "POST /rehealth/mobile/features/evaluate",
                "GET /rehealth/mobile/risk/latest",
                "POST /rehealth/mobile/interventions/generate",
                "GET /rehealth/mobile/interventions/today",
                "POST /rehealth/mobile/interventions/{id}/feedback",
                "POST /rehealth/mobile/attribution/events"
        );
        response.limitations = List.of(
                softwareDbEnabled
                        ? "software_db schema must be provisioned before enabling persistence"
                        : "software_db persistence is disabled until schema and datasource are configured",
                "hardware telemetry requires the separate named hardware datasource",
                "MQ/stream deployment and pressure testing remain production follow-up",
                "raw signal upload is disabled by default",
                "risk/intervention/attribution authority is Python model-service",
                "group attribution and settlement evidence are not patient mobile APIs"
        );
        return response;
    }

    @Override
    public DeviceBindResponseDto bindDevice(String userId, DeviceBindRequestDto request) {
        return businessRepository.recordDeviceBinding(userId, request);
    }

    @Override
    public TelemetryBatchResponseDto acceptTelemetryBatch(TelemetryBatchRequestDto request) {
        return hardwareIngestionPort.acceptBatch(request);
    }

    @Override
    public RiskEvaluateResponseDto evaluateFeatures(String userId, RiskEvaluateRequestDto request) {
        RiskEvaluateResponseDto response = modelServiceClient.evaluateRisk(request);
        businessRepository.saveRiskResult(userId, request == null ? null : request.requestId, request, response);
        return response;
    }

    @Override
    public RiskEvaluateResponseDto latestRisk(String userId) {
        return businessRepository.findLatestRiskResult(userId).orElse(null);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(String userId, InterventionGenerateRequestDto request) {
        InterventionGenerateResponseDto response = modelServiceClient.generateIntervention(request);
        businessRepository.saveInterventionPlan(userId, response);
        return response;
    }

    @Override
    public InterventionGenerateResponseDto latestIntervention(String userId) {
        return businessRepository.findLatestInterventionPlan(userId).orElse(null);
    }

    @Override
    public Map<String, Object> submitFeedback(String userId, String interventionId, FeedbackRequestDto request) {
        businessRepository.saveFeedback(userId, interventionId, request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("interventionId", interventionId);
        response.put("status", softwareDbEnabled ? "SOFTWARE_DB_COMMITTED" : "SOFTWARE_DB_DISABLED");
        response.put("persisted", softwareDbEnabled);
        return response;
    }

    @Override
    public AttributionResponseDto recordAttributionEvents(String userId, AttributionEventsRequestDto request) {
        AttributionResponseDto response = modelServiceClient.evaluateAttribution(request);
        businessRepository.recordAttributionResult(userId, request, response);
        return response;
    }
}
