package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class InsuranceStudyResponse {
    private InsuranceStudyResponse() {
    }

    public record Study(
            String id, String studyNo, String title, LocalDate periodStart, LocalDate periodEnd,
            String methodology, String status, String modelVersion, LocalDateTime createdAt,
            LocalDateTime updatedAt, String latestSnapshotId, String latestJobId,
            String latestJobStatus, String latestResultId, String latestResultStatus
    ) {
    }

    public record Snapshot(
            String id, String studyId, int version, String snapshotHash, String sourceWatermark,
            int cohortTotal, int treatedTotal, int controlTotal, boolean immutable,
            LocalDateTime createdAt, List<Member> members
    ) {
    }

    public record Member(
            String subjectRef, String cohortGroup, BigDecimal baselineRisk, BigDecimal outcomeValue,
            String interventionStatus, Map<String, Object> covariates
    ) {
    }

    public record Job(
            String id, String studyId, String snapshotId, String status, String requestId,
            int attempt, String errorMessage, String resultId, LocalDateTime createdAt,
            LocalDateTime startedAt, LocalDateTime finishedAt
    ) {
    }

    public record Result(
            String id, String studyId, String snapshotId, int version, String status,
            BigDecimal attEstimate, BigDecimal ciLower, BigDecimal ciUpper, Integer matchedPairs,
            Map<String, Object> balance, Map<String, Object> costBasis, String modelVersion,
            Map<String, Object> result, LocalDateTime createdAt
    ) {
    }

    public record Report(
            String id, String reportNo, String studyId, int version, String title, String status,
            String evidenceHash, Map<String, Object> report, LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record Settlement(
            String id, String packageNo, String studyId, String reportId, int version, String status,
            String currency, BigDecimal estimatedSavings, BigDecimal approvedAmount,
            String snapshotHash, String contentHash, Map<String, Object> evidenceManifest,
            Map<String, Object> packageData, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
    }
}
