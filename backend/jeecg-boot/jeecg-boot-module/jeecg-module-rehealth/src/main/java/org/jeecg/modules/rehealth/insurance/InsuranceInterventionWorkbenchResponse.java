package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public final class InsuranceInterventionWorkbenchResponse {
    private InsuranceInterventionWorkbenchResponse() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Dashboard(
            @JsonProperty("scope_mode") String scopeMode,
            long total,
            @JsonProperty("pending_action") long pendingAction,
            @JsonProperty("in_progress") long inProgress,
            @JsonProperty("pending_review") long pendingReview,
            long improved,
            @JsonProperty("average_adherence") BigDecimal averageAdherence,
            @JsonProperty("latest_updated_at") String latestUpdatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubjectSummary(
            @JsonProperty("subject_id") String subjectId,
            @JsonProperty("display_name") String displayName,
            @JsonProperty("workflow_status") String workflowStatus,
            @JsonProperty("risk_score") Double riskScore,
            @JsonProperty("risk_level") String riskLevel,
            @JsonProperty("risk_is_mock") Boolean riskIsMock,
            List<Factor> factors,
            @JsonProperty("rhi_score") Double rhiScore,
            @JsonProperty("rhi_confidence") Double rhiConfidence,
            @JsonProperty("rdi_score") Double rdiScore,
            @JsonProperty("rdi_confidence") Double rdiConfidence,
            @JsonProperty("rdi_status") String rdiStatus,
            @JsonProperty("rdi_is_mock") Boolean rdiIsMock,
            @JsonProperty("rdi_scored_on") String rdiScoredOn,
            @JsonProperty("adherence_score") Double adherenceScore,
            @JsonProperty("owner_name") String ownerName,
            @JsonProperty("department_name") String departmentName,
            @JsonProperty("current_intervention") String currentIntervention,
            @JsonProperty("intervention_due_at") String interventionDueAt,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubjectPage(
            @JsonProperty("scope_mode") String scopeMode,
            @JsonProperty("page_no") int pageNo,
            @JsonProperty("page_size") int pageSize,
            long total,
            List<SubjectSummary> records
    ) {
    }

    public record TrendPoint(String date, Double value, String level, Boolean synthetic) {
    }

    public record Factor(String key, Double contribution, Double value) {
    }

    public record RdiContribution(
            @JsonProperty("factor_code") String factorCode,
            String domain,
            String source,
            @JsonProperty("current_value") Double currentValue,
            @JsonProperty("baseline_value") Double baselineValue,
            String unit,
            @JsonProperty("final_points") Double finalPoints,
            Double confidence
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Plan(
            @JsonProperty("plan_id") String planId,
            String status,
            String summary,
            List<JsonNode> items,
            Boolean synthetic,
            @JsonProperty("generated_at") String generatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Feedback(
            String id,
            @JsonProperty("feedback_type") String feedbackType,
            @JsonProperty("intervention_id") String interventionId,
            @JsonProperty("completion_rate") Double completionRate,
            @JsonProperty("adherence_score") Double adherenceScore,
            @JsonProperty("occurred_at") String occurredAt,
            JsonNode outcome
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Action(
            String id,
            @JsonProperty("action_type") String actionType,
            String title,
            String content,
            @JsonProperty("assignee_user_id") String assigneeUserId,
            @JsonProperty("assignee_name") String assigneeName,
            String status,
            @JsonProperty("due_at") String dueAt,
            @JsonProperty("completed_at") String completedAt,
            JsonNode result,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Attribution(
            String status,
            @JsonProperty("data_sufficient") Boolean dataSufficient,
            @JsonProperty("is_mock") Boolean isMock,
            @JsonProperty("individual_att") Double individualAtt,
            @JsonProperty("trend_delta") Double trendDelta,
            String interpretation,
            @JsonProperty("created_at") String createdAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubjectDetail(
            @JsonProperty("scope_mode") String scopeMode,
            SubjectSummary subject,
            @JsonProperty("risk_trend") List<TrendPoint> riskTrend,
            @JsonProperty("rhi_trend") List<TrendPoint> rhiTrend,
            @JsonProperty("rdi_trend") List<TrendPoint> rdiTrend,
            List<Factor> factors,
            @JsonProperty("rdi_contributions") List<RdiContribution> rdiContributions,
            Plan plan,
            List<Feedback> feedback,
            List<Action> actions,
            Attribution attribution,
            @JsonProperty("evidence_notice") String evidenceNotice
    ) {
    }
}
