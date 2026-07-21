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

    @Query("SELECT * FROM intervention_feedback_queue WHERE upload_status IN ('pending', 'failed') AND next_retry_at <= :now ORDER BY created_at ASC")
    suspend fun pendingUploads(now: Long = System.currentTimeMillis()): List<InterventionFeedbackEntity>

    @Query("SELECT * FROM intervention_feedback_queue WHERE intervention_id = :interventionId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestForIntervention(interventionId: String): InterventionFeedbackEntity?

    @Query("SELECT * FROM intervention_feedback_queue WHERE upload_status != 'done' ORDER BY created_at DESC")
    fun observePendingFeedback(): Flow<List<InterventionFeedbackEntity>>

    @Query("DELETE FROM intervention_feedback_queue WHERE upload_status = 'done' AND created_at < :cutoffTimestamp")
    suspend fun pruneDone(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM intervention_feedback_queue WHERE upload_status = 'pending'")
    suspend fun countPending(): Int
}
