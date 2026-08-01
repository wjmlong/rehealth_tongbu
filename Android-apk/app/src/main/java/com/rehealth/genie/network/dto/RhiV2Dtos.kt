package com.rehealth.genie.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * RHI v2 research-preview contract models.
 *
 * Local RHI remains the offline default. When the user explicitly selects remote
 * recalculation, these DTOs travel through the authenticated JeecgBoot proxy to the
 * model-service `/v2/rhi/evaluate` endpoint. RDI-16 remains a separate clinical-risk
 * contract and is not replaced by this preview route.
 */
@JsonClass(generateAdapter = true)
data class RhiV2FeatureVectorDto(
    val age: Int? = null,
    @Json(name = "biological_sex") val biologicalSex: String? = null,
    @Json(name = "waist_circumference_cm") val waistCircumferenceCm: Double? = null,
    val bmi: Double? = null,
    @Json(name = "sbp_7d_mean") val sbp7dMean: Double? = null,
    @Json(name = "total_cholesterol") val totalCholesterol: Double? = null,
    @Json(name = "hdl_c") val hdlC: Double? = null,
    @Json(name = "ldl_c") val ldlC: Double? = null,
    val triglycerides: Double? = null,
    @Json(name = "glycemia_value") val glycemiaValue: Double? = null,
    val egfr: Double? = null,
    @Json(name = "nicotine_exposure") val nicotineExposure: Int? = null,
    @Json(name = "diabetes_status") val diabetesStatus: Int? = null,
    @Json(name = "antihypertensive_medication") val antihypertensiveMedication: Int? = null,
    @Json(name = "lipid_lowering_medication") val lipidLoweringMedication: Int? = null,
    @Json(name = "premature_cvd_family_history") val prematureCvdFamilyHistory: Int? = null,
    @Json(name = "dbp_7d_mean") val dbp7dMean: Double? = null,
    @Json(name = "resting_hr_14d_median") val restingHr14dMedian: Double? = null,
    @Json(name = "resting_hr_change_28d_pct") val restingHrChange28dPct: Double? = null,
    @Json(name = "nocturnal_hrv_14d_median") val nocturnalHrv14dMedian: Double? = null,
    @Json(name = "hrv_change_28d_pct") val hrvChange28dPct: Double? = null,
    @Json(name = "cardiorespiratory_fitness_score") val cardiorespiratoryFitnessScore: Double? = null,
    @Json(name = "sleep_duration_7d_mean_hours") val sleepDuration7dMeanHours: Double? = null,
    @Json(name = "sleep_regularity_14d_pct") val sleepRegularity14dPct: Double? = null,
    @Json(name = "sleep_efficiency_14d_pct") val sleepEfficiency14dPct: Double? = null,
    @Json(name = "nocturnal_spo2_drop_burden_14d_pct") val nocturnalSpo2DropBurden14dPct: Double? = null,
    @Json(name = "steps_7d_mean") val steps7dMean: Double? = null,
    @Json(name = "mvpa_minutes_7d") val mvpaMinutes7d: Double? = null,
    @Json(name = "sedentary_hours_7d_mean") val sedentaryHours7dMean: Double? = null,
    @Json(name = "active_day_regularity_14d_pct") val activeDayRegularity14dPct: Double? = null,
    @Json(name = "weight_change_28d_pct") val weightChange28dPct: Double? = null,
    @Json(name = "adherence_composite_28d_pct") val adherenceComposite28dPct: Double? = null,
)

object RhiV2FeatureFields {
    val ALL = listOf(
        "age",
        "biological_sex",
        "waist_circumference_cm",
        "bmi",
        "sbp_7d_mean",
        "total_cholesterol",
        "hdl_c",
        "ldl_c",
        "triglycerides",
        "glycemia_value",
        "egfr",
        "nicotine_exposure",
        "diabetes_status",
        "antihypertensive_medication",
        "lipid_lowering_medication",
        "premature_cvd_family_history",
        "dbp_7d_mean",
        "resting_hr_14d_median",
        "resting_hr_change_28d_pct",
        "nocturnal_hrv_14d_median",
        "hrv_change_28d_pct",
        "cardiorespiratory_fitness_score",
        "sleep_duration_7d_mean_hours",
        "sleep_regularity_14d_pct",
        "sleep_efficiency_14d_pct",
        "nocturnal_spo2_drop_burden_14d_pct",
        "steps_7d_mean",
        "mvpa_minutes_7d",
        "sedentary_hours_7d_mean",
        "active_day_regularity_14d_pct",
        "weight_change_28d_pct",
        "adherence_composite_28d_pct",
    )
}

