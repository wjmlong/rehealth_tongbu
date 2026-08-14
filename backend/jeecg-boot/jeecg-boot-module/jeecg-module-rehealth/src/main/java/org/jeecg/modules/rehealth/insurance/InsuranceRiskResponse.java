package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public final class InsuranceRiskResponse {
    private InsuranceRiskResponse() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record UnavailableMetric(String status, Object value) {
        public static UnavailableMetric notConnected() {
            return new UnavailableMetric("not_connected", null);
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record RiskDistribution(long high, long medium, long low) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record BusinessSummary(
            @JsonProperty("active_policies") long activePolicies,
            @JsonProperty("active_coverages") long activeCoverages,
            @JsonProperty("claim_count") long claimCount,
            @JsonProperty("billed_amount") BigDecimal billedAmount,
            @JsonProperty("paid_amount") BigDecimal paidAmount,
            @JsonProperty("active_interventions") long activeInterventions,
            @JsonProperty("latest_updated_at") String latestUpdatedAt
    ) {
        public static BusinessSummary unavailable() {
            return new BusinessSummary(0, 0, 0, null, null, 0, null);
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Dashboard(
            @JsonProperty("scope_mode") String scopeMode,
            @JsonProperty("total_insured") long totalInsured,
            @JsonProperty("assessed_insured") long assessedInsured,
            @JsonProperty("synthetic_insured") long syntheticInsured,
            @JsonProperty("unassessed_insured") long unassessedInsured,
            @JsonProperty("risk_distribution") RiskDistribution riskDistribution,
            @JsonProperty("latest_evaluated_at") String latestEvaluatedAt,
            UnavailableMetric claims,
            UnavailableMetric savings,
            UnavailableMetric psm,
            UnavailableMetric rwe,
            @JsonProperty("business_summary") BusinessSummary businessSummary
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record PositiveFactor(String key, double contribution) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Risk(
            String status,
            Double score,
            String level,
            @JsonProperty("model_version") String modelVersion,
            @JsonProperty("evaluated_at") String evaluatedAt,
            @JsonProperty("positive_factors") List<PositiveFactor> positiveFactors
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Intervention(
            String status,
            String summary,
            @JsonProperty("generated_at") String generatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record SubjectBusiness(
            @JsonProperty("active_policies") long activePolicies,
            @JsonProperty("active_coverages") long activeCoverages,
            @JsonProperty("claim_count") long claimCount,
            @JsonProperty("billed_amount") BigDecimal billedAmount,
            @JsonProperty("paid_amount") BigDecimal paidAmount,
            @JsonProperty("consent_status") String consentStatus,
            @JsonProperty("intervention_status") String interventionStatus,
            @JsonProperty("latest_updated_at") String latestUpdatedAt
    ) {
        public static SubjectBusiness unavailable() {
            return new SubjectBusiness(0, 0, 0, null, null, "unknown", "unknown", null);
        }
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Subject(
            @JsonProperty("subject_id") String subjectId,
            @JsonProperty("display_name") String displayName,
            Integer age,
            String gender,
            BigDecimal bmi,
            @JsonProperty("product_name") String productName,
            @JsonProperty("channel_name") String channelName,
            Risk risk,
            Intervention intervention,
            SubjectBusiness business
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record InsuredPage(
            @JsonProperty("scope_mode") String scopeMode,
            @JsonProperty("page_no") int pageNo,
            @JsonProperty("page_size") int pageSize,
            long total,
            List<Subject> records
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record InsuredDetail(
            @JsonProperty("scope_mode") String scopeMode,
            Subject subject
    ) {
    }
}
