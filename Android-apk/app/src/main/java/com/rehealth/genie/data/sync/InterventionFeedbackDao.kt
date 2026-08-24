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

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status IN ('pending', 'retry', 'failed') AND next_retry_at <= :now ORDER BY created_at ASC")
    suspend fun pendingUploads(ownerUserId: String, now: Long = System.currentTimeMillis()): List<InterventionFeedbackEntity>

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND id = :feedbackId LIMIT 1")
    fun observeFeedback(ownerUserId: String, feedbackId: String): Flow<InterventionFeedbackEntity?>

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND intervention_id = :interventionId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestForIntervention(ownerUserId: String, interventionId: String): InterventionFeedbackEntity?

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status IN ('pending', 'retry', 'failed', 'dead_letter') ORDER BY created_at DESC")
    fun observePendingFeedback(ownerUserId: String): Flow<List<InterventionFeedbackEntity>>

    @Query(
        "UPDATE intervention_feedback_queue SET upload_status = 'superseded' " +
            "WHERE owner_user_id = :ownerUserId AND occurrence_id = :occurrenceId " +
            "AND upload_status IN ('pending', 'retry', 'failed', 'dead_letter')",
    )
    suspend fun supersedeForOccurrence(ownerUserId: String, occurrenceId: String)

    @Query(
        "UPDATE intervention_feedback_queue SET upload_status = 'superseded' " +
            "WHERE owner_user_id = :ownerUserId AND intervention_id = :interventionId " +
            "AND occurrence_id IS NULL AND upload_status = 'dead_letter'",
    )
    suspend fun supersedeDeadLettersForIntervention(ownerUserId: String, interventionId: String)

    @Query("SELECT * FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND checked_at >= :since ORDER BY checked_at ASC")
    suspend fun completedFeedbackSince(ownerUserId: String, since: Long): List<InterventionFeedbackEntity>

    @Query("DELETE FROM intervention_feedback_queue WHERE upload_status IN ('done', 'superseded') AND created_at < :cutoffTimestamp")
    suspend fun pruneDone(cutoffTimestamp: Long)

    @Query("DELETE FROM intervention_feedback_queue WHERE upload_status = 'dead_letter' AND created_at < :cutoffTimestamp")
    suspend fun pruneDeadLetters(cutoffTimestamp: Long)

    @Query("SELECT COUNT(*) FROM intervention_feedback_queue WHERE owner_user_id = :ownerUserId AND upload_status IN ('pending', 'retry', 'failed')")
    suspend fun countPending(ownerUserId: String): Int
}
