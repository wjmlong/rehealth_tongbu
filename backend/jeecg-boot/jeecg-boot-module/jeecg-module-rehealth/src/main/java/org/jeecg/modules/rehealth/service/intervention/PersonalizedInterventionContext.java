package org.jeecg.modules.rehealth.service.intervention;

import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;

public record PersonalizedInterventionContext(
        String contextVersion,
        String tenantId,
        String userId,
        PatientProfileDto profile,
        HealthInterviewSubmitRequestDto latestInterview,
        RiskEvaluateResponseDto latestRisk,
        DeviceInterventionContext telemetry
) {
}
