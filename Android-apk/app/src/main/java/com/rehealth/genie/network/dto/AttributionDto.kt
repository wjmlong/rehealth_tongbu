package com.rehealth.genie.network.dto

import com.squareup.moshi.JsonClass

/**
 * DTOs for the authenticated ReHealth mobile attribution endpoint.
 *
 * Android sends local Room history to `POST /rehealth/mobile/attribution/events`.
 * JeecgBoot authenticates the user and proxies this PIAS-shaped payload server-side.
 */

/** One day of risk history. Y = risk score (0..1), Z = 1 if intervention day. */
@JsonClass(generateAdapter = true)
data class AttributionHistoryPointDto(
    val date: String,
    val Y: Double,
    val Z: Int,
)

@JsonClass(generateAdapter = true)
data class IndividualAttributionRequestDto(
    val risk_history: List<AttributionHistoryPointDto>,
    val forecast_days: Int = 30,
    val language: String = "zh",
)

// ---- Response (inside ApiResult.result) ----

@JsonClass(generateAdapter = true)
data class AttributionCurrentStateDto(
    val risk_score: Double? = null,
    val risk_level: String? = null,
    val trend: String? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionForecastRawDto(
    val dates: List<String>? = null,
    val no_action: List<Double>? = null,
    val with_plan: List<Double>? = null,
    val ci_upper: List<Double>? = null,
    val ci_lower: List<Double>? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionForecastSummaryDto(
    val `30d_no_action`: Double? = null,
    val `30d_with_plan`: Double? = null,
    val risk_reduction: Double? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionForecastDto(
    val raw: AttributionForecastRawDto? = null,
    val summary: AttributionForecastSummaryDto? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionInterventionEffectDto(
    val individual_att: Double? = null,
    val att_ci_lower: Double? = null,
    val att_ci_upper: Double? = null,
    val att_p_value: Double? = null,
    val att_significant: Boolean? = null,
    val att_available: Boolean? = null,
    val att_unavailable_reason: String? = null,
    val intervention_days: Int? = null,
    val intervention_data_sufficient: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionUserReportDto(
    val headline: String? = null,
    val body: String? = null,
    val advice: String? = null,
)

@JsonClass(generateAdapter = true)
data class AttributionReportsDto(
    val user: AttributionUserReportDto? = null,
)

@JsonClass(generateAdapter = true)
data class IndividualAttributionResponseDto(
    val status: String? = null,
    val history_days: Int? = null,
    val min_history_days: Int? = null,
    val intervention_days: Int? = null,
    val intervention_data_sufficient: Boolean? = null,
    val current_state: AttributionCurrentStateDto? = null,
    val forecast: AttributionForecastDto? = null,
    val intervention_effect: AttributionInterventionEffectDto? = null,
    val reports: AttributionReportsDto? = null,
)
