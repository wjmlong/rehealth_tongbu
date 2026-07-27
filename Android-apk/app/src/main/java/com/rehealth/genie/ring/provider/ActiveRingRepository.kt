package com.rehealth.genie.ring.provider

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
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
) : RingRepository, WearableUserProfileSink {
    private val operationMutex = Mutex()
    override var wearableUserProfile: BaselineHealthProfile? = null
        set(value) {
            field = value
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

    override val supportedMetrics: Set<RingMetricType>
        get() = provider().supportedMetrics

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

    suspend fun switchProduct(profile: WearableProductProfile) = operationMutex.withLock {
        val current = store.activeBinding.value
        if (current.productCode == profile.productCode && current.vendor == profile.vendor) return@withLock
        provider(current.vendor).disconnect()
        store.activateProduct(profile)
        (provider(profile.vendor) as? WearableUserProfileSink)?.wearableUserProfile = wearableUserProfile
    }

    private fun provider(vendor: WearableVendor = store.activeBinding.value.vendor): RingRepository =
        registry.repositoryOrNull(vendor) ?: UnsupportedRingRepository
}

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
