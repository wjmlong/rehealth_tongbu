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
            UnavailableMetric rwe
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
    public record Subject(
            @JsonProperty("subject_id") String subjectId,
            @JsonProperty("display_name") String displayName,
            Integer age,
            String gender,
            BigDecimal bmi,
            Risk risk,
            Intervention intervention
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
