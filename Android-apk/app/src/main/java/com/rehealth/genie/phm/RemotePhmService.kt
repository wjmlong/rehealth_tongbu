package com.rehealth.genie.phm

import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.CvdFeatureVectorDtoMapper
import com.rehealth.genie.network.RemotePhmError
import com.rehealth.genie.network.RemotePhmOutcome
import com.rehealth.genie.network.ReHealthMobileApi
import com.rehealth.genie.network.dto.FeatureEvaluateRequest
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InterventionFeedbackResponse
import com.rehealth.genie.network.dto.InterventionPlanDto
import com.rehealth.genie.network.dto.MobileConfigResponse
import com.rehealth.genie.network.dto.RiskResultDto
import kotlinx.coroutines.delay

/**
 * Outcome of a remote feature-evaluation pass. Carries either the backend [result]
 * plus the locally-produced feature vector, or a typed [error] plus a flag indicating
 * whether the [mockFallback] used local mock data so the UI can be honest about it.
 *
 * Raw feature vector values are intentionally NOT embedded in error/log surfaces.
 */
data class FeatureEvaluationOutcome(
    val result: RiskResultDto?,
    val featureVector: CvdFeatureVector,
    val requestId: String?,
    val usedMockFallback: Boolean,
    val mockFallbackReason: String?,
    val error: RemotePhmError?,
)

/**
 * Remote-capable PHM service. Connects the local C1 feature extractor to the backend
 * E1 `/rehealth/mobile/features/evaluate` endpoint and the risk/intervention retrieval
 * endpoints, while keeping [MockPhmService] available as a local/dev fallback.
 *
 * D1 scope decisions (intentionally deferred to E2 unless noted):
 *  - No durable telemetry upload via `/rehealth/mobile/measurements/batch`.
 *  - No hardware_db/MQ/high-concurrency wearable ingest.
 *  - No raw PPG/RRI or high-frequency signal upload.
 *  - A single lightweight timeout retry on feature evaluation only (not a queue).
 *
 * Medical guidance returned is conservative and must never be displayed as diagnosis
 * or a clinician replacement; the model-service already tags every response with a
 * `medical_disclaimer`.
 */
class RemotePhmService(
    private val api: ReHealthMobileApi?,
    private val mockFallback: MockPhmService = MockPhmService(),
    private val retryDelayMillis: Long = 500L,
    private val maxAttempts: Int = 2,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {

    suspend fun evaluateFeatures(vector: CvdFeatureVector, requestId: String? = null): FeatureEvaluationOutcome {
        if (api == null) {
            return FeatureEvaluationOutcome(
                result = null,
                featureVector = vector,
                requestId = null,
                usedMockFallback = true,
                mockFallbackReason = "Remote PHM API is not configured; using local mock fallback.",
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
                usedMockFallback = true,
                mockFallbackReason = error.message,
                error = error,
            )
        }

        var lastError: RemotePhmError? = null
        repeat(maxAttempts) { attempt ->
            val outcome = api.evaluateFeatures(request)
            when (outcome) {
                is RemotePhmOutcome.Success -> {
                    return FeatureEvaluationOutcome(
                        result = outcome.data,
                        featureVector = vector,
                        requestId = request.requestId,
                        usedMockFallback = false,
                        mockFallbackReason = null,
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
                            usedMockFallback = true,
                            mockFallbackReason = describeFallback(outcome.error),
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
            usedMockFallback = true,
            mockFallbackReason = describeFallback(lastError),
            error = lastError,
        )
    }

    suspend fun getRiskLatest(): RemotePhmOutcome<RiskResultDto?> =
        api?.getRiskLatest() ?: RemotePhmOutcome.Failure(
            RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
        )

    suspend fun getInterventionsToday(): RemotePhmOutcome<InterventionPlanDto?> =
        api?.getInterventionsToday() ?: RemotePhmOutcome.Failure(
            RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
        )

    suspend fun submitInterventionFeedback(
        interventionId: String,
        status: String,
        note: String? = null,
    ): RemotePhmOutcome<InterventionFeedbackResponse> {
        if (api == null) {
            return RemotePhmOutcome.Failure(
                RemotePhmError.BackendUnavailable("Remote PHM API is not configured."),
            )
        }
        val request = InterventionFeedbackRequest(
            status = status,
            note = note,
            checkedAt = nowProvider(),
        )
        return api.submitInterventionFeedback(interventionId, request)
    }

    /** Convenience accessor for the local mock fallback used by UI/dev/demo paths. */
    fun mock(): PhmService = mockFallback

    private fun describeFallback(error: RemotePhmError?): String =
        "Remote feature evaluation unavailable (${error?.eventName ?: "unknown"}); using local mock fallback."
}
