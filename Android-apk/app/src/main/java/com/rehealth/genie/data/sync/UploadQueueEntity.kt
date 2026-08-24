package com.rehealth.genie.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Offline-first upload queue row. Producers (BLE collection, feature extraction,
 * intervention feedback) enqueue payloads; a flusher drains them to the WSL2
 * backend and updates status with exponential backoff.
 *
 * Active kind values include "telemetry_batch", "health_interview",
 * "rhi_daily_snapshot", "rdi_daily_snapshot", and "rhi_manual_health_input".
 * status values: "pending" | "uploading" | "done" | "failed" | "dead_letter"
 */
@Entity(
    tableName = "sync_upload_queue",
    indices = [
        Index(
            value = ["owner_user_id", "status", "next_retry_at"],
            name = "index_sync_upload_queue_owner_status_retry",
        ),
    ],
)
data class UploadQueueEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long,
    @ColumnInfo(name = "owner_user_id") val ownerUserId: String? = null,
    @ColumnInfo(name = "claim_time") val claimTime: Long? = null,
)
