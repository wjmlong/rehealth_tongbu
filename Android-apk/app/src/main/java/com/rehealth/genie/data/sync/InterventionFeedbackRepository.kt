package com.rehealth.genie.data.sync

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.InterventionFeedbackRequest
import com.rehealth.genie.network.dto.InsurancePlanFeedbackRequestDto
import com.rehealth.genie.network.dto.InstitutionCarePlanFeedbackRequestDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    private val userIdProvider: () -> String?,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {

    /**
     * Submit feedback for an intervention. Always succeeds locally, queues for upload.
     */
    suspend fun submitFeedback(
        interventionId: String,
        status: String,
        note: String? = null,
        bindingId: String? = null,
        tenantId: Int? = null,
        planItemId: String? = null,
        occurrenceId: String? = null,
        expectedCount: Double? = null,
        completedCount: Double? = null,
        verificationType: String = "self_report",
    ): String {
        val ownerUserId = userIdProvider()?.takeIf(String::isNotBlank)
            ?: error("登录后才能提交干预反馈")
        val feedbackId = UUID.randomUUID().toString()
        val now = nowProvider()
        if (occurrenceId != null) {
            dao.supersedeDeadLetters(ownerUserId, occurrenceId)
        }
        val feedback = InterventionFeedbackEntity(
            id = feedbackId,
            ownerUserId = ownerUserId,
            interventionId = interventionId,
            bindingId = bindingId,
            tenantId = tenantId,
            planItemId = planItemId,
            occurrenceId = occurrenceId,
            status = status,
            note = note,
            expectedCount = expectedCount,
            completedCount = completedCount,
            verificationType = verificationType,
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
        if (feedback.ownerUserId != userIdProvider()) {
            return feedback.toDeadLetter("feedback_owner_mismatch")
        }
        if (feedback.occurrenceId != null) {
            return uploadInstitutionCarePlanFeedback(feedback)
        }
        if (feedback.bindingId != null && feedback.planItemId != null) {
            return uploadInsuranceFeedback(feedback)
        }
        val request = InterventionFeedbackRequest(
            status = feedback.status,
            note = feedback.note,
            checkedAt = feedback.checkedAt,
        )

        val result = apiClient.submitInterventionFeedback(feedback.interventionId, request)

        return when (result) {
            is ApiResult.Success -> {
                if (result.data.persisted) {
                    feedback.copy(uploadStatus = "done", lastError = null)
                } else {
                    feedback.nextBackoff(error = "feedback_not_persisted")
                }
            }
            is ApiResult.Unauthorized -> {
                null // Queue paused, don't retry
            }
            is ApiResult.Forbidden -> {
                // Intervention doesn't belong to this user, mark as failed
                feedback.toDeadLetter("feedback_forbidden")
            }
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> {
                // Permanent failure
                feedback.toDeadLetter("feedback_invalid")
            }
            is ApiResult.NetworkError,
            is ApiResult.ServiceUnavailable -> {
                // Transient failure, retry with backoff
                feedback.nextBackoff(error = result.toString())
            }
        }
    }

    private suspend fun uploadInsuranceFeedback(
        feedback: InterventionFeedbackEntity,
    ): InterventionFeedbackEntity? {
        val request = InsurancePlanFeedbackRequestDto(
            feedbackType = feedback.status,
            occurredAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(feedback.checkedAt)),
            completionRate = null,
            adherenceScore = null,
            sourceRecordId = feedback.id,
            interventionId = feedback.interventionId,
            planItemId = feedback.planItemId ?: return feedback.toDeadLetter("plan_item_id_missing"),
            expectedCount = feedback.expectedCount ?: 1.0,
            completedCount = feedback.completedCount,
            verificationType = feedback.verificationType,
            outcomeSummary = feedback.note?.let { mapOf("note" to it) }.orEmpty(),
        )
        return when (val result = apiClient.submitInsurancePlanFeedback(feedback.bindingId!!, request)) {
            is ApiResult.Success -> feedback.copy(uploadStatus = "done", lastError = null)
            is ApiResult.Unauthorized -> null
            is ApiResult.Forbidden -> feedback.toDeadLetter("insurance_feedback_forbidden")
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> feedback.toDeadLetter("insurance_feedback_invalid")
            is ApiResult.NetworkError,
            is ApiResult.ServiceUnavailable -> feedback.nextBackoff(error = result.toString())
        }
    }

    private suspend fun uploadInstitutionCarePlanFeedback(
        feedback: InterventionFeedbackEntity,
    ): InterventionFeedbackEntity? {
        val occurrenceId = feedback.occurrenceId
            ?: return feedback.toDeadLetter("occurrence_id_missing")
        val request = InstitutionCarePlanFeedbackRequestDto(
            feedbackType = feedback.status,
            occurredAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(feedback.checkedAt)),
            sourceRecordId = feedback.id,
            verificationType = feedback.verificationType,
            note = feedback.note,
        )
        return when (val result = apiClient.submitInstitutionCarePlanFeedback(occurrenceId, request)) {
            is ApiResult.Success -> feedback.copy(uploadStatus = "done", lastError = null)
            is ApiResult.Unauthorized -> null
            is ApiResult.Forbidden -> feedback.toDeadLetter("care_plan_feedback_forbidden")
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> feedback.toDeadLetter("care_plan_feedback_invalid")
            is ApiResult.NetworkError,
            is ApiResult.ServiceUnavailable -> feedback.nextBackoff(error = result.toString())
        }
    }

    suspend fun getPendingUploads(): List<InterventionFeedbackEntity> =
        userIdProvider()?.takeIf(String::isNotBlank)?.let { dao.pendingUploads(it) }.orEmpty()

    suspend fun saveFeedback(feedback: InterventionFeedbackEntity) = dao.update(feedback)

    fun observePendingFeedback(): Flow<List<InterventionFeedbackEntity>> =
        userIdProvider()?.takeIf(String::isNotBlank)?.let(dao::observePendingFeedback) ?: flowOf(emptyList())

    fun observeFeedback(feedbackId: String): Flow<InterventionFeedbackEntity?> =
        userIdProvider()?.takeIf(String::isNotBlank)
            ?.let { dao.observeFeedback(it, feedbackId) }
            ?: flowOf(null)

    suspend fun getLatestForIntervention(interventionId: String): InterventionFeedbackEntity? =
        userIdProvider()?.takeIf(String::isNotBlank)?.let { dao.getLatestForIntervention(it, interventionId) }

    suspend fun pruneDone() = dao.pruneDone(nowProvider() - 7 * 86_400_000L)

    suspend fun countPending(): Int =
        userIdProvider()?.takeIf(String::isNotBlank)?.let { dao.countPending(it) } ?: 0

    private fun InterventionFeedbackEntity.nextBackoff(error: String?): InterventionFeedbackEntity {
        return nextFeedbackRetry(error = error, now = nowProvider())
    }

    private fun InterventionFeedbackEntity.toDeadLetter(error: String): InterventionFeedbackEntity = copy(
        uploadStatus = "dead_letter",
        lastError = error,
    )
}

internal const val MAX_FEEDBACK_UPLOAD_ATTEMPTS = 10

internal fun InterventionFeedbackEntity.nextFeedbackRetry(
    error: String?,
    now: Long,
): InterventionFeedbackEntity {
    val attempts = uploadAttempts + 1
    if (attempts >= MAX_FEEDBACK_UPLOAD_ATTEMPTS) {
        return copy(
            uploadStatus = "dead_letter",
            uploadAttempts = attempts,
            lastError = error,
        )
    }
    val delayMs = 30_000L * (1 shl uploadAttempts.coerceAtMost(6))
    return copy(
        uploadStatus = "retry",
        uploadAttempts = attempts,
        lastError = error,
        nextRetryAt = now + delayMs,
    )
}
