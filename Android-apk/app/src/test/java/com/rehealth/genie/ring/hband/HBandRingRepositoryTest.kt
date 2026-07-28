package com.rehealth.genie.ring.hband

import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.BloodGlucoseCalibration
import com.rehealth.genie.ring.MenstrualCycleConfig
import com.rehealth.genie.ring.RingFeatureType
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
            capabilitiesValue = HBandCapabilities(
                heartRate = true,
                bloodOxygen = true,
                hrv = true,
                bloodPressure = true,
                ecg = true,
                bloodComponent = true,
                bodyComposition = true,
            ),
            payload = HBandPayload(
                measurements = listOf(HBandMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 68.0, "bpm")),
            ),
        )
        val repository = repository(dao, store, gateway).apply {
            wearableUserProfile = BaselineHealthProfile(age = 35, gender = "female", heightCm = 165.0, weightKg = 55.0)
        }

        repository.connect(DEVICE)
        val result = repository.syncAll()

        assertEquals(
            setOf(
                RingMetricType.HEART_RATE,
                RingMetricType.STEPS,
                RingMetricType.ACTIVITY,
                RingMetricType.SLEEP,
                RingMetricType.BLOOD_OXYGEN,
                RingMetricType.HRV,
                RingMetricType.BLOOD_PRESSURE,
                RingMetricType.ECG,
                RingMetricType.BLOOD_COMPONENT,
                RingMetricType.BODY_COMPOSITION,
            ),
            repository.supportedMetrics,
        )
        assertEquals(WearableVendor.HBAND, store.activeBinding.value.vendor)
        assertEquals("hband_wearable", dao.measurements.single().source)
        assertEquals(1, result.recordsWritten)
    }

    @Test
    fun unsupportedMetricNeverReachesSdk() = runTest {
        val gateway = FakeHBandGateway(capabilitiesValue = HBandCapabilities())
        val result = repository(FakeHBandDao(), HBandBindingStore(), gateway).measure(RingMetricType.TEMPERATURE)
        assertEquals(0, gateway.measureCalls)
        assertEquals(0, result.recordsWritten)
    }

    @Test
    fun allSupportedManualMetricsReachSdkMeasurement() = runTest {
        val gateway = FakeHBandGateway(
            capabilitiesValue = HBandCapabilities(
                heartRate = true,
                bloodOxygen = true,
                hrv = true,
                bloodPressure = true,
                ecg = true,
                bloodComponent = true,
                bodyComposition = true,
            ),
        )
        val repository = repository(FakeHBandDao(), HBandBindingStore(), gateway)

        repository.measure(RingMetricType.HEART_RATE)
        repository.measure(RingMetricType.BLOOD_OXYGEN)
        repository.measure(RingMetricType.HRV)
        repository.measure(RingMetricType.BLOOD_PRESSURE)
        repository.measure(RingMetricType.ECG)
        repository.measure(RingMetricType.BLOOD_COMPONENT)
        repository.measure(RingMetricType.BODY_COMPOSITION)

        assertEquals(
            listOf(
                RingMetricType.HEART_RATE,
                RingMetricType.BLOOD_OXYGEN,
                RingMetricType.HRV,
                RingMetricType.BLOOD_PRESSURE,
                RingMetricType.ECG,
                RingMetricType.BLOOD_COMPONENT,
                RingMetricType.BODY_COMPOSITION,
            ),
            gateway.measuredTypes,
        )
    }

    @Test
    fun backgroundSyncReconnectsOnlyBoundDeviceWhenRealProfileIsAvailable() = runTest {
        val store = HBandBindingStore().apply {
            recordConnectedDevice(WearableVendor.HBAND, DEVICE)
        }
        val gateway = FakeHBandGateway(capabilitiesValue = HBandCapabilities(heartRate = true))
        val repository = repository(FakeHBandDao(), store, gateway).apply {
            wearableUserProfile = BaselineHealthProfile(
                age = 35,
                gender = "female",
                heightCm = 165.0,
                weightKg = 55.0,
            )
        }

        repository.syncAll()

        assertEquals(1, gateway.connectCalls)
        assertEquals(DEVICE.address, gateway.lastConnectedAddress)
        assertEquals(1, gateway.syncCalls)
    }

    @Test
    fun backgroundSyncWithoutBindingDoesNotScanOrCallSdk() = runTest {
        val gateway = FakeHBandGateway()
        val repository = repository(FakeHBandDao(), HBandBindingStore(), gateway).apply {
            wearableUserProfile = BaselineHealthProfile(
                age = 35,
                gender = "female",
                heightCm = 165.0,
                weightKg = 55.0,
            )
        }

        val result = repository.syncAll()

        assertEquals(0, gateway.connectCalls)
        assertEquals(0, gateway.syncCalls)
        assertEquals(0, result.recordsWritten)
    }

    @Test
    fun featureSettingsAreCapabilityGatedBeforeReachingSdk() = runTest {
        val gateway = FakeHBandGateway(
            capabilitiesValue = HBandCapabilities(
                bloodGlucoseCalibration = true,
                womensHealth = true,
            ),
        )
        val repository = repository(FakeHBandDao(), HBandBindingStore(), gateway)

        assertEquals(
            setOf(RingFeatureType.BLOOD_GLUCOSE_CALIBRATION, RingFeatureType.WOMENS_HEALTH),
            repository.supportedFeatures,
        )
        assertTrue(repository.setBloodGlucoseCalibration(BloodGlucoseCalibration(true, 5.6)))
        assertTrue(repository.setMenstrualCycle(MenstrualCycleConfig(5, 28, 1_700_000_000_000L)))
        assertEquals(1, gateway.bloodGlucoseSettingCalls)
        assertEquals(1, gateway.menstrualSettingCalls)

        val unsupportedGateway = FakeHBandGateway()
        val unsupportedRepository = repository(FakeHBandDao(), HBandBindingStore(), unsupportedGateway)
        assertTrue(!unsupportedRepository.setBloodGlucoseCalibration(BloodGlucoseCalibration(true, 5.6)))
        assertEquals(0, unsupportedGateway.bloodGlucoseSettingCalls)
    }

    private fun repository(dao: RingDataDao, store: ActiveWearableBindingStore, gateway: HBandSdkGateway) =
        HBandRingRepository(
            dao,
            store,
            gateway,
            emptySet(),
            setOf(
                RingMetricType.HEART_RATE,
                RingMetricType.STEPS,
                RingMetricType.ACTIVITY,
                RingMetricType.SLEEP,
                RingMetricType.BLOOD_OXYGEN,
                RingMetricType.HRV,
                RingMetricType.BLOOD_PRESSURE,
                RingMetricType.ECG,
                RingMetricType.BLOOD_COMPONENT,
                RingMetricType.BODY_COMPOSITION,
            ),
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
    val measuredTypes = mutableListOf<RingMetricType>()
    var syncCalls = 0
    var lastConnectedAddress: String? = null
    var bloodGlucoseSettingCalls = 0
    var menstrualSettingCalls = 0
    override val connectionState: StateFlow<RingConnectionState> = state
    override val connectedDevice: StateFlow<RingDevice?> = device
    override val capabilities: StateFlow<HBandCapabilities> = capabilityState
    override suspend fun scan() = emptyList<RingDevice>()
    override suspend fun connect(device: RingDevice, profile: HBandUserProfile): HBandConnectionInfo {
        connectCalls++
        lastConnectedAddress = device.address
        this.device.value = device
        state.value = RingConnectionState.CONNECTED
        return HBandConnectionInfo(device, "device:1", "1.0", capabilityState.value)
    }
    override suspend fun disconnect() { device.value = null; state.value = RingConnectionState.DISCONNECTED }
    override suspend fun sync(metrics: Set<RingMetricType>): HBandPayload {
        syncCalls++
        return payload
    }
    override suspend fun measure(type: RingMetricType): HBandPayload {
        measureCalls++
        measuredTypes += type
        return payload
    }
    override suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean {
        bloodGlucoseSettingCalls++
        return true
    }
    override suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean {
        menstrualSettingCalls++
        return true
    }
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
