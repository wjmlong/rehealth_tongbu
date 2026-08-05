package com.rehealth.genie.ring.provider

import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.WearableUserProfileSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertFalse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest

class ActiveRingRepositoryTest {
    @Test
    fun forcedDebugProductKeepsBindingAfterThatProductConnected() {
        val default = profile("RH-RW-P01", WearableVendor.RWFIT).toBinding()
        val connected = default.copy(address = "AA:BB", deviceName = "RW Ring", boundAt = 10L)
        val oldMrd = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD).toBinding()

        assertEquals(connected, resolveInitialWearableBinding(default, connected, true))
        assertEquals(default, resolveInitialWearableBinding(default, oldMrd, true))
    }

    @Test
    fun releaseMigratesLegacyProviderSelectionToHBandDefault() {
        val hband = profile(HBAND_PRODUCT_CODE, WearableVendor.HBAND).toBinding()
        val oldMrd = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD).toBinding()
        val allowed = setOf(WearableVendor.HBAND, WearableVendor.VIOMI_CLOUD)

        assertEquals(
            hband,
            resolveInitialWearableBinding(hband, oldMrd, forceDefaultSelection = false, allowedVendors = allowed),
        )
    }

    @Test
    fun releaseKeepsExistingViomiBinding() {
        val hband = profile(HBAND_PRODUCT_CODE, WearableVendor.HBAND).toBinding()
        val viomi = profile("RH-VM-K9L", WearableVendor.VIOMI_CLOUD).toBinding()
            .copy(address = "masked-imei", boundAt = 10L)
        val allowed = setOf(WearableVendor.HBAND, WearableVendor.VIOMI_CLOUD)

        assertEquals(
            viomi,
            resolveInitialWearableBinding(hband, viomi, forceDefaultSelection = false, allowedVendors = allowed),
        )
    }

    @Test
    fun registryCreatesOnlyRequestedProviderAndCachesIt() {
        var mrdCreations = 0
        var mockCreations = 0
        val mrd = FakeRingRepository("MRD")
        val registry = RingProviderRegistry(
            mapOf(
                WearableVendor.MRD to { mrdCreations += 1; mrd },
                WearableVendor.MOCK to { mockCreations += 1; FakeRingRepository("MOCK") },
            ),
        )

        assertEquals(emptySet(), registry.initializedVendors())
        assertSame(mrd, registry.requireRepository(WearableVendor.MRD))
        assertSame(mrd, registry.requireRepository(WearableVendor.MRD))
        assertEquals(1, mrdCreations)
        assertEquals(0, mockCreations)
        assertEquals(setOf(WearableVendor.MRD), registry.initializedVendors())
    }

    @Test
    fun productSwitchDisconnectsOldProviderAndRoutesFutureCalls() = runTest {
        val mrdProfile = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD)
        val mockProfile = profile(DEBUG_MOCK_PRODUCT_CODE, WearableVendor.MOCK)
        val store = FakeBindingStore(mrdProfile)
        val mrd = FakeRingRepository("MRD")
        val mock = FakeRingRepository("MOCK")
        val registry = RingProviderRegistry(
            mapOf(
                WearableVendor.MRD to { mrd },
                WearableVendor.MOCK to { mock },
            ),
        )
        val routed = ActiveRingRepository(backgroundScope, store, registry)
        val manager = ActiveWearableManager(store, listOf(mrdProfile, mockProfile), registry, routed)

        assertEquals("MRD", routed.scan().single().name)
        manager.switchProduct(DEBUG_MOCK_PRODUCT_CODE)
        assertEquals(1, mrd.disconnectCalls)
        assertEquals(WearableVendor.MOCK, store.activeBinding.value.vendor)
        assertEquals("MOCK", routed.scan().single().name)
    }

    @Test
    fun unsupportedVendorCannotBecomeActive() = runTest {
        val mrdProfile = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD)
        val rwfitProfile = profile("RH-RW-P01", WearableVendor.RWFIT)
        val store = FakeBindingStore(mrdProfile)
        val registry = RingProviderRegistry(
            mapOf(WearableVendor.MRD to { FakeRingRepository("MRD") }),
        )
        val routed = ActiveRingRepository(backgroundScope, store, registry)
        val manager = ActiveWearableManager(store, listOf(mrdProfile, rwfitProfile), registry, routed)

        assertFailsWith<IllegalArgumentException> {
            manager.switchProduct(rwfitProfile.productCode)
        }
        assertEquals(WearableVendor.MRD, store.activeBinding.value.vendor)
    }

    @Test
    fun realUserProfileFollowsTheSingleActiveProvider() = runTest {
        val mrdProfile = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD)
        val hbandProfile = profile(HBAND_PRODUCT_CODE, WearableVendor.HBAND)
        val store = FakeBindingStore(mrdProfile)
        val mrd = FakeRingRepository("MRD")
        val hband = FakeRingRepository("HBAND")
        val registry = RingProviderRegistry(
            mapOf(WearableVendor.MRD to { mrd }, WearableVendor.HBAND to { hband }),
        )
        val routed = ActiveRingRepository(backgroundScope, store, registry)
        val profile = BaselineHealthProfile(age = 30, gender = "male", heightCm = 175.0, weightKg = 70.0)

        routed.wearableUserProfile = profile
        routed.switchProduct(hbandProfile)

        assertEquals(profile, hband.wearableUserProfile)
    }

    @Test
    fun foregroundAndBackgroundOperationsShareOneMutex() = runTest {
        val mrdProfile = profile(DEFAULT_MRD_PRODUCT_CODE, WearableVendor.MRD)
        val blocking = BlockingRingRepository()
        val routed = ActiveRingRepository(
            backgroundScope,
            FakeBindingStore(mrdProfile),
            RingProviderRegistry(mapOf(WearableVendor.MRD to { blocking })),
        )

        val sync = async { routed.syncAll() }
        blocking.syncStarted.await()
        val measurement = async { routed.measure(RingMetricType.HEART_RATE) }
        testScheduler.runCurrent()
        assertFalse(blocking.measureStarted.isCompleted)

        blocking.allowSyncToFinish.complete(Unit)
        sync.await()
        measurement.await()
        assertEquals(1, blocking.maxConcurrentOperations)
    }

    @Test
    fun restoredProfileReachesLazyProviderAndUpdatesArePersisted() = runTest {
        val hbandProfile = profile(HBAND_PRODUCT_CODE, WearableVendor.HBAND)
        val restored = BaselineHealthProfile(age = 40, gender = "male", heightCm = 178.0, weightKg = 75.0)
        val updated = restored.copy(weightKg = 74.0)
        var persisted: BaselineHealthProfile? = null
        val hband = FakeRingRepository("HBAND")
        val routed = ActiveRingRepository(
            backgroundScope,
            FakeBindingStore(hbandProfile),
            RingProviderRegistry(mapOf(WearableVendor.HBAND to { hband })),
            initialUserProfile = restored,
            persistUserProfile = { persisted = it },
        )

        routed.scan()
        assertEquals(restored, hband.wearableUserProfile)
        routed.wearableUserProfile = updated
        assertEquals(updated, persisted)
        assertEquals(updated, hband.wearableUserProfile)
    }

    private fun profile(productCode: String, vendor: WearableVendor) = WearableProductProfile(
        productCode = productCode,
        vendor = vendor,
        displayName = vendor.name,
        modelNameHints = emptySet(),
        expectedMetrics = setOf(RingMetricType.HEART_RATE),
    )
}

