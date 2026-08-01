package com.rehealth.genie.rhi

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rehealth.genie.data.AppDatabase
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.data.sync.UploadQueueDao
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.AuthState
import com.rehealth.genie.network.MeasurementUploadClient
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import com.rehealth.genie.ring.data.RingActivityEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real [RhiRepository] against a real Room database.
 *
 * The migration test proves the tables can be created; this proves they are
 * actually written by the production code path, which is the part a manual UI
 * walkthrough cannot distinguish from "there was simply no data to score".
 */
@RunWith(AndroidJUnit4::class)
class RhiSnapshotPersistenceTest {
    private lateinit var database: AppDatabase
    private lateinit var syncDatabase: TestSyncDatabase
    private val zoneId: ZoneId = ZoneId.of("Asia/Shanghai")
    private val today: LocalDate = LocalDate.of(2026, 7, 31)
    private val userId = "persistence-user"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        syncDatabase = Room.inMemoryDatabaseBuilder(context, TestSyncDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        syncDatabase.close()
    }

    @Test
    fun refreshPeriod_writesIndexDomainFeatureAndQualityRows() = runBlocking {
        seedActivities(steps = 7_000)
        val repository = repository()

        val summary = repository.refreshPeriod(periodDays = 7, scoredOn = today)
        assertNotNull("expected a scored 7-day summary", summary.score)

        val dao = database.rhiSnapshotDao()
        val index = dao.getIndex(userId, today.toString())
        assertNotNull("index row must be persisted", index)
        assertEquals(summary.score!!, index!!.displayScore, 0.001)
        assertEquals(RHI_LITE_ALGORITHM_VERSION, index.algorithmVersion)
        assertEquals("local", index.calculationSource)
        assertEquals(RHI_DISPLAY_SMOOTHING_ALPHA, index.smoothingAlpha, 0.0001)

        // Every domain is recorded, including the ones with no eligible
        // indicator, so an excluded domain is visibly NULL rather than absent.
        val domains = dao.getDomains(userId, today.toString())
        assertEquals(RHI_DOMAIN_WEIGHTS.size, domains.size)
        assertTrue(
            "activity_fitness must be scored from seeded steps",
            domains.first { it.domain == "activity_fitness" }.score != null,
        )
        assertNull(
            "a domain without eligible evidence must be NULL, never a neutral 50",
            domains.first { it.domain == "metabolic_control" }.score,
        )

        val bundle = dao.getBundle(userId, today.toString())
        assertNotNull(bundle)
        assertTrue("feature snapshot must be persisted", bundle!!.features.isNotEmpty())
        assertTrue(
            "steps_7d_mean must be among persisted features",
            bundle.features.any { it.feature == "steps_7d_mean" },
        )

        val quality = dao.getQuality(userId, today.toString())
        assertNotNull("quality snapshot must be persisted", quality)
        assertEquals(index.dataConfidence, quality!!.confidenceScore, 0.001)
        assertEquals(rhiConfidenceGrade(index.dataConfidence), quality.confidenceGrade)
    }

    @Test
    fun recomputationReplacesTheDayInsteadOfDuplicatingIt() = runBlocking {
        seedActivities(steps = 7_000)
        val repository = repository()

        repository.refreshPeriod(periodDays = 7, scoredOn = today)
        repository.refreshPeriod(periodDays = 7, scoredOn = today)

        val dao = database.rhiSnapshotDao()
        assertEquals(
            "a second run must replace the day, not append a duplicate",
            RHI_DOMAIN_WEIGHTS.size,
            dao.getDomains(userId, today.toString()).size,
        )
        assertEquals(1, dao.getIndexRange(userId, today.toString(), today.toString()).size)
    }

    @Test
    fun stepsWithoutExerciseMinutesPersistTheQualityWarning() = runBlocking {
        // The "945 steps but 0 minutes" case: ambulation is recorded but the
        // exercise-minute feed is broken, which must be surfaced, not silently skipped.
        seedActivities(steps = 945)
        val repository = repository()

        repository.refreshPeriod(periodDays = 7, scoredOn = today)

        val quality = database.rhiSnapshotDao().getQuality(userId, today.toString())
        assertNotNull(quality)
        assertTrue(
            "expected activity_duration_missing, got ${quality!!.warningCodes}",
            quality.warningCodes.contains("activity_duration_missing"),
        )
    }

    @Test
    fun anonymousSessionScoresWithoutPersisting() = runBlocking {
        seedActivities(steps = 7_000)
        val repository = RhiRepository(
            ringDataDao = database.ringDataDao(),
            snapshotDao = database.rhiSnapshotDao(),
            userIdProvider = { null },
            zoneId = zoneId,
        )

        // Persistence is a side effect of scoring, never a precondition: a user
        // without a session still gets a score, it is just not stored.
        val summary = repository.refreshPeriod(periodDays = 7, scoredOn = today)

        assertNotNull(summary.score)
        assertNull(database.rhiSnapshotDao().getIndex(userId, today.toString()))
    }

    private fun repository(): RhiRepository = RhiRepository(
        ringDataDao = database.ringDataDao(),
        snapshotDao = database.rhiSnapshotDao(),
        syncRepository = SyncRepository(
            dao = syncDatabase.uploadQueueDao(),
            apiClient = stubUploadClient,
        ),
        userIdProvider = { userId },
        zoneId = zoneId,
    )

    /**
     * Enqueuing the computed RHI snapshot is the Android-side half of "upload
     * RHI to the backend". The queue row is created by the production persist
     * path regardless of whether the (not-yet-implemented) backend endpoint
     * accepts it; a rejected upload becomes a dead letter, never a crash.
     */
    @Test
    fun refreshPeriod_enqueuesRhiSnapshotForUpload() = runBlocking {
        seedActivities(steps = 7_000)
        val repository = repository()

        repository.refreshPeriod(periodDays = 7, scoredOn = today)

        val queued = syncDatabase.uploadQueueDao().observeOutstanding().first()
        val rhiItems = queued.filter { it.kind == "rhi_daily_snapshot" }
        assertTrue("a rhi_daily_snapshot item must be enqueued", rhiItems.isNotEmpty())
        val payload = rhiItems.first()
        assertTrue(payload.payloadJson.contains("\"userId\":\"$userId\""))
        assertTrue(
            "payload must carry the computed day snapshot",
            payload.payloadJson.contains(today.toString()),
        )
    }

    private val stubUploadClient = object : MeasurementUploadClient {
        override val authState: AuthState = AuthState.Authorized
        override suspend fun uploadMeasurements(
            request: TelemetryBatchRequestDto,
        ): com.rehealth.genie.network.ApiResult<TelemetryBatchResponseDto> =
            com.rehealth.genie.network.ApiResult.NetworkError("stub")
    }

    private suspend fun seedActivities(steps: Int) {
        val activities = (0L..34L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            RingActivityEntity(
                id = "activity-$daysAgo",
                startedAt = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endedAt = null,
                activityType = "daily_summary",
                steps = steps,
                distanceMeters = 0.0,
                caloriesKcal = 0.0,
                durationMinutes = 0,
                averageHeartRate = null,
                source = "TEST_DEVICE",
            )
        }
        database.ringDataDao().insertActivities(activities)
    }
}

@androidx.room.Database(entities = [UploadQueueEntity::class], version = 1)
abstract class TestSyncDatabase : androidx.room.RoomDatabase() {
    abstract fun uploadQueueDao(): UploadQueueDao
}
