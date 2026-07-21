package org.jeecg.modules.rehealth.repository;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;

import java.util.Optional;

public interface ReHealthBusinessRepository {
    DeviceBindResponseDto recordDeviceBinding(DeviceBindRequestDto request);

    void saveRiskResult(String requestId, RiskEvaluateResponseDto response);

    Optional<RiskEvaluateResponseDto> findLatestRiskResult();

    void saveInterventionPlan(InterventionGenerateResponseDto response);

    Optional<InterventionGenerateResponseDto> findLatestInterventionPlan();

    void saveFeedback(String interventionId, FeedbackRequestDto request);

    void recordAttributionEvents(AttributionEventsRequestDto request);
}
