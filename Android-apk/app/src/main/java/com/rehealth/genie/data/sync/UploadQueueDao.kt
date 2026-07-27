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
            "WHERE status IN ('pending','failed') AND next_retry_at <= :now ORDER BY next_retry_at ASC",
    )
    suspend fun pending(now: Long): List<UploadQueueEntity>

    @Query("SELECT * FROM sync_upload_queue WHERE status != 'done' ORDER BY created_at DESC")
    fun observeOutstanding(): Flow<List<UploadQueueEntity>>

    @Query("DELETE FROM sync_upload_queue WHERE status = 'done' AND created_at < :before")
    suspend fun pruneDone(before: Long)

    @Query("SELECT * FROM sync_upload_queue WHERE id = :id")
    suspend fun getById(id: String): UploadQueueEntity?

    @Query("SELECT * FROM sync_upload_queue WHERE kind = :kind AND status = 'pending'")
    suspend fun getPendingByKind(kind: String): List<UploadQueueEntity>
}
