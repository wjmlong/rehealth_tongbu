package com.rehealth.genie.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RdiContributionDto(
    @Json(name = "factor_code") val factorCode: String,
    val domain: String,
    val source: String,
    @Json(name = "current_value") val currentValue: Double,
    @Json(name = "baseline_value") val baselineValue: Double?,
    val unit: String,
    @Json(name = "raw_points") val rawPoints: Double,
    val confidence: Double,
    @Json(name = "final_points") val finalPoints: Double,
    @Json(name = "source_factor_id") val sourceFactorId: String,
)

@JsonClass(generateAdapter = true)
data class RdiDailyIndexDto(
    @Json(name = "scored_on") val scoredOn: String,
    @Json(name = "raw_score") val rawScore: Double,
    @Json(name = "display_score") val displayScore: Double,
    @Json(name = "data_confidence") val dataConfidence: Double,
    val status: String,
    @Json(name = "is_mock") val isMock: Boolean,
    @Json(name = "algorithm_version") val algorithmVersion: String,
    @Json(name = "calculation_source") val calculationSource: String,
    val contributions: List<RdiContributionDto>,
)

@JsonClass(generateAdapter = true)
data class RdiDailySnapshotBatchDto(
    val userId: String,
    val snapshots: List<RdiDailyIndexDto>,
)

@JsonClass(generateAdapter = true)
data class RdiDailySnapshotResponseDto(
    val accepted: Boolean = false,
    val persisted: Boolean = false,
    val status: String? = null,
)
