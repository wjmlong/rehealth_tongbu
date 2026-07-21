package com.rehealth.genie.ring.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ring_measurements",
    indices = [Index(value = ["metric_type", "measured_at"])],
)
data class RingMeasurementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "metric_type") val metricType: String,
    @ColumnInfo(name = "measured_at") val measuredAt: Long,
    @ColumnInfo(name = "primary_value") val primaryValue: Double,
    @ColumnInfo(name = "secondary_value") val secondaryValue: Double? = null,
    val unit: String,
    val quality: Int? = null,
    val source: String,
    @ColumnInfo(name = "raw_payload") val rawPayload: String? = null,
)

@Entity(tableName = "ring_sleep_sessions")
data class RingSleepSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "deep_minutes") val deepMinutes: Int,
    @ColumnInfo(name = "light_minutes") val lightMinutes: Int,
    @ColumnInfo(name = "awake_minutes") val awakeMinutes: Int,
    @ColumnInfo(name = "rem_minutes") val remMinutes: Int,
    @ColumnInfo(name = "interruption_minutes") val interruptionMinutes: Int,
    val source: String,
    @ColumnInfo(name = "raw_payload") val rawPayload: String? = null,
)

@Entity(tableName = "ring_activities")
data class RingActivityEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "activity_type") val activityType: String,
    val steps: Int,
    @ColumnInfo(name = "distance_meters") val distanceMeters: Double,
    @ColumnInfo(name = "calories_kcal") val caloriesKcal: Double,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "average_heart_rate") val averageHeartRate: Double?,
    val source: String,
    @ColumnInfo(name = "raw_payload") val rawPayload: String? = null,
)

@Entity(
    tableName = "ring_signal_chunks",
    indices = [Index(value = ["signal_type", "started_at"])],
)
data class RingSignalChunkEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "signal_type") val signalType: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "sample_rate_hz") val sampleRateHz: Int?,
    @ColumnInfo(name = "sample_count") val sampleCount: Int,
    val encoding: String = "INT32_LE",
    val payload: ByteArray,
    val source: String,
) {
    override fun equals(other: Any?): Boolean =
        other is RingSignalChunkEntity &&
            id == other.id &&
            payload.contentEquals(other.payload)

    override fun hashCode(): Int = 31 * id.hashCode() + payload.contentHashCode()
}
