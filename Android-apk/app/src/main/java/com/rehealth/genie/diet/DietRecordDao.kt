package com.rehealth.genie.diet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DietRecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: DietRecordEntity)

    @Query(
        """
        SELECT diet_records.*, sync_upload_queue.status AS upload_status
        FROM diet_records
        LEFT JOIN sync_upload_queue
          ON sync_upload_queue.id = diet_records.upload_batch_id
        WHERE diet_records.user_id = :userId
          AND diet_records.consumed_at >= :fromInclusive
          AND diet_records.consumed_at < :toExclusive
        ORDER BY diet_records.consumed_at DESC, diet_records.created_at DESC
        """,
    )
    fun observeBetween(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<DietRecordWithUploadState>>

    @Query(
        """
        SELECT * FROM diet_records
        WHERE user_id = :userId AND upload_batch_id IS NULL
        ORDER BY consumed_at ASC
        """,
    )
    suspend fun findNotQueued(userId: String): List<DietRecordEntity>

    /** RDI 归因专用：按用户 + 日期区间拉取当日餐食（不含上传状态 join，纯本地估算用）。 */
    @Query(
        """
        SELECT * FROM diet_records
        WHERE user_id = :userId
          AND consumed_at >= :fromInclusive
          AND consumed_at < :toExclusive
        ORDER BY consumed_at ASC
        """,
    )
    suspend fun recordsBetween(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): List<DietRecordEntity>

    @Query(
        """
        UPDATE diet_records
        SET upload_batch_id = :batchId
        WHERE id = :recordId AND user_id = :userId AND upload_batch_id IS NULL
        """,
    )
    suspend fun attachUploadBatch(recordId: String, userId: String, batchId: String): Int
}
