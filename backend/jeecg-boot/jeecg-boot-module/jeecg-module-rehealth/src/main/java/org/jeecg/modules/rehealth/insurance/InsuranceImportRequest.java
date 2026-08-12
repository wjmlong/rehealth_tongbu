package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class InsuranceImportRequest {
    private InsuranceImportRequest() {
    }

    public record SubjectBatch(
            String sourceSystem,
            String idempotencyKey,
            List<SubjectRow> records
    ) {
    }

    public record SubjectRow(
            String rehealthUserId,
            String externalSubjectRef,
            String sourceRecordId,
            String enrollmentStatus,
            String consentStatus,
            String consentVersion,
            LocalDateTime consentedAt,
            Map<String, Object> metadata
    ) {
    }

    public record PolicyBatch(
            String sourceSystem,
            String idempotencyKey,
            List<PolicyRow> records
    ) {
    }

    public record PolicyRow(
            String policyNo,
            String productCode,
            String productName,
            String policyType,
            String policyholderSubjectRef,
            String insuredSubjectRef,
            BigDecimal coverageAmount,
            BigDecimal premiumAmount,
            BigDecimal deductibleAmount,
            Integer waitingPeriodDays,
            LocalDate effectiveOn,
            LocalDate expiresOn,
            String status,
            String sourceRecordId,
            Map<String, Object> metadata
    ) {
    }

    public record ClaimBatch(
            String sourceSystem,
            String idempotencyKey,
            List<ClaimRow> records
    ) {
    }

    public record ClaimRow(
            String claimNo,
            String policyNo,
            String subjectRef,
            String claimType,
            LocalDate eventOn,
            LocalDateTime submittedAt,
            LocalDateTime decidedAt,
            String status,
            BigDecimal billedAmount,
            BigDecimal approvedAmount,
            BigDecimal paidAmount,
            String currency,
            String coverageCode,
            String outcomeCode,
            String sourceRecordId,
            Map<String, Object> metadata
    ) {
    }
}
