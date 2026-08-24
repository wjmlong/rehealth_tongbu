package com.rehealth.genie.ring.rwfit

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import com.rehealth.genie.ring.provider.ActiveWearableBinding
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.RWFIT_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableProductProfile
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class RwFitRingRepositoryTest {
    @Test
    fun connectPersistsOnlyDomainMetadataAndSyncWritesExistingTables() = runTest {
        val dao = TrackingRingDataDao()
        val store = TrackingBindingStore()
        val gateway = FakeRwFitGateway(
            RwFitCapabilities(heartRate = true),
            RwFitPayload(
                measurements = listOf(
                    RwFitMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 70.0, "bpm"),
                ),
            ),
        )
        val repository = RwFitRingRepository(dao, store, gateway, setOf("RW")) { "user-1" }
        val device = RingDevice("AA:BB:CC:DD:EE:FF", "RW Ring", -45)

        repository.connect(device)
        val result = repository.syncAll()

        assertEquals(WearableVendor.RWFIT, store.activeBinding.value.vendor)
        assertEquals(device.address, store.activeBinding.value.address)
        assertTrue(store.activeBinding.value.capabilityJson.orEmpty().contains("heartRate"))
        assertEquals(1, dao.measurements.size)
        assertEquals("rwfit", dao.measurements.single().source)
        assertEquals("user-1", dao.measurements.single().ownerUserId)
        assertEquals(device.address, dao.measurements.single().deviceId)
        assertEquals(setOf(RingMetricType.HEART_RATE), result.collectedTypes)
    }

    @Test
    fun unsupportedManualMetricDoesNotReachSdkOrCreateZeroRecord() = runTest {
        val dao = TrackingRingDataDao()
        val gateway = FakeRwFitGateway(RwFitCapabilities(heartRate = true), RwFitPayload())
        val repository = RwFitRingRepository(dao, TrackingBindingStore(), gateway, emptySet())

        val result = repository.measure(RingMetricType.BLOOD_OXYGEN)

        assertEquals(0, gateway.measureCalls)
        assertEquals(0, result.recordsWritten)
        assertTrue(dao.measurements.isEmpty())
    }

    @Test
    fun scanRanksBoundDeviceWithoutFilteringOtherSdkCandidates() = runTest {
        val store = TrackingBindingStore().apply {
            recordConnectedDevice(
                WearableVendor.RWFIT,
                RingDevice("BOUND", "Old", null),
            )
        }
        val gateway = FakeRwFitGateway(RwFitCapabilities(), RwFitPayload()).apply {
            scanResults = listOf(
                RingDevice("OTHER", "RW New", -30),
                RingDevice("BOUND", "Saved", -90),
                RingDevice("THIRD", "Compatible", -20),
            )
        }
        val repository = RwFitRingRepository(TrackingRingDataDao(), store, gateway, setOf("RW"))

        val results = repository.scan()

        assertEquals("BOUND", results.first().address)
        assertEquals(3, results.size)
    }

    @Test
    fun backgroundSyncReconnectsOnlyTheBoundDevice() = runTest {
        val store = TrackingBindingStore().apply {
            recordConnectedDevice(WearableVendor.RWFIT, RingDevice("BOUND", "RW Ring", null))
        }
        val gateway = FakeRwFitGateway(
            RwFitCapabilities(heartRate = true),
            RwFitPayload(measurements = listOf(RwFitMetricSample(RingMetricType.HEART_RATE, 1000, 65.0, "bpm"))),
        )
        val repository = RwFitRingRepository(TrackingRingDataDao(), store, gateway, emptySet())

        val result = repository.syncAll()

        assertEquals(1, gateway.connectCalls)
        assertEquals("BOUND", gateway.lastConnectedAddress)
        assertEquals(1, result.recordsWritten)
    }

    @Test
    fun backgroundSyncWithoutBindingDoesNotScanOrCallSdk() = runTest {
        val gateway = FakeRwFitGateway(RwFitCapabilities(), RwFitPayload())
        val result = RwFitRingRepository(
            TrackingRingDataDao(),
            TrackingBindingStore(),
            gateway,
            emptySet(),
        ).syncAll()

        assertEquals(0, gateway.connectCalls)
        assertEquals(0, gateway.syncCalls)
        assertEquals(0, result.recordsWritten)
    }
}

