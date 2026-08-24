package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Population-level intervention effect report contract.
 *
 * The JSON shape intentionally mirrors the snake_case structure consumed by the
 * report template at {@code E:\code\RehealthCore_website\templates\rehealth_intervention_report_template.py},
 * so the website BFF can feed this payload straight into the PDF renderer.
 */
public final class InsuranceInterventionReportResponse {
    private InsuranceInterventionReportResponse() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record ReportData(
            @JsonProperty("title") String title,
            @JsonProperty("subtitle") String subtitle,
            @JsonProperty("focus_line") String focusLine,
            @JsonProperty("study_id") String studyId,
            @JsonProperty("generated_date") String generatedDate,
            @JsonProperty("data_status_label") String dataStatusLabel,
            @JsonProperty("total_managed") long totalManaged,
            @JsonProperty("high_risk_waiting") long highRiskWaiting,
            @JsonProperty("active_interventions") long activeInterventions,
            @JsonProperty("improved") long improved,
            @JsonProperty("risk_distribution") Map<String, Map<String, String>> riskDistribution,
            @JsonProperty("movement") Map<String, Map<String, String>> movement,
            @JsonProperty("adherence") List<AdherenceRow> adherence,
            @JsonProperty("high_risk_people") List<HighRiskPerson> highRiskPeople,
            @JsonProperty("suggestions") List<Suggestion> suggestions,
            @JsonProperty("outcomes") List<Outcome> outcomes,
            @JsonProperty("factors") List<Factor> factors
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record AdherenceRow(
            @JsonProperty("name") String name,
            @JsonProperty("count") String count,
            @JsonProperty("share") String share,
            @JsonProperty("meaning") String meaning
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record HighRiskPerson(
            @JsonProperty("id") String id,
            @JsonProperty("priority") String priority,
            @JsonProperty("signal") String signal,
            @JsonProperty("action") String action,
            @JsonProperty("risk") String risk
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Suggestion(
            @JsonProperty("issue") String issue,
            @JsonProperty("action") String action,
            @JsonProperty("completion") String completion
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Outcome(
            @JsonProperty("name") String name,
            @JsonProperty("change") String change,
            @JsonProperty("meaning") String meaning
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Factor(
            @JsonProperty("name") String name,
            @JsonProperty("contribution") String contribution,
            @JsonProperty("meaning") String meaning
    ) {
    }
}
