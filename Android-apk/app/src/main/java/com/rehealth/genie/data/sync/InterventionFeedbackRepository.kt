package com.rehealth.genie.data.sync

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * D3 intervention feedback repository.
 *
 * Replaces legacy `submitCheckIn` with typed intervention feedback that:
 * - References specific intervention IDs
 * - Queues feedback locally first
 * - Uploads asynchronously with retry
 * - Pauses on 401 (see [SyncRepository])
 */
class InterventionFeedbackRepository(
    private val dao: InterventionFeedbackDao,
    private val apiClient: AuthenticatedApiClient,
) {

    /**
     * Submit feedback for an intervention. Always succeeds locally, queues for upload.
     */
    suspend fun submitFeedback(
        interventionId: String,
        status: String,
        note: String? = null,
    ): String {
        val feedbackId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val feedback = InterventionFeedbackEntity(
            id = feedbackId,
            interventionId = interventionId,
            status = status,
            note = note,
            checkedAt = now,
            createdAt = now,
            uploadStatus = "pending",
            uploadAttempts = 0,
            nextRetryAt = now,
        )
        dao.insert(feedback)
        return feedbackId
    }

    /**
     * Attempt to upload pending feedback. Returns updated entity or null if 401 detected.
     */
    suspend fun uploadFeedback(feedback: InterventionFeedbackEntity): InterventionFeedbackEntity? {
        val request = InterventionFeedbackRequest(
            status = feedback.status,
            note = feedback.note,
            checkedAt = feedback.checkedAt,
        )

        val result = apiClient.submitInterventionFeedback(feedback.interventionId, request)

        return when (result) {
            is ApiResult.Success -> {
                feedback.copy(uploadStatus = "done", lastError = null)
            }
            is ApiResult.Unauthorized -> {
                null // Queue paused, don't retry
            }
            is ApiResult.Forbidden -> {
                // Intervention doesn't belong to this user, mark as failed
                feedback.copy(uploadStatus = "failed", lastError = "Forbidden: ${result.message}")
            }
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> {
                // Permanent failure
                feedback.copy(uploadStatus = "failed", lastError = result.toString())
            }
            is ApiResult.NetworkError,
            is ApiResult.ServiceUnavailable -> {
                // Transient failure, retry with backoff
                feedback.nextBackoff(error = result.toString())
            }
        }
    }

    suspend fun getPendingUploads(): List<InterventionFeedbackEntity> = dao.pendingUploads()

    suspend fun saveFeedback(feedback: InterventionFeedbackEntity) = dao.update(feedback)

    fun observePendingFeedback(): Flow<List<InterventionFeedbackEntity>> = dao.observePendingFeedback()

    suspend fun getLatestForIntervention(interventionId: String): InterventionFeedbackEntity? =
        dao.getLatestForIntervention(interventionId)

    suspend fun pruneDone() = dao.pruneDone(System.currentTimeMillis() - 7 * 86_400_000L)

    suspend fun countPending(): Int = dao.countPending()

    private fun InterventionFeedbackEntity.nextBackoff(error: String?): InterventionFeedbackEntity {
        val delayMs = (30_000L * (1 shl uploadAttempts.coerceAtMost(6)))
        return copy(
            uploadStatus = "failed",
            uploadAttempts = uploadAttempts + 1,
            lastError = error,
            nextRetryAt = System.currentTimeMillis() + delayMs,
        )
    }
}
