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

    public ReHealthMobileServiceImpl(
            ModelServiceClient modelServiceClient,
            HardwareIngestionPort hardwareIngestionPort,
            ReHealthBusinessRepository businessRepository,
            ReHealthIngestProperties ingestProperties
    ) {
        this.modelServiceClient = modelServiceClient;
        this.hardwareIngestionPort = hardwareIngestionPort;
        this.businessRepository = businessRepository;
        this.ingestProperties = ingestProperties;
    }

    @Override
    public HealthResponseDto health() {
        HealthResponseDto response = new HealthResponseDto();
        response.status = "ok";
        response.service = "rehealth-mobile-api";
        response.module = "jeecg-module-rehealth";
        response.modelServiceConfigured = modelServiceClient.isConfigured();
        response.dependencies.put("software_db", "interface_ready_e1_persistence_pending");
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
        response.softwareDbPersistenceEnabled = false;
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
                "software_db tables and mappers are not implemented in E1",
                "hardware telemetry requires the separate named hardware datasource",
                "MQ/stream deployment and pressure testing remain production follow-up",
                "raw signal upload is disabled by default",
                "risk/intervention/attribution authority is Python model-service",
                "group attribution and settlement evidence are not patient mobile APIs"
        );
        return response;
    }

    @Override
    public DeviceBindResponseDto bindDevice(DeviceBindRequestDto request) {
        return businessRepository.recordDeviceBinding(request);
    }

    @Override
    public TelemetryBatchResponseDto acceptTelemetryBatch(TelemetryBatchRequestDto request) {
        return hardwareIngestionPort.acceptBatch(request);
    }

    @Override
    public RiskEvaluateResponseDto evaluateFeatures(RiskEvaluateRequestDto request) {
        RiskEvaluateResponseDto response = modelServiceClient.evaluateRisk(request);
        businessRepository.saveRiskResult(request == null ? null : request.requestId, response);
        return response;
    }

    @Override
    public RiskEvaluateResponseDto latestRisk() {
        return businessRepository.findLatestRiskResult().orElse(null);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(InterventionGenerateRequestDto request) {
        InterventionGenerateResponseDto response = modelServiceClient.generateIntervention(request);
        businessRepository.saveInterventionPlan(response);
        return response;
    }

    @Override
    public InterventionGenerateResponseDto latestIntervention() {
        return businessRepository.findLatestInterventionPlan().orElse(null);
    }

    @Override
    public Map<String, Object> submitFeedback(String interventionId, FeedbackRequestDto request) {
        businessRepository.saveFeedback(interventionId, request);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("interventionId", interventionId);
        response.put("status", "SOFTWARE_DB_INTERFACE_READY_E1_PERSISTENCE_PENDING");
        response.put("persisted", false);
        return response;
    }

    @Override
    public AttributionResponseDto recordAttributionEvents(AttributionEventsRequestDto request) {
        businessRepository.recordAttributionEvents(request);
        return modelServiceClient.evaluateAttribution(request);
    }
}
