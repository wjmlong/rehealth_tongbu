package org.jeecg.modules.rehealth.service;

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

import java.util.Map;

public interface ReHealthMobileService {
    HealthResponseDto health();

    MobileConfigResponseDto config();

    DeviceBindResponseDto bindDevice(String userId, DeviceBindRequestDto request);

    TelemetryBatchResponseDto acceptTelemetryBatch(TelemetryBatchRequestDto request);

    RiskEvaluateResponseDto evaluateFeatures(String userId, RiskEvaluateRequestDto request);

    RiskEvaluateResponseDto latestRisk(String userId);

    InterventionGenerateResponseDto generateIntervention(String userId, InterventionGenerateRequestDto request);

    InterventionGenerateResponseDto latestIntervention(String userId);

    Map<String, Object> submitFeedback(String userId, String interventionId, FeedbackRequestDto request);

    AttributionResponseDto recordAttributionEvents(String userId, AttributionEventsRequestDto request);
}
