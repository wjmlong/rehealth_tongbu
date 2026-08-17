package org.jeecg.modules.rehealth.insurance;

import java.time.LocalDateTime;

public record InsuranceMobilePlanResponse(
        Integer tenantId,
        String bindingId,
        String subjectRef,
        String policyId,
        String policyNo,
        String planId,
        String consentId,
        String consentVersion,
        String status,
        LocalDateTime boundAt
) {
}