@JsonClass(generateAdapter = true)
data class RhiV2PersonalBaselineDto(
    val median: Double,
    val mad: Double,
    @Json(name = "sample_count") val sampleCount: Int,
    @Json(name = "window_days") val windowDays: Int = 28,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null,
)

@JsonClass(generateAdapter = true)
data class RhiV2HistoryContextDto(
    @Json(name = "available_days") val availableDays: Int = 1,
    @Json(name = "previous_display_score") val previousDisplayScore: Double? = null,
    @Json(name = "display_score_7d_ago") val displayScore7dAgo: Double? = null,
    @Json(name = "display_score_28d_ago") val displayScore28dAgo: Double? = null,
)

@JsonClass(generateAdapter = true)
data class RhiV2ClinicalRiskDto(
    val model: String,
    @Json(name = "risk_10y") val risk10y: Double? = null,
    @Json(name = "risk_level") val riskLevel: String? = null,
    val applicable: Boolean,
    @Json(name = "last_updated_at") val lastUpdatedAt: String? = null,
    @Json(name = "model_version") val modelVersion: String,
    val reason: String? = null,
)

@JsonClass(generateAdapter = true)
data class RhiV2DeviceContextDto(
    val brand: String? = null,
    val model: String? = null,
    @Json(name = "firmware_version") val firmwareVersion: String? = null,
    @Json(name = "algorithm_version") val algorithmVersion: String? = null,
    @Json(name = "measurement_method") val measurementMethod: String? = null,
    @Json(name = "signal_quality") val signalQuality: Double? = null,
    @Json(name = "wear_time_hours") val wearTimeHours: Double? = null,
    @Json(name = "device_change_detected") val deviceChangeDetected: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RhiV2EvaluateRequestDto(
    val featureVector: RhiV2FeatureVectorDto,
    val featureQuality: Map<String, FeatureQualityDto>,
    val productTier: String,
    val glycemiaMetric: String? = null,
    val personalBaselines: Map<String, RhiV2PersonalBaselineDto> = emptyMap(),
    val history: RhiV2HistoryContextDto = RhiV2HistoryContextDto(),
    val clinicalRisk: RhiV2ClinicalRiskDto? = null,
    val deviceContext: RhiV2DeviceContextDto = RhiV2DeviceContextDto(),
    val safetyFlags: List<String> = emptyList(),
    val requestId: String? = null,
)

@JsonClass(generateAdapter = true)
data class RhiV2DynamicHealthIndexDto(
    val score: Double,
    @Json(name = "raw_score") val rawScore: Double,
    @Json(name = "delta_7d") val delta7d: Double? = null,
    @Json(name = "delta_28d") val delta28d: Double? = null,
    val status: String,
    @Json(name = "smoothing_alpha") val smoothingAlpha: Double,
)

@JsonClass(generateAdapter = true)
data class RhiV2DomainScoresDto(
    val hemodynamic: Double? = null,
    @Json(name = "activity_fitness") val activityFitness: Double? = null,
    @Json(name = "sleep_recovery") val sleepRecovery: Double? = null,
    @Json(name = "metabolic_control") val metabolicControl: Double? = null,
    @Json(name = "behavior_adherence") val behaviorAdherence: Double? = null,
)

@JsonClass(generateAdapter = true)
data class RhiV2DataConfidenceDto(
    val score: Double,
    val grade: String,
    @Json(name = "missing_fields") val missingFields: List<String> = emptyList(),
    @Json(name = "stale_fields") val staleFields: List<String> = emptyList(),
    @Json(name = "low_confidence_fields") val lowConfidenceFields: List<String> = emptyList(),
    @Json(name = "device_change_detected") val deviceChangeDetected: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RhiV2DriverDto(
    val feature: String,
    val effect: Double,
    val direction: String,
)

@JsonClass(generateAdapter = true)
data class RhiV2EvaluateResponseDto(
    @Json(name = "schema_version") val schemaVersion: String,
    @Json(name = "algorithm_version") val algorithmVersion: String,
    @Json(name = "algorithm_status") val algorithmStatus: String,
    @Json(name = "product_tier") val productTier: String,
    @Json(name = "clinical_risk") val clinicalRisk: RhiV2ClinicalRiskDto,
    @Json(name = "dynamic_health_index") val dynamicHealthIndex: RhiV2DynamicHealthIndexDto,
    val domains: RhiV2DomainScoresDto,
    @Json(name = "data_confidence") val dataConfidence: RhiV2DataConfidenceDto,
    @Json(name = "top_drivers") val topDrivers: List<RhiV2DriverDto> = emptyList(),
    @Json(name = "safety_flags") val safetyFlags: List<String> = emptyList(),
    @Json(name = "request_id") val requestId: String? = null,
) {
    val isProductionEligible: Boolean
        get() = algorithmStatus == "validated_production"
}

@JsonClass(generateAdapter = true)
data class RhiDailyDomainScoreDto(
    val domain: String,
    val score: Double? = null,
    val weight: Double,
)

@JsonClass(generateAdapter = true)
data class RhiDailyFeatureSnapshotDto(
    val feature: String,
    val value: Double,
    val confidence: Double,
    @Json(name = "baseline_median") val baselineMedian: Double? = null,
    @Json(name = "baseline_mad") val baselineMad: Double? = null,
    @Json(name = "baseline_sample_count") val baselineSampleCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class RhiDailyQualitySnapshotDto(
    @Json(name = "confidence_score") val confidenceScore: Double,
    @Json(name = "confidence_grade") val confidenceGrade: String,
    @Json(name = "missing_fields") val missingFields: List<String> = emptyList(),
    @Json(name = "low_confidence_fields") val lowConfidenceFields: List<String> = emptyList(),
    @Json(name = "warning_codes") val warningCodes: List<String> = emptyList(),
    @Json(name = "warning_messages") val warningMessages: List<String> = emptyList(),
    @Json(name = "device_change_detected") val deviceChangeDetected: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RhiDailyIndexDto(
    @Json(name = "scored_on") val scoredOn: String,
    @Json(name = "raw_score") val rawScore: Double,
    @Json(name = "display_score") val displayScore: Double,
    @Json(name = "data_confidence") val dataConfidence: Double,
    val status: String,
    @Json(name = "product_tier") val productTier: String,
    @Json(name = "available_days") val availableDays: Int,
    @Json(name = "available_feature_count") val availableFeatureCount: Int,
    @Json(name = "smoothing_alpha") val smoothingAlpha: Double,
    @Json(name = "algorithm_version") val algorithmVersion: String,
    @Json(name = "calculation_source") val calculationSource: String,
    val domains: List<RhiDailyDomainScoreDto> = emptyList(),
    val features: List<RhiDailyFeatureSnapshotDto> = emptyList(),
    val quality: RhiDailyQualitySnapshotDto? = null,
)

@JsonClass(generateAdapter = true)
data class RhiDailySnapshotBatchDto(
    val userId: String,
    val snapshots: List<RhiDailyIndexDto>,
)

@JsonClass(generateAdapter = true)
data class RhiDailySnapshotResponseDto(
    val accepted: Boolean = false,
    val persisted: Boolean = false,
    val status: String? = null,
)

data class RhiV2SeriesEvaluateRequestDto(
    val evaluations: List<RhiV2EvaluateRequestDto>,
)

@JsonClass(generateAdapter = true)
data class RhiV2SeriesEvaluateResponseDto(
    val provider: String,
    val route: String,
    val evaluations: List<RhiV2EvaluateResponseDto>,
)
