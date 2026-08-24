package com.rehealth.genie.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UploadQueueEntity)

    @Update
    suspend fun update(item: UploadQueueEntity)

    @Query(
        "SELECT * FROM sync_upload_queue " +
            "WHERE owner_user_id = :ownerUserId " +
            "AND status IN ('pending','failed') AND next_retry_at <= :now ORDER BY next_retry_at ASC",
    )
    suspend fun pending(now: Long, ownerUserId: String): List<UploadQueueEntity>

    @Query(
        "SELECT * FROM sync_upload_queue " +
            "WHERE owner_user_id = :ownerUserId AND kind = :kind " +
            "AND status IN ('pending','failed') " +
            "AND next_retry_at <= :now ORDER BY next_retry_at ASC",
    )
    suspend fun pendingByKind(now: Long, kind: String, ownerUserId: String): List<UploadQueueEntity>

    @Query(
        "SELECT * FROM sync_upload_queue " +
            "WHERE owner_user_id = :ownerUserId AND kind != :excludedKind " +
            "AND status IN ('pending','failed') " +
            "AND next_retry_at <= :now ORDER BY next_retry_at ASC",
    )
    suspend fun pendingExcludingKind(now: Long, excludedKind: String, ownerUserId: String): List<UploadQueueEntity>

    @Query(
        "SELECT * FROM sync_upload_queue WHERE owner_user_id = :ownerUserId " +
            "AND status != 'done' ORDER BY created_at DESC",
    )
    fun observeOutstanding(ownerUserId: String): Flow<List<UploadQueueEntity>>

    @Query("DELETE FROM sync_upload_queue WHERE status = 'done' AND created_at < :before")
    suspend fun pruneDone(before: Long)

    @Query("SELECT * FROM sync_upload_queue WHERE id = :id")
    suspend fun getById(id: String): UploadQueueEntity?

    @Query(
        "SELECT * FROM sync_upload_queue WHERE owner_user_id = :ownerUserId " +
            "AND kind = :kind AND status = 'pending'",
    )
    suspend fun getPendingByKind(kind: String, ownerUserId: String): List<UploadQueueEntity>

    @Query(
        "SELECT * FROM sync_upload_queue WHERE owner_user_id = :ownerUserId AND kind = :kind " +
            "AND status IN ('pending','failed') AND next_retry_at <= :now " +
            "ORDER BY next_retry_at ASC LIMIT 1",
    )
    suspend fun nextByKind(now: Long, kind: String, ownerUserId: String): UploadQueueEntity?

    @Query(
        "SELECT * FROM sync_upload_queue WHERE owner_user_id = :ownerUserId " +
            "AND kind != :excludedKind AND status IN ('pending','failed') " +
            "AND next_retry_at <= :now ORDER BY next_retry_at ASC LIMIT 1",
    )
    suspend fun nextExcludingKind(now: Long, excludedKind: String, ownerUserId: String): UploadQueueEntity?

    /** Atomically marks a row in-flight; returns 1 only when the claim won the row. */
    @Query(
        "UPDATE sync_upload_queue SET status = 'uploading', claim_time = :claimedAt " +
            "WHERE id = :id AND status IN ('pending','failed')",
    )
    suspend fun claim(id: String, claimedAt: Long): Int

    /** Returns an in-flight row to pending (e.g. after a 401 pauses the queue). */
    @Query(
        "UPDATE sync_upload_queue SET status = 'pending', claim_time = NULL " +
            "WHERE id = :id AND status = 'uploading'",
    )
    suspend fun releaseClaim(id: String): Int

    /** Recovers rows left in-flight by a crashed worker after the lease expires. */
    @Query(
        "UPDATE sync_upload_queue SET status = 'pending', claim_time = NULL " +
            "WHERE status = 'uploading' AND claim_time IS NOT NULL AND claim_time < :before",
    )
    suspend fun releaseStaleClaims(before: Long): Int
}
