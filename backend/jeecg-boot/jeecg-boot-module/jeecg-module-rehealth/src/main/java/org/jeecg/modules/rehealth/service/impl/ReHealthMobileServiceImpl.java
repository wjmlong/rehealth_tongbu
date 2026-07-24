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
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.model.ModelServiceException;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.service.ReHealthMobileService;
import org.jeecg.modules.rehealth.service.attribution.AttributionRequestAssembler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ReHealthMobileServiceImpl implements ReHealthMobileService {
    private static final Pattern CORRELATION_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
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
                "POST /rehealth/mobile/attribution/events",
                "POST /rehealth/mobile/agent/messages"
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
        String requestId = correlationId(request == null ? null : request.requestId);
        if (request != null) {
            request.requestId = requestId;
        }
        long startedNanos = System.nanoTime();
        RiskEvaluateResponseDto response;
        try {
            response = modelServiceClient.evaluateRisk(request);
        } catch (RuntimeException failure) {
            recordModelFailure(userId, requestId, "RISK_EVALUATE", startedNanos, failure);
            throw failure;
        }
        businessRepository.saveRiskResult(userId, requestId, request, response);
        businessRepository.recordModelRequest(userId, new ModelCallAudit(
                requestId,
                "RISK_EVALUATE",
                response == null ? null : response.modelVersion,
                "SUCCESS",
                null,
                elapsedMillis(startedNanos)
        ));
        return response;
    }

    @Override
    public RiskEvaluateResponseDto latestRisk(String userId) {
        return businessRepository.findLatestRiskResult(userId).orElse(null);
    }

    @Override
    public InterventionGenerateResponseDto generateIntervention(String userId, InterventionGenerateRequestDto request) {
        String requestId = correlationId(request == null ? null : request.requestId);
        if (request != null) {
            request.requestId = requestId;
        }
        long startedNanos = System.nanoTime();
        InterventionGenerateResponseDto response;
        try {
            response = modelServiceClient.generateIntervention(request);
        } catch (RuntimeException failure) {
            recordModelFailure(userId, requestId, "INTERVENTION_GENERATE", startedNanos, failure);
            throw failure;
        }
        businessRepository.saveInterventionPlan(userId, response);
        businessRepository.recordModelRequest(userId, new ModelCallAudit(
                requestId,
                "INTERVENTION_GENERATE",
                response == null ? null : response.modelVersion,
                "SUCCESS",
                null,
                elapsedMillis(startedNanos)
        ));
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
        long startedNanos = System.nanoTime();
        try {
            AttributionEventsRequestDto authorizedRequest = AttributionRequestAssembler.fromPersistedHistory(
                    userId,
                    request,
                    businessRepository.findAttributionHistory(userId)
            );
            AttributionResponseDto response = modelServiceClient.evaluateAttribution(authorizedRequest);
            businessRepository.recordAttributionResult(userId, authorizedRequest, response);
            businessRepository.recordModelRequest(userId, new ModelCallAudit(
                    authorizedRequest.requestId,
                    "ATTRIBUTION_EVALUATE_" + response.attributionMode.toUpperCase(),
                    response.modelVersion,
                    "error".equals(response.status) ? "ERROR" : "SUCCESS",
                    response.errorCode,
                    elapsedMillis(startedNanos)
            ));
            return response;
        } catch (RuntimeException failure) {
            recordModelFailure(userId, null, "ATTRIBUTION_EVALUATE", startedNanos, failure);
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
            long startedNanos,
            RuntimeException originalFailure
    ) {
        String correlationId = requestId;
        String errorCode = "UNCLASSIFIED";
        if (originalFailure instanceof ModelServiceException modelFailure) {
            correlationId = modelFailure.correlationId() == null
                    ? requestId
                    : modelFailure.correlationId();
            errorCode = modelFailure.code().name();
        }
        try {
            businessRepository.recordModelRequest(userId, new ModelCallAudit(
                    correlationId,
                    operation,
                    null,
                    "FAILED",
                    errorCode,
                    elapsedMillis(startedNanos)
            ));
        } catch (RuntimeException auditFailure) {
            originalFailure.addSuppressed(auditFailure);
        }
    }

    private String correlationId(String candidate) {
        if (candidate != null && CORRELATION_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }
}
