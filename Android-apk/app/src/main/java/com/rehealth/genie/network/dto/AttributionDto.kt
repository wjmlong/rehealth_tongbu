package com.rehealth.genie.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTOs for the PIAS individual-attribution endpoint.
 *
 * Endpoint: POST {MODEL_SERVICE_BASE_URL}/attribute/individual
 * (model-service FastAPI, WSL2 :8000; JeecgBoot envelope format)
 *
 * Field names must match the Python pydantic models in
 * rehealth-algorithms/api/routers/pias_jeecg.py exactly.
 */

/** One day of risk history. Y = risk score (0..1), Z = 1 if intervention day. */
data class AttributionHistoryPointDto(
    @SerializedName("date") val date: String,
    @SerializedName("Y") val y: Double,
    @SerializedName("Z") val z: Int,
)

data class IndividualAttributionRequestDto(
    @SerializedName("risk_history") val riskHistory: List<AttributionHistoryPointDto>,
    @SerializedName("forecast_days") val forecastDays: Int = 30,
    @SerializedName("language") val language: String = "zh",
)

// ---- Response (inside ApiResult.result) ----

data class AttributionCurrentStateDto(
    @SerializedName("risk_score") val riskScore: Double? = null,
    @SerializedName("risk_level") val riskLevel: String? = null,
    @SerializedName("trend") val trend: String? = null,
)

data class AttributionForecastRawDto(
    @SerializedName("dates") val dates: List<String>? = null,
    @SerializedName("no_action") val noAction: List<Double>? = null,
    @SerializedName("with_plan") val withPlan: List<Double>? = null,
    @SerializedName("ci_upper") val ciUpper: List<Double>? = null,
    @SerializedName("ci_lower") val ciLower: List<Double>? = null,
)

data class AttributionForecastSummaryDto(
    @SerializedName("30d_no_action") val d30NoAction: Double? = null,
    @SerializedName("30d_with_plan") val d30WithPlan: Double? = null,
    @SerializedName("risk_reduction") val riskReduction: Double? = null,
)

data class AttributionForecastDto(
    @SerializedName("raw") val raw: AttributionForecastRawDto? = null,
    @SerializedName("summary") val summary: AttributionForecastSummaryDto? = null,
)

data class AttributionInterventionEffectDto(
    @SerializedName("individual_att") val individualAtt: Double? = null,
    @SerializedName("att_ci_lower") val attCiLower: Double? = null,
    @SerializedName("att_ci_upper") val attCiUpper: Double? = null,
    @SerializedName("att_p_value") val attPValue: Double? = null,
    @SerializedName("att_significant") val attSignificant: Boolean? = null,
)

data class AttributionUserReportDto(
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("advice") val advice: String? = null,
)

data class AttributionReportsDto(
    @SerializedName("user") val user: AttributionUserReportDto? = null,
)

data class IndividualAttributionResponseDto(
    @SerializedName("current_state") val currentState: AttributionCurrentStateDto? = null,
    @SerializedName("forecast") val forecast: AttributionForecastDto? = null,
    @SerializedName("intervention_effect") val interventionEffect: AttributionInterventionEffectDto? = null,
    @SerializedName("reports") val reports: AttributionReportsDto? = null,
)
