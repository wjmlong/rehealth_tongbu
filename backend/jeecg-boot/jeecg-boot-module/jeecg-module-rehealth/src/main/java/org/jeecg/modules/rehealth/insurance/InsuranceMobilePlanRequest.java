package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class InsuranceMobilePlanRequest {
    private InsuranceMobilePlanRequest() {
    }

    public record Bind(
            String tenantId,
            String policyNo,
            String planId,
            String consentVersion,
            String consentType,
            String evidenceRef,
            String evidenceHash,
            String sourceRecordId,
            Map<String, Object> metadata
    ) {
    }

    public record Feedback(
            String feedbackType,
            LocalDateTime occurredAt,
            BigDecimal completionRate,
            BigDecimal adherenceScore,
            String sourceRecordId,
            String interventionId,
            String planItemId,
            BigDecimal expectedCount,
            BigDecimal completedCount,
            String verificationType,
            Map<String, Object> outcomeSummary
    ) {
    }

    public record OccurrenceFeedback(
            String feedbackType,
            LocalDateTime occurredAt,
            String sourceRecordId,
            String verificationType,
            String note
    ) {
    }
}
