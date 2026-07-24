package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class StubReHealthBusinessRepository implements ReHealthBusinessRepository {
    PatientProfileDto profile;
    RiskEvaluateResponseDto risk;
    InterventionGenerateResponseDto intervention;
    final List<String> queriedUsers = new ArrayList<>();
    ModelCallAudit audit;

    @Override
    public Optional<PatientProfileDto> findPatientProfile(String userId) {
        queriedUsers.add(userId);
        return Optional.ofNullable(profile);
    }

    @Override
    public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
        queriedUsers.add(userId);
        return Optional.ofNullable(risk);
    }

    @Override
    public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
        queriedUsers.add(userId);
        return Optional.ofNullable(intervention);
    }

    @Override
    public void recordModelRequest(String userId, ModelCallAudit audit) {
        queriedUsers.add(userId);
        this.audit = audit;
    }

    @Override
    public PatientProfileDto savePatientProfile(String userId, PatientProfileDto profile) {
        return profile;
    }

    @Override
    public HealthInterviewSubmitRequestDto saveHealthInterview(
            String userId,
            HealthInterviewSubmitRequestDto request
    ) {
        return request;
    }

    @Override
    public Optional<HealthInterviewSubmitRequestDto> findLatestHealthInterview(String userId) {
        return Optional.empty();
    }

    @Override
    public DeviceBindResponseDto recordDeviceBinding(String userId, DeviceBindRequestDto request) {
        return null;
    }

    @Override
    public boolean hasActiveDeviceBinding(String userId, String deviceId) {
        return false;
    }

    @Override
    public void saveRiskResult(
            String userId,
            String requestId,
            RiskEvaluateRequestDto request,
            RiskEvaluateResponseDto response
    ) {
    }

    @Override
    public List<AttributionEventsRequestDto.AttributionHistoryPointDto> findAttributionHistory(String userId) {
        return List.of();
    }

    @Override
    public void saveInterventionPlan(String userId, InterventionGenerateResponseDto response) {
    }

    @Override
    public void saveFeedback(String userId, String interventionId, FeedbackRequestDto request) {
    }

    @Override
    public void recordAttributionResult(
            String userId,
            AttributionEventsRequestDto request,
            AttributionResponseDto response
    ) {
    }
}
