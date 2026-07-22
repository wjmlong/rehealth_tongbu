package com.rehealth.genie.data.sync

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthState
import com.rehealth.genie.network.MeasurementUploadClient
import com.rehealth.genie.network.HealthInterviewUploadClient
import com.rehealth.genie.network.dto.HealthInterviewSubmitRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * D3 upload queue repository with 401-aware pause/resume.
 *
 * When the authenticated client detects a 401:
 * - Marks queue as [QueueState.Paused]
 * - Stops attempting uploads
 * - Notifies UI via [queueState] Flow
 *
 * After successful re-login:
 * - Call [resumeQueue] to restart uploads
 */
class SyncRepository(
    private val dao: UploadQueueDao,
    private val apiClient: MeasurementUploadClient,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
    private val healthInterviewClient: HealthInterviewUploadClient? = apiClient as? HealthInterviewUploadClient,
) {

    private val _queueState = MutableStateFlow<QueueState>(QueueState.Active)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    suspend fun enqueue(item: UploadQueueEntity) = dao.insert(item)

    suspend fun save(item: UploadQueueEntity) = dao.update(item)

    suspend fun pending(): List<UploadQueueEntity> = dao.pending(nowProvider())

    fun observeOutstanding(): Flow<List<UploadQueueEntity>> = dao.observeOutstanding()

    suspend fun pruneDone() = dao.pruneDone(nowProvider() - 7 * 86_400_000L)

    suspend fun uploadMeasurement(item: UploadQueueEntity): MeasurementUploadOutcome {
        if (item.kind != MEASUREMENT_KIND) return MeasurementUploadOutcome.Skipped
        val request = try {
            gson.fromJson(item.payloadJson, TelemetryBatchRequestDto::class.java)
                ?: return deadLetter(item)
        } catch (_: JsonParseException) {
            return deadLetter(item)
        }
        if (request.batchId.isNullOrBlank() || request.deviceId.isNullOrBlank()) {
            return deadLetter(item)
        }

        return when (val result = apiClient.uploadMeasurements(request)) {
            is ApiResult.Success -> handleDurableSuccess(item, result.data)
            is ApiResult.Unauthorized -> {
                pauseQueue()
                MeasurementUploadOutcome.Paused
            }
            is ApiResult.Forbidden -> saveDeadLetter(item, "measurement_upload_forbidden")
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> saveDeadLetter(item, "measurement_upload_invalid")
            is ApiResult.NetworkError -> saveRetry(item, "measurement_upload_network")
            is ApiResult.ServiceUnavailable -> saveRetry(item, "measurement_upload_service_unavailable")
        }
    }

    suspend fun uploadQueuedItem(item: UploadQueueEntity): MeasurementUploadOutcome = when (item.kind) {
        MEASUREMENT_KIND -> uploadMeasurement(item)
        HEALTH_INTERVIEW_KIND -> uploadHealthInterview(item)
        else -> MeasurementUploadOutcome.Skipped
    }

    private suspend fun uploadHealthInterview(item: UploadQueueEntity): MeasurementUploadOutcome {
        val client = healthInterviewClient ?: return saveRetry(item, "health_interview_client_unavailable")
        val request = try {
            gson.fromJson(item.payloadJson, HealthInterviewSubmitRequestDto::class.java)
                ?: return deadLetter(item)
        } catch (_: JsonParseException) {
            return deadLetter(item)
        }
        if (request.answers.isEmpty()) return deadLetter(item)
        return when (client.submitHealthInterview(request)) {
            is ApiResult.Success -> {
                dao.update(item.copy(status = "done", lastError = null))
                MeasurementUploadOutcome.Uploaded
            }
            is ApiResult.Unauthorized -> {
                pauseQueue()
                MeasurementUploadOutcome.Paused
            }
            is ApiResult.Forbidden -> saveDeadLetter(item, "health_interview_forbidden")
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> saveDeadLetter(item, "health_interview_invalid")
            is ApiResult.NetworkError -> saveRetry(item, "health_interview_network")
            is ApiResult.ServiceUnavailable -> saveRetry(item, "health_interview_service_unavailable")
        }
    }

    /**
     * Check if queue should process items. Returns false if unauthorized or paused.
     */
    fun canUpload(): Boolean {
        return when (_queueState.value) {
            QueueState.Active -> apiClient.authState == AuthState.Authorized
            QueueState.Paused -> false
        }
    }

    /**
     * Pause queue after 401 detection.
     */
    fun pauseQueue() {
        _queueState.value = QueueState.Paused
    }

    /**
     * Resume queue after successful re-login.
     */
    fun resumeQueue() {
        if (apiClient.authState == AuthState.Authorized) {
            _queueState.value = QueueState.Active
        }
    }

    /**
     * Handle API result and return whether item should be retried.
     * Returns null if unauthorized (queue paused), otherwise returns updated item.
     */
    suspend fun <T> handleResult(
        item: UploadQueueEntity,
        result: ApiResult<T>,
    ): UploadQueueEntity? {
        return when (result) {
            is ApiResult.Success -> {
                item.copy(status = "done", lastError = null)
            }
            is ApiResult.Unauthorized -> {
                pauseQueue()
                null // Don't retry, wait for re-login
            }
            is ApiResult.Forbidden -> {
                // Forbidden means the item is invalid (e.g., wrong user), mark as failed
                item.copy(status = "failed", lastError = "Forbidden: ${result.message}")
            }
            is ApiResult.InvalidRequest,
            is ApiResult.InvalidResponse -> {
                // Permanent failure, don't retry
                item.copy(status = "failed", lastError = result.toString())
            }
            is ApiResult.NetworkError,
            is ApiResult.ServiceUnavailable -> {
                // Transient failure, retry with backoff
                item.nextBackoff(error = result.toString())
            }
        }
    }

    /** Exponential backoff: 30s, 60s, 120s ... capped at 30 minutes. */
    private fun UploadQueueEntity.nextBackoff(error: String?): UploadQueueEntity {
        val delayMs = (30_000L * (1 shl attempts.coerceAtMost(6)))
        return copy(
            status = "failed",
            attempts = attempts + 1,
            lastError = error,
            nextRetryAt = nowProvider() + delayMs,
        )
    }

    private suspend fun handleDurableSuccess(
        item: UploadQueueEntity,
        response: TelemetryBatchResponseDto,
    ): MeasurementUploadOutcome {
        val durable = response.accepted && response.persisted && response.status.orEmpty().startsWith("ACCEPTED_")
        return if (durable) {
            dao.update(item.copy(status = "done", lastError = null))
            MeasurementUploadOutcome.Uploaded
        } else {
            saveDeadLetter(item, "measurement_upload_not_persisted")
        }
    }

    private suspend fun deadLetter(item: UploadQueueEntity): MeasurementUploadOutcome =
        saveDeadLetter(item, "measurement_payload_invalid")

    private suspend fun saveDeadLetter(
        item: UploadQueueEntity,
        safeError: String,
    ): MeasurementUploadOutcome {
        dao.update(item.copy(status = "dead_letter", lastError = safeError))
        return MeasurementUploadOutcome.DeadLettered
    }

    private suspend fun saveRetry(
        item: UploadQueueEntity,
        safeError: String,
    ): MeasurementUploadOutcome {
        dao.update(item.nextBackoff(safeError))
        return MeasurementUploadOutcome.RetryScheduled
    }

    private companion object {
        const val MEASUREMENT_KIND = "telemetry_batch"
        const val HEALTH_INTERVIEW_KIND = "health_interview"
    }
}

sealed class MeasurementUploadOutcome {
    object Uploaded : MeasurementUploadOutcome()
    object RetryScheduled : MeasurementUploadOutcome()
    object DeadLettered : MeasurementUploadOutcome()
    object Paused : MeasurementUploadOutcome()
    object Skipped : MeasurementUploadOutcome()
}

sealed class QueueState {
    object Active : QueueState()
    object Paused : QueueState()
}
