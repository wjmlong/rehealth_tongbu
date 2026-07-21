package org.jeecg.modules.rehealth.model;

import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;

public interface ModelServiceClient {
    boolean isConfigured();

    ModelHealthResponseDto health();

    RiskEvaluateResponseDto evaluateRisk(RiskEvaluateRequestDto request);

    InterventionGenerateResponseDto generateIntervention(InterventionGenerateRequestDto request);

    AttributionResponseDto evaluateAttribution(AttributionEventsRequestDto request);
}
