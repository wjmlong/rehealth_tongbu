package com.rehealth.genie.ring.hband

import com.rehealth.genie.features.BaselineHealthProfile
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
import com.rehealth.genie.ring.provider.HBAND_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableProductProfile
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class HBandRingRepositoryTest {
    @Test
    fun requiresRealProfileBeforeSdkConnection() = runTest {
        val gateway = FakeHBandGateway()
        val repository = repository(FakeHBandDao(), HBandBindingStore(), gateway)
        assertFailsWith<IllegalStateException> { repository.connect(DEVICE) }
        assertEquals(0, gateway.connectCalls)
    }

    @Test
    fun intersectsProductAndDeviceCapabilitiesThenPersistsExistingRoomEntities() = runTest {
        val dao = FakeHBandDao()
        val store = HBandBindingStore()
        val gateway = FakeHBandGateway(
            capabilitiesValue = HBandCapabilities(heartRate = true, bloodOxygen = true),
            payload = HBandPayload(
                measurements = listOf(HBandMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 68.0, "bpm")),
            ),
        )
        val repository = repository(dao, store, gateway).apply {
            wearableUserProfile = BaselineHealthProfile(age = 35, gender = "female", heightCm = 165.0, weightKg = 55.0)
        }

        repository.connect(DEVICE)
        val result = repository.syncAll()

        assertEquals(setOf(RingMetricType.HEART_RATE, RingMetricType.STEPS, RingMetricType.ACTIVITY, RingMetricType.SLEEP), repository.supportedMetrics)
        assertTrue(RingMetricType.BLOOD_OXYGEN !in repository.supportedMetrics)
        assertEquals(WearableVendor.HBAND, store.activeBinding.value.vendor)
        assertEquals("hband_wearable", dao.measurements.single().source)
        assertEquals(1, result.recordsWritten)
    }

    @Test
    fun unsupportedMetricNeverReachesSdk() = runTest {
        val gateway = FakeHBandGateway(capabilitiesValue = HBandCapabilities(bloodOxygen = true))
        val result = repository(FakeHBandDao(), HBandBindingStore(), gateway).measure(RingMetricType.BLOOD_OXYGEN)
        assertEquals(0, gateway.measureCalls)
        assertEquals(0, result.recordsWritten)
    }

    private fun repository(dao: RingDataDao, store: ActiveWearableBindingStore, gateway: HBandSdkGateway) =
        HBandRingRepository(
            dao,
            store,
            gateway,
            emptySet(),
            setOf(RingMetricType.HEART_RATE, RingMetricType.STEPS, RingMetricType.ACTIVITY, RingMetricType.SLEEP),
        )

    private companion object {
        val DEVICE = RingDevice("AA:BB:CC:DD:EE:FF", "HBand", -40)
    }
}

private class FakeHBandGateway(
    capabilitiesValue: HBandCapabilities = HBandCapabilities(),
    private val payload: HBandPayload = HBandPayload(),
) : HBandSdkGateway {
    private val state = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val device = MutableStateFlow<RingDevice?>(null)
    private val capabilityState = MutableStateFlow(capabilitiesValue)
    var connectCalls = 0
    var measureCalls = 0
    override val connectionState: StateFlow<RingConnectionState> = state
    override val connectedDevice: StateFlow<RingDevice?> = device
    override val capabilities: StateFlow<HBandCapabilities> = capabilityState
    override suspend fun scan() = emptyList<RingDevice>()
    override suspend fun connect(device: RingDevice, profile: HBandUserProfile): HBandConnectionInfo {
        connectCalls++
        this.device.value = device
        state.value = RingConnectionState.CONNECTED
        return HBandConnectionInfo(device, "device:1", "1.0", capabilityState.value)
    }
    override suspend fun disconnect() { device.value = null; state.value = RingConnectionState.DISCONNECTED }
    override suspend fun sync(metrics: Set<RingMetricType>) = payload
    override suspend fun measure(type: RingMetricType): HBandPayload { measureCalls++; return payload }
}

private class HBandBindingStore : ActiveWearableBindingStore {
    private val binding = MutableStateFlow(ActiveWearableBinding(HBAND_PRODUCT_CODE, WearableVendor.HBAND, null, null, null, null, null, 0, 0))
    override val activeBinding: StateFlow<ActiveWearableBinding> = binding
    override fun activateProduct(profile: WearableProductProfile, changedAt: Long) {
        binding.value = binding.value.copy(productCode = profile.productCode, vendor = profile.vendor)
    }
    override fun recordConnectedDevice(vendor: WearableVendor, device: RingDevice, modelCode: String?, firmwareVersion: String?, capabilityJson: String?, changedAt: Long) {
        binding.value = binding.value.copy(vendor = vendor, address = device.address, deviceName = device.name, modelCode = modelCode, firmwareVersion = firmwareVersion, capabilityJson = capabilityJson, boundAt = changedAt, lastDeviceChangedAt = changedAt)
    }
}

private class FakeHBandDao : RingDataDao {
    val measurements = mutableListOf<RingMeasurementEntity>()
    override suspend fun insertMeasurements(records: List<RingMeasurementEntity>) { measurements += records }
    override suspend fun insertSleepSessions(records: List<RingSleepSessionEntity>) = Unit
    override suspend fun insertActivities(records: List<RingActivityEntity>) = Unit
    override suspend fun insertSignalChunks(records: List<RingSignalChunkEntity>) = Unit
    override fun observeMeasurements(metricType: String, limit: Int): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeSleepSessions(limit: Int): Flow<List<RingSleepSessionEntity>> = emptyFlow()
    override fun observeActivities(limit: Int): Flow<List<RingActivityEntity>> = emptyFlow()
    override fun observeSignalChunks(signalType: String, limit: Int): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override fun observeLatestMeasurements(): Flow<List<RingMeasurementEntity>> = emptyFlow()
    override fun observeLatestSleepSession(): Flow<RingSleepSessionEntity?> = emptyFlow()
    override fun observeLatestActivity(): Flow<RingActivityEntity?> = emptyFlow()
    override fun observeLatestSignalChunks(): Flow<List<RingSignalChunkEntity>> = emptyFlow()
    override suspend fun getMeasurementsSince(since: Long) = emptyList<RingMeasurementEntity>()
    override suspend fun getLatestMeasurement(metricType: String): RingMeasurementEntity? = null
    override suspend fun getActivitiesSince(since: Long) = emptyList<RingActivityEntity>()
    override suspend fun getSleepSessionsSince(since: Long) = emptyList<RingSleepSessionEntity>()
}
