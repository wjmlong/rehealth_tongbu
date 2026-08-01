package com.rehealth.genie.ui

import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.phm.IndividualAttributionResult
import com.rehealth.genie.rhi.RhiDailyScore
import com.rehealth.genie.rdi.RdiPeriodSummary
import java.time.LocalDate

enum class AttributionPeriod(val days: Long, val selectorLabel: String) {
    DAYS_7(7, "7 天"),
    DAYS_30(30, "30 天"),
    DAYS_90(90, "90 天"),
}

enum class AttributionRefreshPhase {
    IDLE,
    LOADING,
    REFRESHING,
    READY,
    ERROR,
}

data class AttributionRiskEvaluation(
    val riskScore: Double?,
    val riskLevel: String?,
    val contributions: Map<String, Double>,
    val confirmed: Boolean,
    val factorConfirmed: Boolean = confirmed,
    val factorValues: Map<String, String> = emptyMap(),
    val contributionRuleVersion: String? = null,
    val measuredComponents: Map<String, Double> = emptyMap(),
    val controlSupportComponents: Map<String, Double> = emptyMap(),
)

data class AttributionActivityInput(
    val startedAt: Long,
    val activityType: String,
    val steps: Int,
    val durationMinutes: Int,
    val caloriesKcal: Double,
    val distanceMeters: Double,
    val source: String,
    val replay: Boolean,
)

data class AttributionInterventionInput(
    val id: String?,
    val title: String?,
    val action: String?,
    val duration: String?,
    val reason: String?,
    val status: String?,
)

data class AttributionRemoteData(
    val history: List<AttributionHistoryPoint>,
    val pias: IndividualAttributionResult?,
)

data class AttributionUiInput(
    val period: AttributionPeriod,
    val today: LocalDate,
    val evaluation: AttributionRiskEvaluation?,
    val remote: AttributionRemoteData,
    val rdiSummary: RdiPeriodSummary? = null,
    val refreshPhase: AttributionRefreshPhase,
    val refreshError: String? = null,
    val activity: AttributionActivityInput? = null,
    val allowDebugReplay: Boolean = false,
    val factorValues: Map<String, String> = emptyMap(),
    val interventions: List<AttributionInterventionInput> = emptyList(),
    val interventionSourceMode: String? = null,
)

data class AttributionFactorUi(
    val key: String,
    val label: String,
    val section: String,
    val value: String?,
    val contribution: Double?,
    val contributionRuleVersion: String?,
    val measuredComponent: Double?,
    val controlSupportComponent: Double?,
) {
    val contributionMissing: Boolean
        get() = contribution == null
}

data class AttributionFactorGroupUi(
    val title: String,
    val factors: List<AttributionFactorUi>,
)

data class AttributionForecastUi(
    val noAction: List<Double>,
    val withPlan: List<Double>,
    val ciLower: List<Double>,
    val ciUpper: List<Double>,
    val d30NoAction: Double?,
    val d30WithPlan: Double?,
    val riskReduction: Double?,
) {
    val chartAvailable: Boolean
        get() = noAction.size >= 2 && withPlan.size >= 2
}

/**
 * Reserved RDI-16 scenario fields. The current RDI-16 contract only supplies
 * confirmed historical risk scores, so these values remain null until that
 * contract explicitly returns RDI-16-native forecasts.
 */
data class AttributionRdiScenarioUi(
    val noActionScore: Double? = null,
    val withPlanScore: Double? = null,
    val expectedReduction: Double? = null,
    val ciLower: List<Double> = emptyList(),
    val ciUpper: List<Double> = emptyList(),
) {
    val forecastAvailable: Boolean
        get() = noActionScore != null && withPlanScore != null && expectedReduction != null

    val intervalAvailable: Boolean
        get() = ciLower.size >= 2 && ciUpper.size >= 2
}

sealed interface AttributionAttUiState {
    data class Available(
        val value: Double,
        val ciLower: Double?,
        val ciUpper: Double?,
        val pValue: Double?,
        val significant: Boolean?,
    ) : AttributionAttUiState

    data class Unavailable(val reason: String) : AttributionAttUiState
}

sealed interface AttributionPiasUiState {
    data object Empty : AttributionPiasUiState
    data object Loading : AttributionPiasUiState
    data class Failed(val message: String) : AttributionPiasUiState
    data class Accumulating(
        val historyDays: Int,
        val minHistoryDays: Int,
    ) : AttributionPiasUiState {
        val remainingDays: Int
            get() = (minHistoryDays - historyDays).coerceAtLeast(0)
    }

    data class Ready(
        val forecast: AttributionForecastUi,
        val att: AttributionAttUiState,
        val trend: String?,
        val historyDays: Int?,
    ) : AttributionPiasUiState
}

data class AttributionActivityUi(
    val startedAt: Long,
    val activityType: String,
    val steps: Int,
    val durationMinutes: Int,
    val caloriesKcal: Double,
    val distanceMeters: Double,
    val provenanceLabel: String,
)

data class AttributionInterventionUi(
    val id: String,
    val title: String,
    val action: String?,
    val duration: String?,
    val reason: String?,
    val status: String?,
    val feedbackEnabled: Boolean,
)

data class AttributionUiState(
    val period: AttributionPeriod,
    val refreshPhase: AttributionRefreshPhase,
    val refreshMessage: String?,
    val currentRisk: Double?,
    val currentRiskText: String,
    val riskLevel: String?,
    val selectedHistory: List<AttributionHistoryPoint>,
    val rdiScenario: AttributionRdiScenarioUi,
    val pias: AttributionPiasUiState,
    val activity: AttributionActivityUi?,
    val factors: List<AttributionFactorUi>,
    val factorGroups: List<AttributionFactorGroupUi>,
    val interventions: List<AttributionInterventionUi>,
)

data class AttributionRhiImprovementUi(
    val improvementPoints: Double?,
    val improvementText: String,
    val comparisonText: String,
    val selectedHistory: List<RhiDailyScore>,
)

data class AttributionRefreshState(
    val activeRequestId: Long? = null,
    val phase: AttributionRefreshPhase = AttributionRefreshPhase.IDLE,
    val data: AttributionRemoteData? = null,
    val errorMessage: String? = null,
) {
    val canRetry: Boolean
        get() = phase == AttributionRefreshPhase.ERROR

    fun reduce(event: AttributionRefreshEvent): AttributionRefreshState = when (event) {
        is AttributionRefreshEvent.Started -> copy(
            activeRequestId = event.requestId,
            phase = if (data == null) AttributionRefreshPhase.LOADING else AttributionRefreshPhase.REFRESHING,
            errorMessage = null,
        )
        is AttributionRefreshEvent.Succeeded -> if (event.requestId != activeRequestId) {
            this
        } else {
            copy(
                activeRequestId = null,
                phase = AttributionRefreshPhase.READY,
                data = event.data,
                errorMessage = null,
            )
        }
        is AttributionRefreshEvent.Failed -> if (event.requestId != activeRequestId) {
            this
        } else {
            copy(
                activeRequestId = null,
                phase = AttributionRefreshPhase.ERROR,
                data = event.data ?: data,
                errorMessage = event.message,
            )
        }
    }
}

sealed interface AttributionRefreshEvent {
    data class Started(val requestId: Long) : AttributionRefreshEvent
    data class Succeeded(val requestId: Long, val data: AttributionRemoteData) : AttributionRefreshEvent
    data class Failed(
        val requestId: Long,
        val message: String,
        val data: AttributionRemoteData? = null,
    ) : AttributionRefreshEvent
}
