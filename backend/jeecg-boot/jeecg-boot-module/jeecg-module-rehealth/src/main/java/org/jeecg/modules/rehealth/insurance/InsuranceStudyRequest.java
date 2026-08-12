package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class InsuranceStudyRequest {
    private InsuranceStudyRequest() {
    }

    public record CreateStudy(
            String studyNo,
            String title,
            LocalDate periodStart,
            LocalDate periodEnd,
            Map<String, Object> populationRule,
            Map<String, Object> interventionRule,
            Map<String, Object> outcomeRule,
            String modelVersion
    ) {
    }

    public record QueueJob(String snapshotId, String requestId) {
    }

    public record CompleteJob(
            String status,
            BigDecimal attEstimate,
            BigDecimal ciLower,
            BigDecimal ciUpper,
            Integer matchedPairs,
            Map<String, Object> balance,
            Map<String, Object> costBasis,
            String modelVersion,
            Map<String, Object> result
    ) {
    }

    public record Review(String action, String comment, String requestId) {
    }

    public record CreateReport(String reportNo, String title, Map<String, Object> report) {
    }

    public record CreateSettlement(
            String packageNo,
            String reportId,
            String currency,
            BigDecimal estimatedSavings,
            Map<String, Object> evidenceManifest,
            Map<String, Object> packageData
    ) {
    }

    public record SettlementAction(
            String action,
            String comment,
            String requestId,
            BigDecimal approvedAmount
    ) {
    }
}