private class BlockingRingRepository : RingRepository {
    private val state = MutableStateFlow(RingConnectionState.CONNECTED)
    private val device = MutableStateFlow<RingDevice?>(RingDevice("MRD", "MRD", null))
    val syncStarted = CompletableDeferred<Unit>()
    val measureStarted = CompletableDeferred<Unit>()
    val allowSyncToFinish = CompletableDeferred<Unit>()
    private var activeOperations = 0
    var maxConcurrentOperations = 0
        private set
    override val connectionState: StateFlow<RingConnectionState> = state
    override val connectedDevice: StateFlow<RingDevice?> = device
    override val supportedMetrics: Set<RingMetricType> = setOf(RingMetricType.HEART_RATE)
    override suspend fun scan() = emptyList<RingDevice>()
    override suspend fun connect(device: RingDevice) = Unit
    override suspend fun autoConnect() = true
    override suspend fun disconnect() = Unit
    override suspend fun syncAll(): RingSyncResult = track {
        syncStarted.complete(Unit)
        allowSyncToFinish.await()
        emptyResult()
    }
    override suspend fun measure(type: RingMetricType): RingSyncResult = track {
        measureStarted.complete(Unit)
        emptyResult()
    }
    override suspend fun sendCommand(data: ByteArray) = false

    private suspend fun <T> track(operation: suspend () -> T): T {
        activeOperations += 1
        maxConcurrentOperations = maxOf(maxConcurrentOperations, activeOperations)
        return try { operation() } finally { activeOperations -= 1 }
    }

    private fun emptyResult() = RingSyncResult(emptySet(), 0, 1L)
}

private class FakeBindingStore(profile: WearableProductProfile) : ActiveWearableBindingStore {
    private val mutableBinding = MutableStateFlow(profile.toBinding())
    override val activeBinding: StateFlow<ActiveWearableBinding> = mutableBinding.asStateFlow()

    override fun activateProduct(profile: WearableProductProfile, changedAt: Long) {
        mutableBinding.value = profile.toBinding(changedAt)
    }

    override fun recordConnectedDevice(
        vendor: WearableVendor,
        device: RingDevice,
        modelCode: String?,
        firmwareVersion: String?,
        capabilityJson: String?,
        changedAt: Long,
    ) {
        if (mutableBinding.value.vendor != vendor) return
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

private fun WearableProductProfile.toBinding(changedAt: Long = 0L) = ActiveWearableBinding(
    productCode = productCode,
    vendor = vendor,
    address = null,
    deviceName = null,
    modelCode = null,
    firmwareVersion = null,
    capabilityJson = null,
    boundAt = 0L,
    lastDeviceChangedAt = changedAt,
)

private class FakeRingRepository(private val label: String) : RingRepository, WearableUserProfileSink {
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    var disconnectCalls = 0
    override var wearableUserProfile: BaselineHealthProfile? = null

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = setOf(RingMetricType.HEART_RATE)

    override suspend fun scan(): List<RingDevice> = listOf(RingDevice(label, label, null))
    override suspend fun connect(device: RingDevice) {
        mutableConnectedDevice.value = device
        mutableConnectionState.value = RingConnectionState.CONNECTED
    }
    override suspend fun autoConnect(): Boolean = false
    override suspend fun disconnect() {
        disconnectCalls += 1
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }
    override suspend fun syncAll(): RingSyncResult = emptyResult()
    override suspend fun measure(type: RingMetricType): RingSyncResult = emptyResult()
    override suspend fun sendCommand(data: ByteArray): Boolean = false

    private fun emptyResult() = RingSyncResult(emptySet(), 0, 1L)
}
