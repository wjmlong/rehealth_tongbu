package com.rehealth.genie.ring.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RingDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(records: List<RingMeasurementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleepSessions(records: List<RingSleepSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(records: List<RingActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignalChunks(records: List<RingSignalChunkEntity>)

    @Query(
        "SELECT * FROM ring_measurements WHERE metric_type = :metricType ORDER BY measured_at DESC LIMIT :limit",
    )
    fun observeMeasurements(metricType: String, limit: Int = 100): Flow<List<RingMeasurementEntity>>

    @Query("SELECT * FROM ring_sleep_sessions ORDER BY started_at DESC LIMIT :limit")
    fun observeSleepSessions(limit: Int = 30): Flow<List<RingSleepSessionEntity>>

    @Query("SELECT * FROM ring_activities ORDER BY started_at DESC LIMIT :limit")
    fun observeActivities(limit: Int = 100): Flow<List<RingActivityEntity>>

    @Query(
        "SELECT * FROM ring_signal_chunks WHERE signal_type = :signalType ORDER BY started_at DESC LIMIT :limit",
    )
    fun observeSignalChunks(signalType: String, limit: Int = 20): Flow<List<RingSignalChunkEntity>>

    @Query(
        """
        SELECT measurement.* FROM ring_measurements AS measurement
        INNER JOIN (
            SELECT metric_type, MAX(measured_at) AS latest_at
            FROM ring_measurements
            GROUP BY metric_type
        ) AS latest
        ON measurement.metric_type = latest.metric_type
        AND measurement.measured_at = latest.latest_at
        """,
    )
    fun observeLatestMeasurements(): Flow<List<RingMeasurementEntity>>

    @Query("SELECT * FROM ring_sleep_sessions ORDER BY started_at DESC LIMIT 1")
    fun observeLatestSleepSession(): Flow<RingSleepSessionEntity?>

    @Query("SELECT * FROM ring_activities ORDER BY started_at DESC LIMIT 1")
    fun observeLatestActivity(): Flow<RingActivityEntity?>

    @Query(
        """
        SELECT signal.* FROM ring_signal_chunks AS signal
        INNER JOIN (
            SELECT signal_type, MAX(started_at) AS latest_at
            FROM ring_signal_chunks
            GROUP BY signal_type
        ) AS latest
        ON signal.signal_type = latest.signal_type
        AND signal.started_at = latest.latest_at
        """,
    )
    fun observeLatestSignalChunks(): Flow<List<RingSignalChunkEntity>>

    @Transaction
    suspend fun insertBatch(batch: RingDataBatch) {
        insertMeasurements(batch.measurements)
        insertSleepSessions(batch.sleepSessions)
        insertActivities(batch.activities)
        insertSignalChunks(batch.signalChunks)
    }
}

data class RingDataBatch(
    val measurements: List<RingMeasurementEntity> = emptyList(),
    val sleepSessions: List<RingSleepSessionEntity> = emptyList(),
    val activities: List<RingActivityEntity> = emptyList(),
    val signalChunks: List<RingSignalChunkEntity> = emptyList(),
) {
    val size: Int
        get() = measurements.size + sleepSessions.size + activities.size + signalChunks.size
}
