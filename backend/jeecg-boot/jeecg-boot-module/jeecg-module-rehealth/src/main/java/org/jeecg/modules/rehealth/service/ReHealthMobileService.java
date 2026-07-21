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

    DeviceBindResponseDto bindDevice(DeviceBindRequestDto request);

    TelemetryBatchResponseDto acceptTelemetryBatch(TelemetryBatchRequestDto request);

    RiskEvaluateResponseDto evaluateFeatures(RiskEvaluateRequestDto request);

    RiskEvaluateResponseDto latestRisk();

    InterventionGenerateResponseDto generateIntervention(InterventionGenerateRequestDto request);

    InterventionGenerateResponseDto latestIntervention();

    Map<String, Object> submitFeedback(String interventionId, FeedbackRequestDto request);

    AttributionResponseDto recordAttributionEvents(AttributionEventsRequestDto request);
}
