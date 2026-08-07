package com.rehealth.genie.ring.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import java.nio.charset.StandardCharsets
import java.util.UUID
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

    @Query(
        """
        SELECT * FROM ring_measurements
        WHERE owner_user_id = :ownerUserId AND metric_type = :metricType
        ORDER BY measured_at DESC LIMIT :limit
        """,
    )
    fun observeMeasurementsForOwner(
        ownerUserId: String,
        metricType: String,
        limit: Int = 100,
    ): Flow<List<RingMeasurementEntity>>

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

    @Query(
        """
        SELECT measurement.* FROM ring_measurements AS measurement
        INNER JOIN (
            SELECT metric_type, MAX(measured_at) AS latest_at
            FROM ring_measurements
            WHERE owner_user_id = :ownerUserId
            GROUP BY metric_type
        ) AS latest
        ON measurement.metric_type = latest.metric_type
        AND measurement.measured_at = latest.latest_at
        WHERE measurement.owner_user_id = :ownerUserId
        """,
    )
    fun observeLatestMeasurementsForOwner(ownerUserId: String): Flow<List<RingMeasurementEntity>>

    @Query(
        """
        SELECT measurement.* FROM ring_measurements AS measurement
        INNER JOIN (
            SELECT metric_type, MAX(measured_at) AS latest_at
            FROM ring_measurements
            WHERE owner_user_id = :ownerUserId
              AND device_id = :deviceId
              AND source = :source
            GROUP BY metric_type
        ) AS latest
        ON measurement.metric_type = latest.metric_type
        AND measurement.measured_at = latest.latest_at
        WHERE measurement.owner_user_id = :ownerUserId
          AND measurement.device_id = :deviceId
          AND measurement.source = :source
        """,
    )
    fun observeLatestMeasurementsForBinding(
        ownerUserId: String,
        deviceId: String,
        source: String,
    ): Flow<List<RingMeasurementEntity>>

    @Query("SELECT * FROM ring_sleep_sessions ORDER BY started_at DESC LIMIT 1")
    fun observeLatestSleepSession(): Flow<RingSleepSessionEntity?>

    @Query(
        "SELECT * FROM ring_sleep_sessions WHERE owner_user_id = :ownerUserId ORDER BY started_at DESC LIMIT 1",
    )
    fun observeLatestSleepSessionForOwner(ownerUserId: String): Flow<RingSleepSessionEntity?>

    @Query("SELECT * FROM ring_activities ORDER BY started_at DESC LIMIT 1")
    fun observeLatestActivity(): Flow<RingActivityEntity?>

    @Query(
        "SELECT * FROM ring_activities WHERE owner_user_id = :ownerUserId ORDER BY started_at DESC LIMIT :limit",
    )
    fun observeActivitiesForOwner(ownerUserId: String, limit: Int = 100): Flow<List<RingActivityEntity>>

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

    @Query(
        """
        SELECT signal.* FROM ring_signal_chunks AS signal
        INNER JOIN (
            SELECT signal_type, MAX(started_at) AS latest_at
            FROM ring_signal_chunks
            WHERE owner_user_id = :ownerUserId
            GROUP BY signal_type
        ) AS latest
        ON signal.signal_type = latest.signal_type
        AND signal.started_at = latest.latest_at
        WHERE signal.owner_user_id = :ownerUserId
        """,
    )
    fun observeLatestSignalChunksForOwner(ownerUserId: String): Flow<List<RingSignalChunkEntity>>

    @Query(
        """
        SELECT * FROM ring_signal_chunks
        WHERE owner_user_id = :ownerUserId AND signal_type = :signalType
        ORDER BY started_at DESC LIMIT :limit
        """,
    )
    fun observeSignalChunksForOwner(
        ownerUserId: String,
        signalType: String,
        limit: Int = 20,
    ): Flow<List<RingSignalChunkEntity>>

    @Query("SELECT * FROM ring_measurements WHERE measured_at >= :since ORDER BY measured_at DESC")
    suspend fun getMeasurementsSince(since: Long): List<RingMeasurementEntity>

    @Query(
        "SELECT * FROM ring_measurements WHERE owner_user_id = :ownerUserId AND measured_at >= :since ORDER BY measured_at DESC",
    )
    suspend fun getMeasurementsSinceForOwner(since: Long, ownerUserId: String): List<RingMeasurementEntity>

    @Query(
        """
        SELECT * FROM ring_measurements
        WHERE measured_at >= :since
          AND owner_user_id = :ownerUserId
          AND device_id = :deviceId
          AND source = :source
        ORDER BY measured_at DESC
        """,
    )
    suspend fun getMeasurementsSinceForBinding(
        since: Long,
        ownerUserId: String,
        deviceId: String,
        source: String,
    ): List<RingMeasurementEntity>

    @Query(
        """
        SELECT MAX(measured_at) FROM ring_measurements
        WHERE owner_user_id = :ownerUserId
          AND device_id = :deviceId
          AND source = :source
        """,
    )
    suspend fun getLatestMeasuredAtForBinding(
        ownerUserId: String,
        deviceId: String,
        source: String,
    ): Long?

    @Query(
        "SELECT * FROM ring_measurements WHERE metric_type = :metricType ORDER BY measured_at DESC LIMIT 1",
    )
    suspend fun getLatestMeasurement(metricType: String): RingMeasurementEntity?

    @Query("SELECT * FROM ring_activities WHERE started_at >= :since ORDER BY started_at DESC")
    suspend fun getActivitiesSince(since: Long): List<RingActivityEntity>

    @Query(
        "SELECT * FROM ring_activities WHERE owner_user_id = :ownerUserId AND started_at >= :since ORDER BY started_at DESC",
    )
    suspend fun getActivitiesSinceForOwner(since: Long, ownerUserId: String): List<RingActivityEntity>

    @Query("SELECT * FROM ring_sleep_sessions WHERE ended_at >= :since ORDER BY started_at DESC")
    suspend fun getSleepSessionsSince(since: Long): List<RingSleepSessionEntity>

    @Query(
        "SELECT * FROM ring_sleep_sessions WHERE owner_user_id = :ownerUserId AND ended_at >= :since ORDER BY started_at DESC",
    )
    suspend fun getSleepSessionsSinceForOwner(since: Long, ownerUserId: String): List<RingSleepSessionEntity>

    @Query("DELETE FROM ring_measurements WHERE source = :source")
    suspend fun deleteMeasurementsBySource(source: String)

    @Query("DELETE FROM ring_sleep_sessions WHERE source = :source")
    suspend fun deleteSleepSessionsBySource(source: String)

    @Query("DELETE FROM ring_activities WHERE source = :source")
    suspend fun deleteActivitiesBySource(source: String)

    @Query("DELETE FROM ring_signal_chunks WHERE source = :source")
    suspend fun deleteSignalChunksBySource(source: String)

    @Transaction
    suspend fun deleteSourceData(source: String) {
        deleteMeasurementsBySource(source)
        deleteSleepSessionsBySource(source)
        deleteActivitiesBySource(source)
        deleteSignalChunksBySource(source)
    }

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

    fun ownedBy(ownerUserId: String, deviceId: String?): RingDataBatch = copy(
        measurements = measurements.map {
            it.copy(id = scopedRecordId(ownerUserId, it.id), ownerUserId = ownerUserId, deviceId = deviceId)
        },
        sleepSessions = sleepSessions.map {
            it.copy(id = scopedRecordId(ownerUserId, it.id), ownerUserId = ownerUserId, deviceId = deviceId)
        },
        activities = activities.map {
            it.copy(id = scopedRecordId(ownerUserId, it.id), ownerUserId = ownerUserId, deviceId = deviceId)
        },
        signalChunks = signalChunks.map {
            it.copy(id = scopedRecordId(ownerUserId, it.id), ownerUserId = ownerUserId, deviceId = deviceId)
        },
    )
}

private fun scopedRecordId(ownerUserId: String, sourceId: String): String =
    "wearable-${UUID.nameUUIDFromBytes(
        "$ownerUserId|$sourceId".toByteArray(StandardCharsets.UTF_8),
    )}"
