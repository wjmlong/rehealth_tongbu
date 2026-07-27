package com.rehealth.genie.data.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Offline-first upload queue row. Producers (BLE collection, feature extraction,
 * intervention feedback) enqueue payloads; a flusher drains them to the WSL2
 * backend and updates status with exponential backoff.
 *
 * kind values: "telemetry_batch" | "feature_evaluate" | "intervention_feedback"
 *              | "device_bind" | "attribution_events" | "health_interview"
 * status values: "pending" | "uploading" | "done" | "failed"
 */
@Entity(tableName = "sync_upload_queue")
data class UploadQueueEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "attempts") val attempts: Int = 0,
    @ColumnInfo(name = "last_error") val lastError: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "next_retry_at") val nextRetryAt: Long,
)
