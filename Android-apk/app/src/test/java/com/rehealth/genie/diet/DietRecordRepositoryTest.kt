package com.rehealth.genie.diet

import com.google.gson.Gson
import com.rehealth.genie.data.sync.SyncRepository
import com.rehealth.genie.data.sync.UploadQueueDao
import com.rehealth.genie.data.sync.UploadQueueEntity
import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthState
import com.rehealth.genie.network.MeasurementUploadClient
import com.rehealth.genie.network.dto.TelemetryBatchRequestDto
import com.rehealth.genie.network.dto.TelemetryBatchResponseDto
import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.WearableVendor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DietRecordRepositoryTest {
    @Test
    fun savePersistsLocallyBeforeDeviceBindingIsAvailable() = runTest {
        val dietDao = FakeDietRecordDao()
        val queueDao = FakeUploadQueueDao()
        val repository = repository(dietDao, queueDao, binding = { null })

        val result = repository.save(validDraft())

        assertFalse(result.queued)
        assertEquals(1, dietDao.records.size)
        assertEquals("user-1", dietDao.records.single().userId)
        assertTrue(queueDao.rows.isEmpty())
    }

    @Test
    fun saveQueuesTelemetryV2DietBatchUsingBoundDeviceIdentity() = runTest {
        val dietDao = FakeDietRecordDao()
        val queueDao = FakeUploadQueueDao()
        val repository = repository(dietDao, queueDao, binding = { binding() })

        val result = repository.save(validDraft())

        assertTrue(result.queued)
        val queued = queueDao.rows.single()
        val request = Gson().fromJson(queued.payloadJson, TelemetryBatchRequestDto::class.java)
        assertEquals("telemetry-v2", request.schemaVersion)
        assertTrue(request.deviceId.orEmpty().startsWith("mrd-"))
        assertEquals(1, request.dietRecords?.size)
        assertEquals("lunch", request.dietRecords?.single()?.get("mealType"))
        assertEquals("牛肉面 + 鸡蛋", request.dietRecords?.single()?.get("description"))
        assertNotNull(dietDao.records.single().uploadBatchId)
    }

    @Test
    fun pendingLocalRecordIsQueuedOnceAfterBindingAppears() = runTest {
        val dietDao = FakeDietRecordDao()
        val queueDao = FakeUploadQueueDao()
        var activeBinding: ActiveWearableBinding? = null
        val repository = repository(dietDao, queueDao, binding = { activeBinding })
        repository.save(validDraft())

        activeBinding = binding()
        assertEquals(1, repository.preparePendingUploads())
        assertEquals(0, repository.preparePendingUploads())
        assertEquals(1, queueDao.rows.size)
    }

    private fun repository(
        dietDao: FakeDietRecordDao,
        queueDao: FakeUploadQueueDao,
        binding: () -> ActiveWearableBinding?,
    ): DietRecordRepository = DietRecordRepository(
        dao = dietDao,
        syncRepository = SyncRepository(queueDao, FakeMeasurementUploadClient()),
        userIdProvider = { "user-1" },
        wearableBindingProvider = binding,
        triggerSync = {},
        nowProvider = { NOW },
    )

    private fun validDraft() = DietRecordDraft(
        mealType = "lunch",
        description = "牛肉面 + 鸡蛋",
        caloriesKcal = 780.0,
        proteinGrams = 29.0,
        carbohydrateGrams = 86.0,
        fatGrams = 25.0,
        consumedAt = NOW,
    )

    private fun binding() = ActiveWearableBinding(
        productCode = "RH-MRD-S01",
        vendor = WearableVendor.MRD,
        address = "AA:BB:CC:DD:EE:FF",
        deviceName = "MR11",
        modelCode = "MR11",
        firmwareVersion = null,
        capabilityJson = null,
        boundAt = NOW,
        lastDeviceChangedAt = NOW,
    )

    private companion object {
        const val NOW = 1_785_484_800_000L
    }
}

private class FakeDietRecordDao : DietRecordDao {
    val records = mutableListOf<DietRecordEntity>()
    private val observed = MutableStateFlow<List<DietRecordWithUploadState>>(emptyList())

    override suspend fun insert(record: DietRecordEntity) {
        records += record
        publish()
    }

    override fun observeBetween(
        userId: String,
        fromInclusive: Long,
        toExclusive: Long,
    ): Flow<List<DietRecordWithUploadState>> = observed

    override suspend fun findNotQueued(userId: String): List<DietRecordEntity> =
        records.filter { it.userId == userId && it.uploadBatchId == null }

    override suspend fun attachUploadBatch(recordId: String, userId: String, batchId: String): Int {
        val index = records.indexOfFirst { it.id == recordId && it.userId == userId && it.uploadBatchId == null }
        if (index < 0) return 0
        records[index] = records[index].copy(uploadBatchId = batchId)
        publish()
        return 1
    }

    private fun publish() {
        observed.value = records.map { DietRecordWithUploadState(it, null) }
    }
}

private class FakeUploadQueueDao : UploadQueueDao {
    val rows = mutableListOf<UploadQueueEntity>()

    override suspend fun insert(item: UploadQueueEntity) {
        rows.removeAll { it.id == item.id }
        rows += item
    }

    override suspend fun update(item: UploadQueueEntity) {
        rows.removeAll { it.id == item.id }
        rows += item
    }

    override suspend fun pending(now: Long): List<UploadQueueEntity> = rows
    override fun observeOutstanding(): Flow<List<UploadQueueEntity>> = flowOf(rows)
    override suspend fun pruneDone(before: Long) = Unit
    override suspend fun getById(id: String): UploadQueueEntity? = rows.firstOrNull { it.id == id }
    override suspend fun getPendingByKind(kind: String): List<UploadQueueEntity> = rows.filter { it.kind == kind }
}

private class FakeMeasurementUploadClient : MeasurementUploadClient {
    override val authState: AuthState = AuthState.Authorized

    override suspend fun uploadMeasurements(
        request: TelemetryBatchRequestDto,
    ): ApiResult<TelemetryBatchResponseDto> = ApiResult.NetworkError("unused")
}
