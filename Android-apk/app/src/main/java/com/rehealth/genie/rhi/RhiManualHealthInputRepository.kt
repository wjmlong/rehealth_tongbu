package com.rehealth.genie.rhi

import com.google.gson.Gson
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.RhiManualHealthInputSyncClient
import com.rehealth.genie.network.dto.toEntity
import com.rehealth.genie.network.dto.toNetworkDto

/** Local-first persistence and retryable MySQL synchronization for manual health archive data. */
class RhiManualHealthInputRepository(
    private val dao: RhiManualHealthInputDao,
    private val syncRepository: SyncRepository,
    private val apiClient: RhiManualHealthInputSyncClient,
    private val triggerSync: () -> Unit,
    private val gson: Gson = Gson(),
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    suspend fun save(input: RhiManualHealthInputEntity) {
        dao.upsert(input)
        enqueue(input)
        triggerSync()
    }

    /**
     * Restores a newer server copy on login/profile open. A newer local copy remains
     * authoritative and is queued for upload, so opening the screen never loses edits.
     */
    suspend fun refreshFromCloud(userId: String) {
        val local = dao.get(userId)
        when (val result = apiClient.getRhiManualHealthInput()) {
            is ApiResult.Success -> {
                val remote = result.data
                when {
                    remote == null && local != null -> {
                        enqueue(local)
                        triggerSync()
                    }
                    remote != null && (local == null || remote.updatedAt > local.updatedAt) ->
                        dao.upsert(remote.toEntity(userId))
                    remote != null && local != null && local.updatedAt > remote.updatedAt -> {
                        enqueue(local)
                        triggerSync()
                    }
                }
            }
            else -> Unit
        }
    }

    private suspend fun enqueue(input: RhiManualHealthInputEntity) {
        val now = nowProvider()
        syncRepository.enqueue(
            UploadQueueEntity(
                id = "rhi-manual:${input.userId}",
                kind = "rhi_manual_health_input",
                payloadJson = gson.toJson(input.toNetworkDto()),
                status = "pending",
                attempts = 0,
                lastError = null,
                createdAt = now,
                nextRetryAt = now,
            ),
        )
    }
}
