package com.rehealth.genie.ring.rwfit

import com.google.gson.Gson
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.WearableVendor
import kotlinx.coroutines.flow.StateFlow

class RwFitRingRepository internal constructor(
    private val dao: RingDataDao,
    private val activeWearableStore: ActiveWearableBindingStore,
    private val gateway: RwFitSdkGateway,
    private val modelNameHints: Set<String>,
) : RingRepository {
    override val connectionState: StateFlow<RingConnectionState> = gateway.connectionState
    override val connectedDevice: StateFlow<RingDevice?> = gateway.connectedDevice
    override val supportedMetrics: Set<RingMetricType>
        get() = gateway.capabilities.value.supportedMetrics

    override suspend fun scan(): List<RingDevice> {
        val boundAddress = activeBindingAddress()
        return gateway.scan().sortedWith(
            compareByDescending<RingDevice> { device ->
                device.address.equals(boundAddress, ignoreCase = true)
            }.thenByDescending { device ->
                modelNameHints.any { hint -> device.name.orEmpty().contains(hint, ignoreCase = true) }
            }.thenByDescending { device -> device.rssi ?: Int.MIN_VALUE },
        )
    }

    override suspend fun connect(device: RingDevice) {
        val info = gateway.connect(device) ?: error("RWFit 设备连接失败")
        activeWearableStore.recordConnectedDevice(
            vendor = WearableVendor.RWFIT,
            device = info.device,
            modelCode = info.modelCode,
            firmwareVersion = info.firmwareVersion,
            capabilityJson = Gson().toJson(info.capabilities),
        )
    }

    override suspend fun autoConnect(): Boolean {
        if (connectionState.value == RingConnectionState.CONNECTED) return true
        val binding = activeWearableStore.activeBinding.value
        if (binding.vendor != WearableVendor.RWFIT || binding.address.isNullOrBlank()) return false
        runCatching { connect(RingDevice(binding.address, binding.deviceName, null)) }
            .getOrElse { return false }
        return connectionState.value == RingConnectionState.CONNECTED
    }

    override suspend fun disconnect() = gateway.disconnect()

    override suspend fun syncAll(): RingSyncResult = persist(gateway.syncSupported())

    override suspend fun measure(type: RingMetricType): RingSyncResult {
        if (type !in supportedMetrics || type !in MANUAL_METRICS) return emptyResult()
        return persist(gateway.measure(type))
    }

    /** RWFit commands are intentionally exposed only through typed gateway operations. */
    override suspend fun sendCommand(data: ByteArray): Boolean = false

    private suspend fun persist(payload: RwFitPayload): RingSyncResult {
        val deviceKey = connectedDevice.value?.address ?: activeBindingAddress()
        if (deviceKey.isNullOrBlank()) return emptyResult()
        val batch = RwFitDataMapper.toEntities(payload, deviceKey)
        if (batch.size > 0) dao.insertBatch(batch)
        return result(batch)
    }

    private fun result(batch: RingDataBatch) = RingSyncResult(
        collectedTypes = RwFitDataMapper.collectedTypes(batch),
        recordsWritten = batch.size,
        completedAt = System.currentTimeMillis(),
    )

    private fun emptyResult() = RingSyncResult(emptySet(), 0, System.currentTimeMillis())

    private fun activeBindingAddress(): String? = activeWearableStore.activeBinding.value
        .takeIf { binding -> binding.vendor == WearableVendor.RWFIT }
        ?.address

    private companion object {
        val MANUAL_METRICS = setOf(
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_OXYGEN,
            RingMetricType.HRV,
        )
    }
}
