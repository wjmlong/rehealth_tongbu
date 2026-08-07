package com.rehealth.genie.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserScopedTelemetryDaoTest {
    @Test
    fun ownerQueriesNeverReturnAnotherUsersRows() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = database.ringDataDao()
            dao.insertBatch(batch("user-a", 100L))
            dao.insertBatch(batch("user-b", 200L))

            assertEquals(listOf("measurement-user-b"), dao.observeLatestMeasurementsForOwner("user-b").first().map { it.id })
            assertEquals("sleep-user-b", dao.observeLatestSleepSessionForOwner("user-b").first()?.id)
            assertEquals(listOf("activity-user-b"), dao.observeActivitiesForOwner("user-b").first().map { it.id })
            assertEquals(listOf("signal-user-b"), dao.observeLatestSignalChunksForOwner("user-b").first().map { it.id })
        } finally {
            database.close()
        }
    }

    private fun batch(userId: String, timestamp: Long) = RingDataBatch(
        measurements = listOf(
            RingMeasurementEntity(
                id = "measurement-$userId",
                metricType = "HEART_RATE",
                measuredAt = timestamp,
                primaryValue = 70.0,
                unit = "bpm",
                source = "hband_wearable",
                ownerUserId = userId,
                deviceId = "device",
            ),
        ),
        sleepSessions = listOf(
            RingSleepSessionEntity(
                id = "sleep-$userId",
                startedAt = timestamp,
                endedAt = timestamp + 1,
                deepMinutes = 1,
                lightMinutes = 0,
                awakeMinutes = 0,
                remMinutes = 0,
                interruptionMinutes = 0,
                source = "hband_wearable",
                ownerUserId = userId,
                deviceId = "device",
            ),
        ),
        activities = listOf(
            RingActivityEntity(
                id = "activity-$userId",
                startedAt = timestamp,
                endedAt = timestamp + 1,
                activityType = "walk",
                steps = 100,
                distanceMeters = 20.0,
                caloriesKcal = 5.0,
                durationMinutes = 1,
                averageHeartRate = null,
                source = "hband_wearable",
                ownerUserId = userId,
                deviceId = "device",
            ),
        ),
        signalChunks = listOf(
            RingSignalChunkEntity(
                id = "signal-$userId",
                signalType = "ECG",
                startedAt = timestamp,
                sampleRateHz = 100,
                sampleCount = 1,
                payload = byteArrayOf(1),
                source = "hband_wearable",
                ownerUserId = userId,
                deviceId = "device",
            ),
        ),
    )
}
