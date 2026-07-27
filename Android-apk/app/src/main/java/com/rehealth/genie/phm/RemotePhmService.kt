package com.rehealth.genie.phm

import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.CvdFeatureVectorDtoMapper
import com.rehealth.genie.network.RemotePhmError
import com.rehealth.genie.network.RemotePhmOutcome
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.ReHealthMobileApi
import com.rehealth.genie.network.dto.AttributionHistoryPointDto
import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.IndividualAttributionRequestDto
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InterventionFeedbackResponse
import com.rehealth.genie.network.dto.InterventionPlanDto
import com.rehealth.genie.network.dto.MobileConfigResponse
import com.rehealth.genie.network.dto.RiskResultDto
import kotlinx.coroutines.delay

/**
 * Outcome of a remote feature-evaluation pass. Carries either the backend [result]
 * plus the locally-produced feature vector, or a typed [error]. Remote failures never
 * synthesize a local risk score.
 *
 * Raw feature vector values are intentionally NOT embedded in error/log surfaces.
 */
data class FeatureEvaluationOutcome(
    val result: RiskResultDto?,
    val featureVector: CvdFeatureVector,
    val requestId: String?,
    val failureReason: String?,
    val error: RemotePhmError?,
)

/**
 * Remote-capable PHM service. Connects the local C1 feature extractor to the backend
 * E1 `/rehealth/mobile/features/evaluate` endpoint and the risk/intervention retrieval
 * endpoints. Backend and model-service failures remain explicit unavailable states.
 *
 * Runtime boundaries:
 *  - Durable telemetry upload is owned by RingCloudRepository/SyncRepository, not this class.
 *  - No raw PPG/RRI or high-frequency signal upload.
 *  - A single lightweight timeout retry on feature evaluation only (not a queue).
 *
 * Medical guidance returned is conservative and must never be displayed as diagnosis
 * or a clinician replacement; the model-service already tags every response with a
 * `medical_disclaimer`.
 */
