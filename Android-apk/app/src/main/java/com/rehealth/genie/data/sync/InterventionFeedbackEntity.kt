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
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String? = null,
    @ColumnInfo(name = "intervention_id") val interventionId: String,
    @ColumnInfo(name = "binding_id") val bindingId: String? = null,
    @ColumnInfo(name = "tenant_id") val tenantId: Int? = null,
    @ColumnInfo(name = "plan_item_id") val planItemId: String? = null,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "note") val note: String? = null,
    @ColumnInfo(name = "expected_count") val expectedCount: Double? = null,
    @ColumnInfo(name = "completed_count") val completedCount: Double? = null,
    @ColumnInfo(name = "verification_type") val verificationType: String = "self_report",
    @ColumnInfo(name = "checked_at") val checkedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "upload_status") val uploadStatus: String = "pending", // pending | uploading | done | failed
    @ColumnInfo(name = "upload_attempts") val uploadAttempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long = 0,
)
