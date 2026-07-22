package org.jeecg.modules.rehealth.service.impl;

import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.ingest.query.HardwareTelemetryQuery;
import org.jeecg.modules.rehealth.config.ReHealthIngestProperties;
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
    private final HardwareTelemetryQuery hardwareTelemetryQuery;
    private final ReHealthBusinessRepository businessRepository;
    private final ReHealthIngestProperties ingestProperties;
    private final boolean softwareDbEnabled;

    public ReHealthMobileServiceImpl(
            ModelServiceClient modelServiceClient,
            HardwareIngestionPort hardwareIngestionPort,
            HardwareTelemetryQuery hardwareTelemetryQuery,
            ReHealthBusinessRepository businessRepository,
            ReHealthIngestProperties ingestProperties,
            @Value("${rehealth.software-db.enabled:false}") boolean softwareDbEnabled
    ) {
        this.modelServiceClient = modelServiceClient;
        this.hardwareIngestionPort = hardwareIngestionPort;
        this.hardwareTelemetryQuery = hardwareTelemetryQuery;
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
                "GET /rehealth/mobile/profile",
                "PUT /rehealth/mobile/profile",
                "POST /rehealth/mobile/interviews",
                "GET /rehealth/mobile/interviews/latest",
                "POST /rehealth/mobile/devices/bind",
                "POST /rehealth/mobile/measurements/batch",
                "GET /rehealth/mobile/measurements/recent",
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
    public PatientProfileDto saveProfile(String userId, PatientProfileDto profile) {
        requireSoftwareDb();
        return businessRepository.savePatientProfile(userId, profile);
    }

    @Override
    public PatientProfileDto profile(String userId) {
        return businessRepository.findPatientProfile(userId).orElse(null);
    }

    @Override
    public HealthInterviewSubmitRequestDto submitInterview(
            String userId,
            HealthInterviewSubmitRequestDto request
    ) {
        requireSoftwareDb();
        return businessRepository.saveHealthInterview(userId, request);
    }

    @Override
    public HealthInterviewSubmitRequestDto latestInterview(String userId) {
        return businessRepository.findLatestHealthInterview(userId).orElse(null);
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
    public RecentTelemetryResponseDto recentTelemetry(String userId, int limit) {
        return hardwareTelemetryQuery.recentForUser(userId, limit);
    }

    @Override
    public RiskEvaluateResponseDto evaluateFeatures(String userId, RiskEvaluateRequestDto request) {
        String requestId = request == null ? null : request.requestId;
        try {
            RiskEvaluateResponseDto response = modelServiceClient.evaluateRisk(request);
            businessRepository.saveRiskResult(userId, requestId, request, response);
            businessRepository.recordModelRequest(
                    userId, requestId, "RISK_EVALUATE",
                    response == null ? null : response.modelVersion, "SUCCESS"
            );
            return response;
        } catch (RuntimeException failure) {
            recordModelFailure(userId, requestId, "RISK_EVALUATE", failure);
            throw failure;
        }
    }

    @Override
    public RiskEvaluateResponseDto latestRisk(String userId) {
        return businessRepository.findLatestRiskResult(userId).orElse(null);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(String userId, InterventionGenerateRequestDto request) {
        try {
            InterventionGenerateResponseDto response = modelServiceClient.generateIntervention(request);
            businessRepository.saveInterventionPlan(userId, response);
            businessRepository.recordModelRequest(
                    userId, response == null ? null : response.planId, "INTERVENTION_GENERATE",
                    response == null ? null : response.modelVersion, "SUCCESS"
            );
            return response;
        } catch (RuntimeException failure) {
            recordModelFailure(userId, null, "INTERVENTION_GENERATE", failure);
            throw failure;
        }
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
        try {
            AttributionResponseDto response = modelServiceClient.evaluateAttribution(request);
            businessRepository.recordAttributionResult(userId, request, response);
            businessRepository.recordModelRequest(
                    userId, null, "ATTRIBUTION_EVALUATE",
                    response == null ? null : response.modelVersion, "SUCCESS"
            );
            return response;
        } catch (RuntimeException failure) {
            recordModelFailure(userId, null, "ATTRIBUTION_EVALUATE", failure);
            throw failure;
        }
    }

    private void requireSoftwareDb() {
        if (!softwareDbEnabled) {
            throw new IllegalStateException("software_db persistence is disabled");
        }
    }

    private void recordModelFailure(
            String userId,
            String requestId,
            String operation,
            RuntimeException originalFailure
    ) {
        try {
            businessRepository.recordModelRequest(userId, requestId, operation, null, "FAILED");
        } catch (RuntimeException auditFailure) {
            originalFailure.addSuppressed(auditFailure);
        }
    }
}