class RemotePhmService(
    private val api: ReHealthMobileApi?,
    private val authenticatedApi: AuthenticatedApiClient? = null,
    private val retryDelayMillis: Long = 500L,
    private val maxAttempts: Int = 2,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun evaluateFeatures(vector: CvdFeatureVector, requestId: String? = null): FeatureEvaluationOutcome {
        if (api == null && authenticatedApi == null) {
            return FeatureEvaluationOutcome(
                result = null,
                featureVector = vector,
                requestId = null,
                failureReason = "Remote PHM API is not configured.",
                error = RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )
        }

        val request: FeatureEvaluateRequest = try {
            CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector, requestId)
        } catch (t: Throwable) {
            val error = RemotePhmError.MissingFeatureFields(
                fields = vector.missingFields,
                message = t.message ?: "Local feature vector mapping failed.",
            )
            return FeatureEvaluationOutcome(
                result = null,
                featureVector = vector,
                requestId = null,
                failureReason = error.message,
                error = error,
            )
        }

        var lastError: RemotePhmError? = null
        repeat(maxAttempts) { attempt ->
            val outcome = authenticatedApi?.evaluateFeatures(request)?.toRemoteOutcome()
                ?: api?.evaluateFeatures(request)
                ?: RemotePhmOutcome.Failure(
                    RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
                )
            when (outcome) {
                is RemotePhmOutcome.Success -> {
                    return FeatureEvaluationOutcome(
                        result = outcome.data,
                        featureVector = vector,
                        requestId = request.requestId,
                        failureReason = null,
                        error = null,
                    )
                }
                is RemotePhmOutcome.Failure -> {
                    lastError = outcome.error
                    if (outcome.error is RemotePhmError.Timeout && attempt < maxAttempts - 1) {
                        delay(retryDelayMillis)
                    } else {
                        return FeatureEvaluationOutcome(
                            result = null,
                            featureVector = vector,
                            requestId = request.requestId,
                            failureReason = describeFailure(outcome.error),
                            error = outcome.error,
                        )
                    }
                }
            }
        }

        return FeatureEvaluationOutcome(
            result = null,
            featureVector = vector,
            requestId = request.requestId,
            failureReason = describeFailure(lastError),
            error = lastError,
        )
    }

    suspend fun getRiskLatest(): RemotePhmOutcome<RiskResultDto?> =
        authenticatedApi?.getRiskLatest()?.toRemoteOutcome()
            ?: api?.getRiskLatest()
            ?: RemotePhmOutcome.Failure(
                RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )

    suspend fun getInterventionsToday(): RemotePhmOutcome<InterventionPlanDto?> =
        authenticatedApi?.getInterventionsToday()?.toRemoteOutcome()
            ?: api?.getInterventionsToday()
            ?: RemotePhmOutcome.Failure(
                RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )

    suspend fun submitInterventionFeedback(
        interventionId: String,
        status: String,
        note: String? = null,
    ): RemotePhmOutcome<InterventionFeedbackResponse> {
        if (api == null && authenticatedApi == null) {
            return RemotePhmOutcome.Failure(
                RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )
        }
        val request = InterventionFeedbackRequest(
            status = status,
            note = note,
            checkedAt = nowProvider(),
        )
        return authenticatedApi?.submitInterventionFeedback(interventionId, request)?.toRemoteOutcome()
            ?: api?.submitInterventionFeedback(interventionId, request)
            ?: RemotePhmOutcome.Failure(
                RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )
    }

    suspend fun attributeIndividual(
        history: List<AttributionHistoryPoint>,
        forecastDays: Int = 30,
        language: String = "zh",
    ): IndividualAttributionResult {
        require(history.isNotEmpty()) { "暂无真实风险历史，完成风险评估后再试。" }
        val client = requireNotNull(authenticatedApi) { "归因服务未配置，请联系管理员。" }
        val request = IndividualAttributionRequestDto(
            risk_history = history.map {
                AttributionHistoryPointDto(it.date, it.riskScore, if (it.isInterventionDay) 1 else 0)
            },
            forecast_days = forecastDays,
            language = language,
        )
        val response = when (val outcome = client.attributeIndividual(request)) {
            is ApiResult.Success -> outcome.data
            is ApiResult.Unauthorized -> throw IllegalStateException("登录已失效，请重新登录后再试。")
            is ApiResult.Forbidden -> throw IllegalStateException("当前账号无权请求归因分析。")
            is ApiResult.InvalidRequest -> throw IllegalStateException(outcome.message)
            is ApiResult.InvalidResponse -> throw IllegalStateException("归因服务返回无效数据，请稍后重试。")
            is ApiResult.ServiceUnavailable -> throw IllegalStateException("归因服务暂时不可用，请稍后重试。")
            is ApiResult.NetworkError -> throw IllegalStateException("网络连接失败，请稍后重试。")
        }
        return IndividualAttributionResult(
            status = response.status,
            historyDays = response.history_days,
            minHistoryDays = response.min_history_days,
            currentRiskScore = response.current_state?.risk_score,
            riskLevel = response.current_state?.risk_level,
            trend = response.current_state?.trend,
            d30NoAction = response.forecast?.summary?.`30d_no_action`,
            d30WithPlan = response.forecast?.summary?.`30d_with_plan`,
            riskReduction = response.forecast?.summary?.risk_reduction,
            individualAtt = response.intervention_effect?.individual_att,
            attCiLower = response.intervention_effect?.att_ci_lower,
            attCiUpper = response.intervention_effect?.att_ci_upper,
            attPValue = response.intervention_effect?.att_p_value,
            attSignificant = response.intervention_effect?.att_significant,
            attAvailable = response.intervention_effect?.att_available,
            attUnavailableReason = response.intervention_effect?.att_unavailable_reason,
            interventionDays = response.intervention_effect?.intervention_days ?: response.intervention_days,
            interventionDataSufficient = response.intervention_effect?.intervention_data_sufficient
                ?: response.intervention_data_sufficient,
            headline = response.reports?.user?.headline,
            body = response.reports?.user?.body,
            advice = response.reports?.user?.advice,
            forecastNoAction = response.forecast?.raw?.no_action.orEmpty(),
            forecastWithPlan = response.forecast?.raw?.with_plan.orEmpty(),
            forecastCiUpper = response.forecast?.raw?.ci_upper.orEmpty(),
            forecastCiLower = response.forecast?.raw?.ci_lower.orEmpty(),
        )
    }

    private fun describeFailure(error: RemotePhmError?): String =
        "Remote feature evaluation unavailable (${error?.eventName ?: "unknown"})."
}

private fun <T> ApiResult<T>.toRemoteOutcome(): RemotePhmOutcome<T> = when (this) {
    is ApiResult.Success -> RemotePhmOutcome.Success(data)
    is ApiResult.Unauthorized -> RemotePhmOutcome.Failure(RemotePhmError.HttpStatusError(401, message))
    is ApiResult.Forbidden -> RemotePhmOutcome.Failure(RemotePhmError.HttpStatusError(403, message))
    is ApiResult.InvalidRequest -> RemotePhmOutcome.Failure(RemotePhmError.InvalidDto(message))
    is ApiResult.InvalidResponse -> RemotePhmOutcome.Failure(RemotePhmError.InvalidDto(message))
    is ApiResult.ServiceUnavailable -> RemotePhmOutcome.Failure(RemotePhmError.ModelServiceUnavailable(message))
    is ApiResult.NetworkError -> RemotePhmOutcome.Failure(RemotePhmError.BackendUnavailable(message))
}
