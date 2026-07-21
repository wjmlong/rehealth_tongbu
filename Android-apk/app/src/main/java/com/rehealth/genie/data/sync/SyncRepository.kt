package com.rehealth.genie.data.sync

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthState
import com.rehealth.genie.network.AuthenticatedApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * D3 upload queue repository with 401-aware pause/resume.
 *
 * When [AuthenticatedApiClient] detects a 401:
 * - Marks queue as [QueueState.Paused]
 * - Stops attempting uploads
 * - Notifies UI via [queueState] Flow
 *
 * After successful re-login:
 * - Call [resumeQueue] to restart uploads
 */
class SyncRepository(
    private val dao: UploadQueueDao,
    private val apiClient: AuthenticatedApiClient,
) {

    private val _queueState = MutableStateFlow<QueueState>(QueueState.Active)
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    suspend fun enqueue(item: UploadQueueEntity) = dao.insert(item)

    suspend fun save(item: UploadQueueEntity) = dao.update(item)

    suspend fun pending(): List<UploadQueueEntity> = dao.pending()

    fun observeOutstanding(): Flow<List<UploadQueueEntity>> = dao.observeOutstanding()

    suspend fun pruneDone() = dao.pruneDone(System.currentTimeMillis() - 7 * 86_400_000L)

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
            nextRetryAt = System.currentTimeMillis() + delayMs,
        )
    }
}

sealed class QueueState {
    object Active : QueueState()
    object Paused : QueueState()
}
