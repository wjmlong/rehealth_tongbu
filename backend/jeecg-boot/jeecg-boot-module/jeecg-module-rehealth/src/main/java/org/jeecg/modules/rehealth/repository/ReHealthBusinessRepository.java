package org.jeecg.modules.rehealth.repository;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;

import java.util.Optional;

public interface ReHealthBusinessRepository {
    DeviceBindResponseDto recordDeviceBinding(String userId, DeviceBindRequestDto request);

    void saveRiskResult(String userId, String requestId, RiskEvaluateRequestDto request, RiskEvaluateResponseDto response);

    Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId);

    void saveInterventionPlan(String userId, InterventionGenerateResponseDto response);

    Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId);

    void saveFeedback(String userId, String interventionId, FeedbackRequestDto request);

    void recordAttributionResult(String userId, AttributionEventsRequestDto request, AttributionResponseDto response);
}
