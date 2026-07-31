package com.rehealth.genie.ring.provider

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.BloodGlucoseCalibration
import com.rehealth.genie.ring.MenstrualCycleConfig
import com.rehealth.genie.ring.RingFeatureRepository
import com.rehealth.genie.ring.RingFeatureType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.RingEcgLiveState
import com.rehealth.genie.ring.RingEcgRepository
import com.rehealth.genie.ring.WearableUserProfileSink
import com.rehealth.genie.features.BaselineHealthProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveRingRepository(
    appScope: CoroutineScope,
    private val store: ActiveWearableBindingStore,
    private val registry: RingProviderRegistry,
    initialUserProfile: BaselineHealthProfile? = null,
    private val persistUserProfile: ((BaselineHealthProfile?) -> Unit)? = null,
) : RingRepository, WearableUserProfileSink, RingFeatureRepository, RingEcgRepository {
    private val operationMutex = Mutex()
    override var wearableUserProfile: BaselineHealthProfile? = initialUserProfile
        set(value) {
            field = value
            persistUserProfile?.invoke(value)
            (provider() as? WearableUserProfileSink)?.wearableUserProfile = value
        }

    override val connectionState: StateFlow<RingConnectionState> = store.activeBinding
        .flatMapLatest { binding -> provider(binding.vendor).connectionState }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = provider().connectionState.value,
        )

    override val connectedDevice: StateFlow<RingDevice?> = store.activeBinding
        .flatMapLatest { binding -> provider(binding.vendor).connectedDevice }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = provider().connectedDevice.value,
        )

    override val liveEcg: StateFlow<RingEcgLiveState> = store.activeBinding
        .flatMapLatest { binding ->
            (provider(binding.vendor) as? RingEcgRepository)?.liveEcg ?: unsupportedLiveEcg
        }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = (provider() as? RingEcgRepository)?.liveEcg?.value ?: RingEcgLiveState(),
        )

    override val supportedMetrics: Set<RingMetricType>
        get() = provider().supportedMetrics
    override val manuallyMeasurableMetrics: Set<RingMetricType>
        get() = provider().manuallyMeasurableMetrics

    override val supportedFeatures: Set<RingFeatureType>
        get() = (provider() as? RingFeatureRepository)?.supportedFeatures.orEmpty()

    override suspend fun scan(): List<RingDevice> = operationMutex.withLock { provider().scan() }

    override suspend fun connect(device: RingDevice) = operationMutex.withLock {
        provider().connect(device)
    }

    override suspend fun autoConnect(): Boolean = operationMutex.withLock { provider().autoConnect() }

    override suspend fun disconnect() = operationMutex.withLock { provider().disconnect() }

    override suspend fun syncAll(): RingSyncResult = operationMutex.withLock { provider().syncAll() }

    override suspend fun measure(type: RingMetricType): RingSyncResult = operationMutex.withLock {
        provider().measure(type)
    }

    override suspend fun sendCommand(data: ByteArray): Boolean = operationMutex.withLock {
        provider().sendCommand(data)
    }

    override suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean =
        operationMutex.withLock {
            (provider() as? RingFeatureRepository)?.setBloodGlucoseCalibration(config) ?: false
        }

    override suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean = operationMutex.withLock {
        (provider() as? RingFeatureRepository)?.setMenstrualCycle(config) ?: false
    }

    suspend fun switchProduct(profile: WearableProductProfile) = operationMutex.withLock {
        val current = store.activeBinding.value
        if (current.productCode == profile.productCode && current.vendor == profile.vendor) return@withLock
        provider(current.vendor).disconnect()
        store.activateProduct(profile)
        (provider(profile.vendor) as? WearableUserProfileSink)?.wearableUserProfile = wearableUserProfile
    }

    private fun provider(vendor: WearableVendor = store.activeBinding.value.vendor): RingRepository {
        val resolved = registry.repositoryOrNull(vendor) ?: UnsupportedRingRepository
        (resolved as? WearableUserProfileSink)?.wearableUserProfile = wearableUserProfile
        return resolved
    }
}

private val unsupportedLiveEcg = MutableStateFlow(RingEcgLiveState())

private object UnsupportedRingRepository : RingRepository {
    private val unsupportedState = MutableStateFlow(RingConnectionState.UNSUPPORTED)
    private val noDevice = MutableStateFlow<RingDevice?>(null)

    override val connectionState: StateFlow<RingConnectionState> = unsupportedState
    override val connectedDevice: StateFlow<RingDevice?> = noDevice
    override val supportedMetrics: Set<RingMetricType> = emptySet()

    override suspend fun scan(): List<RingDevice> = emptyList()
    override suspend fun connect(device: RingDevice) = Unit
    override suspend fun autoConnect(): Boolean = false
    override suspend fun disconnect() = Unit
    override suspend fun syncAll(): RingSyncResult = emptySyncResult()
    override suspend fun measure(type: RingMetricType): RingSyncResult = emptySyncResult()
    override suspend fun sendCommand(data: ByteArray): Boolean = false

    private fun emptySyncResult() = RingSyncResult(
        collectedTypes = emptySet(),
        recordsWritten = 0,
        completedAt = System.currentTimeMillis(),
    )
}
