package com.rehealth.genie.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * D3 typed intervention feedback queue.
 *
 * Replaces legacy `submitCheckIn` with proper intervention-scoped feedback.
 * Each feedback references a specific intervention ID and tracks completion status.
 *
 * status values: "completed" | "partially_completed" | "skipped" | "not_applicable"
 */
@Entity(tableName = "intervention_feedback_queue")
data class InterventionFeedbackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "intervention_id") val interventionId: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "checked_at") val checkedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "upload_status") val uploadStatus: String = "pending", // pending | uploading | done | failed
    @ColumnInfo(name = "upload_attempts") val uploadAttempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long = 0,
)