private class FakeRwFitGateway(
    initialCapabilities: RwFitCapabilities,
    private val syncPayload: RwFitPayload,
) : RwFitSdkGateway {
    private val mutableState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableDevice = MutableStateFlow<RingDevice?>(null)
    private val mutableCapabilities = MutableStateFlow(initialCapabilities)
    var measureCalls = 0
    var connectCalls = 0
    var syncCalls = 0
    var lastConnectedAddress: String? = null
    var scanResults: List<RingDevice> = emptyList()

    override val connectionState: StateFlow<RingConnectionState> = mutableState
    override val connectedDevice: StateFlow<RingDevice?> = mutableDevice
    override val capabilities: StateFlow<RwFitCapabilities> = mutableCapabilities

    override suspend fun scan(): List<RingDevice> = scanResults

    override suspend fun connect(device: RingDevice): RwFitConnectionInfo {
        connectCalls += 1
        lastConnectedAddress = device.address
        mutableDevice.value = device
        mutableState.value = RingConnectionState.CONNECTED
        return RwFitConnectionInfo(device, "pid:7", "2.0.0", mutableCapabilities.value)
    }

    override suspend fun disconnect() {
        mutableDevice.value = null
        mutableState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun syncSupported(): RwFitPayload {
        syncCalls += 1
        return syncPayload
    }

    override suspend fun measure(type: RingMetricType): RwFitPayload {
        measureCalls += 1
        return RwFitPayload()
    }
}

private class TrackingBindingStore : ActiveWearableBindingStore {
    private val mutableBinding = MutableStateFlow(
        ActiveWearableBinding(
            productCode = RWFIT_PRODUCT_CODE,
            vendor = WearableVendor.RWFIT,
            address = null,
            deviceName = null,
            modelCode = null,
            firmwareVersion = null,
            capabilityJson = null,
            boundAt = 0,
            lastDeviceChangedAt = 0,
        ),
    )
    override val activeBinding: StateFlow<ActiveWearableBinding> = mutableBinding

    override fun boundToCurrentUser(): Boolean = true

    override fun activateProduct(profile: WearableProductProfile, changedAt: Long) {
        mutableBinding.value = mutableBinding.value.copy(productCode = profile.productCode, vendor = profile.vendor)
    }

    override fun recordConnectedDevice(
        vendor: WearableVendor,
        device: RingDevice,
        modelCode: String?,
        firmwareVersion: String?,
        capabilityJson: String?,
        changedAt: Long,
    ) {
        assertEquals(WearableVendor.RWFIT, vendor)
        mutableBinding.value = mutableBinding.value.copy(
            address = device.address,
            deviceName = device.name,
            modelCode = modelCode,
            firmwareVersion = firmwareVersion,
            capabilityJson = capabilityJson,
            boundAt = changedAt,
            lastDeviceChangedAt = changedAt,
        )
    }
}

private class TrackingRingDataDao : RingDataDao {
    val measurements = mutableListOf<RingMeasurementEntity>()
    val sleep = mutableListOf<RingSleepSessionEntity>()
    val activities = mutableListOf<RingActivityEntity>()
    val signals = mutableListOf<RingSignalChunkEntity>()

    override suspend fun insertMeasurements(records: List<RingMeasurementEntity>) {
        measurements += records
    }

    override suspend fun insertSleepSessions(records: List<RingSleepSessionEntity>) {
        sleep += records
    }

    override suspend fun insertActivities(records: List<RingActivityEntity>) {
        activities += records
    }

    override suspend fun insertSignalChunks(records: List<RingSignalChunkEntity>) {
        signals += records
    }

    override fun observeMeasurements(metricType: String, limit: Int): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeMeasurementsForOwner(ownerUserId: String, metricType: String, limit: Int): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeSleepSessions(limit: Int): Flow<List<RingSleepSessionEntity>> = emptyFlow()
    override fun observeActivities(limit: Int): Flow<List<RingActivityEntity>> = emptyFlow()
    override fun observeSignalChunks(signalType: String, limit: Int): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override fun observeLatestMeasurements(): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeLatestMeasurementsForOwner(ownerUserId: String): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeLatestMeasurementsForBinding(ownerUserId: String, deviceId: String, source: String): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeLatestSleepSession(): Flow<RingSleepSessionEntity?> = emptyFlow()
    override fun observeLatestSleepSessionForOwner(ownerUserId: String): Flow<RingSleepSessionEntity?> = emptyFlow()
    override fun observeLatestActivity(): Flow<RingActivityEntity?> = emptyFlow()
    override fun observeActivitiesForOwner(ownerUserId: String, limit: Int): Flow<List<RingActivityEntity>> = emptyFlow()
    override fun observeLatestSignalChunks(): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override fun observeLatestSignalChunksForOwner(ownerUserId: String): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override fun observeSignalChunksForOwner(ownerUserId: String, signalType: String, limit: Int): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override suspend fun getMeasurementsSince(since: Long): List<RingMeasurementEntity> = emptyList()
    override suspend fun getMeasurementsSinceForOwner(since: Long, ownerUserId: String): List<RingMeasurementEntity> = measurements.filter { it.ownerUserId == ownerUserId && it.measuredAt >= since }
    override suspend fun getMeasurementsSinceForBinding(since: Long, ownerUserId: String, deviceId: String, source: String): List<RingMeasurementEntity> = emptyList()
    override suspend fun getLatestMeasuredAtForBinding(ownerUserId: String, deviceId: String, source: String): Long? = null
    override suspend fun getLatestMeasurement(metricType: String): RingMeasurementEntity? = null
    override suspend fun getActivitiesSince(since: Long): List<RingActivityEntity> = emptyList()
    override suspend fun getActivitiesSinceForOwner(since: Long, ownerUserId: String): List<RingActivityEntity> = activities.filter { it.ownerUserId == ownerUserId && it.startedAt >= since }
    override suspend fun getSleepSessionsSince(since: Long): List<RingSleepSessionEntity> = emptyList()
    override suspend fun getSleepSessionsSinceForOwner(since: Long, ownerUserId: String): List<RingSleepSessionEntity> = sleep.filter { it.ownerUserId == ownerUserId && it.endedAt >= since }
    override suspend fun deleteMeasurementsBySource(source: String) { measurements.removeAll { it.source == source } }
    override suspend fun deleteSleepSessionsBySource(source: String) { sleep.removeAll { it.source == source } }
    override suspend fun deleteActivitiesBySource(source: String) { activities.removeAll { it.source == source } }
    override suspend fun deleteSignalChunksBySource(source: String) { signals.removeAll { it.source == source } }
}
