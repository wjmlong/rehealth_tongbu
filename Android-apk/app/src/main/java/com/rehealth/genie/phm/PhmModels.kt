package com.rehealth.genie.phm

import com.rehealth.genie.ring.RingMetricType

// ---- UI domain types (moved here from MockPhmService) ----

data class LifeState(
    val score: Int,
    val label: String,
    val insight: String,
)

data class Intervention(
    val title: String,
    val reason: String,
    val duration: String,
    val category: String,
)

enum class ModelInputStage {
    READY,
    FEATURE_EXTRACTED,
    LEARNING,
}

data class ModelInputStatus(
    val type: RingMetricType,
    val label: String,
    val feature: String,
    val stage: ModelInputStage,
)

// ---- Algorithm encapsulation: request + result domain models ----

enum class FeatureQualityStatus {
    REAL_DEVICE, USER_REPORTED, CLINICAL_REPORT, DERIVED, MISSING, STALE, LOW_CONFIDENCE
}

data class FeatureQuality(
    val status: String? = null,
    val source: String? = null,
    val observedAt: Long? = null,
    val reason: String? = null,
)

/** App-level 16-dim CVD feature vector. All clinical fields nullable per contract. */
data class CvdFeatureVector(
    val age: Int? = null,
    val gender: Int? = null,
    val bmi: Double? = null,
    val sbp: Double? = null,
    val dbp: Double? = null,
    val fastingGlucose: Double? = null,
    val totalCholesterol: Double? = null,
    val ldl: Double? = null,
    val hdl: Double? = null,
    val triglycerides: Double? = null,
    val exerciseDays: Int? = null,
    val smoking: Int? = null,
    val drinking: Int? = null,
    val diabetesHistory: Int? = null,
    val hypertensionHistory: Int? = null,
    val familyHistory: Int? = null,
    val featureQuality: Map<String, FeatureQuality> = emptyMap(),
)

data class RiskEvaluateRequest(
    val featureVector: CvdFeatureVector,
    val requestId: String? = null,
)

data class InterventionGenerateRequest(
    val featureVector: CvdFeatureVector? = null,
    val patientContext: Map<String, Any> = emptyMap(),
)

data class FeedbackRequest(
    val status: String? = null,
    val adherence: Double? = null,
    val note: String? = null,
    val checkedAt: Long? = null,
)

data class DeviceBindRequest(
    val deviceId: String,
    val deviceName: String? = null,
    val manufacturer: String? = null,
    val model: String? = null,
    val firmwareVersion: String? = null,
    val hardwareAddressHash: String? = null,
)

data class TelemetryBatchRequest(
    val batchId: String,
    val deviceId: String,
    val collectedFrom: Long? = null,
    val collectedTo: Long? = null,
    val source: String? = null,
    val measurements: List<Map<String, Any>> = emptyList(),
    val sleepSessions: List<Map<String, Any>> = emptyList(),
    val activitySessions: List<Map<String, Any>> = emptyList(),
)

data class AttributionEvent(
    val date: String,
    val riskScore: Double? = null,
    val interventionId: String? = null,
    val adherence: Double? = null,
)

data class AttributionEventsRequest(
    val events: List<AttributionEvent> = emptyList(),
    val baselineRiskScore: Double? = null,
)

data class RiskResult(
    val riskScore: Double? = null,
    val riskLevel: String? = null,
    val featureContributions: Map<String, Double> = emptyMap(),
    val modelVersion: String? = null,
    val isMock: Boolean? = null,
    val missingFields: List<String> = emptyList(),
    val qualityWarnings: List<String> = emptyList(),
    val requestId: String? = null,
    val contributionMethod: String? = null,
    val summary: String? = null,
)

data class InterventionPlan(
    val planId: String? = null,
    val generatedAt: String? = null,
    val priorityIntervention: String? = null,
    val rationale: String? = null,
    val expectedImpact: String? = null,
    val contraindications: List<String> = emptyList(),
    val confidence: Double? = null,
    val modelVersion: String? = null,
    val isMock: Boolean? = null,
    val medicalDisclaimer: String? = null,
)

data class FeedbackResult(
    val id: String? = null,
    val status: String? = null,
    val persisted: Boolean? = null,
    val duplicate: Boolean? = null,
)

data class DeviceBindResult(
    val id: String? = null,
    val userId: String? = null,
    val deviceId: String? = null,
)

data class TelemetryBatchResult(
    val batchId: String? = null,
    val receiptId: String? = null,
    val status: String? = null,
    val accepted: Boolean = false,
    val persisted: Boolean = false,
    val recordCount: Int = 0,
)

data class AttributionResult(
    val requestId: String? = null,
)

// ---- PIAS individual attribution (real algorithm call) ----

data class AttributionHistoryPoint(
    val date: String,
    val riskScore: Double,
    val isInterventionDay: Boolean,
)

data class IndividualAttributionResult(
    val status: String? = null,
    val historyDays: Int? = null,
    val minHistoryDays: Int? = null,
    val currentRiskScore: Double? = null,
    val riskLevel: String? = null,
    val trend: String? = null,
    val d30NoAction: Double? = null,
    val d30WithPlan: Double? = null,
    val riskReduction: Double? = null,
    val individualAtt: Double? = null,
    val attCiLower: Double? = null,
    val attCiUpper: Double? = null,
    val attPValue: Double? = null,
    val attSignificant: Boolean? = null,
    val attAvailable: Boolean? = null,
    val attUnavailableReason: String? = null,
    val interventionDays: Int? = null,
    val interventionDataSufficient: Boolean? = null,
    val headline: String? = null,
    val body: String? = null,
    val advice: String? = null,
    val forecastNoAction: List<Double> = emptyList(),
    val forecastWithPlan: List<Double> = emptyList(),
    val forecastCiUpper: List<Double> = emptyList(),
    val forecastCiLower: List<Double> = emptyList(),
)

data class LoginResult(
    val token: String? = null,
    val userId: String? = null,
    val username: String? = null,
)

/** Local descriptor of which ring metrics feed the model. No network needed. */
fun defaultModelInputs(): List<ModelInputStatus> = listOf(
    ModelInputStatus(RingMetricType.HEART_RATE, "心率", "静息心率、日内波动", ModelInputStage.LEARNING),
    ModelInputStatus(RingMetricType.BLOOD_OXYGEN, "血氧", "均值、低值时长", ModelInputStage.FEATURE_EXTRACTED),
    ModelInputStatus(RingMetricType.BLOOD_PRESSURE, "血压", "收缩压、舒张压趋势", ModelInputStage.LEARNING),
    ModelInputStatus(RingMetricType.SLEEP, "睡眠", "时长、阶段、连续性", ModelInputStage.LEARNING),
    ModelInputStatus(RingMetricType.TEMPERATURE, "体温", "个人基线偏差", ModelInputStage.FEATURE_EXTRACTED),
    ModelInputStatus(RingMetricType.STEPS, "步数", "总量、时段分布", ModelInputStage.LEARNING),
)
