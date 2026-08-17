package com.rehealth.genie.data.sync

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: InterventionFeedbackEntity)

    @Update
    suspend fun update(feedback: InterventionFeedbackEntity)

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status IN ('pending', 'failed') AND next_retry_at <= :now ORDER BY created_at ASC")
    suspend fun pendingUploads(ownerUserId: String, now: Long = System.currentTimeMillis()): List<InterventionFeedbackEntity>

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND intervention_id = :interventionId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestForIntervention(ownerUserId: String, interventionId: String): InterventionFeedbackEntity?

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status != 'done' ORDER BY created_at DESC")
    fun observePendingFeedback(ownerUserId: String): Flow<List<InterventionFeedbackEntity>>

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND checked_at >= :since ORDER BY checked_at ASC")
    suspend fun completedFeedbackSince(ownerUserId: String, since: Long): List<InterventionFeedbackEntity>

    @Query("DELETE FROM intervention_feedback_queue WHERE upload_status = 'done' AND created_at < :cutoffTimestamp")
    suspend fun pruneDone(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status = 'pending'")
    suspend fun countPending(ownerUserId: String): Int
}
