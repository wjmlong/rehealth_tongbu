package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AttributionResponseDto {
    public String status;
    @JSONField(name = "history_days") @JsonProperty("history_days") @JsonAlias("historyDays") public Integer historyDays;
    @JSONField(name = "min_history_days") @JsonProperty("min_history_days") @JsonAlias("minHistoryDays") public Integer minHistoryDays;
    @JSONField(name = "intervention_days") @JsonProperty("intervention_days") @JsonAlias("interventionDays") public Integer interventionDays;
    @JSONField(name = "intervention_data_sufficient") @JsonProperty("intervention_data_sufficient") @JsonAlias("interventionDataSufficient") public Boolean interventionDataSufficient;
    @JSONField(name = "current_state") @JsonProperty("current_state") @JsonAlias("currentState") public CurrentStateDto currentState;
    public ForecastDto forecast;
    @JSONField(name = "intervention_effect") @JsonProperty("intervention_effect") @JsonAlias("interventionEffect") public InterventionEffectDto interventionEffect;
    public ReportsDto reports;
    @JSONField(name = "model_version") @JsonProperty("model_version") @JsonAlias("modelVersion") public String modelVersion;
    @JSONField(name = "attribution_mode") @JsonProperty("attribution_mode") @JsonAlias("attributionMode") public String attributionMode;
    @JSONField(name = "is_mock") @JsonProperty("is_mock") @JsonAlias("isMock") public Boolean isMock;
    public String provider;
    @JSONField(name = "request_id") @JsonProperty("request_id") @JsonAlias("requestId") public String requestId;
    @JSONField(name = "error_code") @JsonProperty("error_code") @JsonAlias("errorCode") public String errorCode;
    @JSONField(name = "error_message") @JsonProperty("error_message") @JsonAlias("errorMessage") public String errorMessage;
    public Boolean retryable;
    @JSONField(name = "trend_delta") @JsonProperty("trend_delta") @JsonAlias("trendDelta") public Double trendDelta;
    @JSONField(name = "adherence_average") @JsonProperty("adherence_average") @JsonAlias("adherenceAverage") public Double adherenceAverage;
    public String interpretation;

    public static class CurrentStateDto {
        @JSONField(name = "risk_score") @JsonProperty("risk_score") @JsonAlias("riskScore") public Double riskScore;
        @JSONField(name = "risk_level") @JsonProperty("risk_level") @JsonAlias("riskLevel") public String riskLevel;
        public String trend;
    }
    public static class ForecastDto {
        public ForecastRawDto raw;
        public ForecastSummaryDto summary;
    }
    public static class ForecastRawDto {
        public List<String> dates;
        @JSONField(name = "no_action") @JsonProperty("no_action") @JsonAlias("noAction") public List<Double> noAction;
        @JSONField(name = "with_plan") @JsonProperty("with_plan") @JsonAlias("withPlan") public List<Double> withPlan;
        @JSONField(name = "ci_upper") @JsonProperty("ci_upper") @JsonAlias("ciUpper") public List<Double> ciUpper;
        @JSONField(name = "ci_lower") @JsonProperty("ci_lower") @JsonAlias("ciLower") public List<Double> ciLower;
    }
    public static class ForecastSummaryDto {
        @JSONField(name = "30d_no_action") @JsonProperty("30d_no_action") @JsonAlias("30dNoAction") public Double d30NoAction;
        @JSONField(name = "30d_with_plan") @JsonProperty("30d_with_plan") @JsonAlias("30dWithPlan") public Double d30WithPlan;
        @JSONField(name = "risk_reduction") @JsonProperty("risk_reduction") @JsonAlias("riskReduction") public Double riskReduction;
    }
    public static class InterventionEffectDto {
        @JSONField(name = "individual_att") @JsonProperty("individual_att") @JsonAlias("individualAtt") public Double individualAtt;
        @JSONField(name = "att_ci_lower") @JsonProperty("att_ci_lower") @JsonAlias("attCiLower") public Double attCiLower;
        @JSONField(name = "att_ci_upper") @JsonProperty("att_ci_upper") @JsonAlias("attCiUpper") public Double attCiUpper;
        @JSONField(name = "att_p_value") @JsonProperty("att_p_value") @JsonAlias("attPValue") public Double attPValue;
        @JSONField(name = "att_significant") @JsonProperty("att_significant") @JsonAlias("attSignificant") public Boolean attSignificant;
        @JSONField(name = "att_available") @JsonProperty("att_available") @JsonAlias("attAvailable") public Boolean attAvailable;
        @JSONField(name = "att_unavailable_reason") @JsonProperty("att_unavailable_reason") @JsonAlias("attUnavailableReason") public String attUnavailableReason;
        @JSONField(name = "intervention_days") @JsonProperty("intervention_days") @JsonAlias("interventionDays") public Integer interventionDays;
        @JSONField(name = "intervention_data_sufficient") @JsonProperty("intervention_data_sufficient") @JsonAlias("interventionDataSufficient") public Boolean interventionDataSufficient;
    }
    public static class ReportsDto { public UserReportDto user; }
    public static class UserReportDto {
        public String headline;
        public String body;
        public String advice;
    }

    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Integer getHistoryDays() { return historyDays; }
    public void setHistoryDays(Integer value) { historyDays = value; }
    public Integer getMinHistoryDays() { return minHistoryDays; }
    public void setMinHistoryDays(Integer value) { minHistoryDays = value; }
    public Integer getInterventionDays() { return interventionDays; }
    public void setInterventionDays(Integer value) { interventionDays = value; }
    public Boolean getInterventionDataSufficient() { return interventionDataSufficient; }
    public void setInterventionDataSufficient(Boolean value) { interventionDataSufficient = value; }
    public CurrentStateDto getCurrentState() { return currentState; }
    public void setCurrentState(CurrentStateDto value) { currentState = value; }
    public ForecastDto getForecast() { return forecast; }
    public void setForecast(ForecastDto value) { forecast = value; }
    public InterventionEffectDto getInterventionEffect() { return interventionEffect; }
    public void setInterventionEffect(InterventionEffectDto value) { interventionEffect = value; }
    public ReportsDto getReports() { return reports; }
    public void setReports(ReportsDto value) { reports = value; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String value) { modelVersion = value; }
    public String getAttributionMode() { return attributionMode; }
    public void setAttributionMode(String value) { attributionMode = value; }
    public Boolean getIsMock() { return isMock; }
    public void setIsMock(Boolean value) { isMock = value; }
    public String getProvider() { return provider; }
    public void setProvider(String value) { provider = value; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String value) { requestId = value; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String value) { errorCode = value; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String value) { errorMessage = value; }
    public Boolean getRetryable() { return retryable; }
    public void setRetryable(Boolean value) { retryable = value; }
    public Double getTrendDelta() { return trendDelta; }
    public void setTrendDelta(Double value) { trendDelta = value; }
    public Double getAdherenceAverage() { return adherenceAverage; }
    public void setAdherenceAverage(Double value) { adherenceAverage = value; }
    public String getInterpretation() { return interpretation; }
    public void setInterpretation(String value) { interpretation = value; }
}
