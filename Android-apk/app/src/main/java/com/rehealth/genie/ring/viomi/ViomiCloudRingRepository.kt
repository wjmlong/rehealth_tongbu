package com.rehealth.genie.ring.viomi

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.ViomiBindRequestDto
import com.rehealth.genie.network.dto.ViomiSyncRequestDto
import com.rehealth.genie.ring.RingAcquisitionMode
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.WearableVendor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViomiCloudRingRepository(
    private val dao: RingDataDao,
    private val api: AuthenticatedApiClient,
    private val bindingStore: ActiveWearableBindingStore,
) : RingRepository {
    override val acquisitionMode = RingAcquisitionMode.CLOUD
    override val supportedMetrics = setOf(
        RingMetricType.HEART_RATE,
        RingMetricType.BLOOD_PRESSURE,
        RingMetricType.BLOOD_OXYGEN,
    )
    override val manuallyMeasurableMetrics: Set<RingMetricType> = emptySet()
    private val initial = savedDevice()
    private val state = MutableStateFlow(if (initial == null) RingConnectionState.DISCONNECTED else RingConnectionState.CONNECTED)
    private val device = MutableStateFlow(initial)
    override val connectionState: StateFlow<RingConnectionState> = state
    override val connectedDevice: StateFlow<RingDevice?> = device

    override suspend fun scan(): List<RingDevice> = listOfNotNull(device.value)

    override suspend fun connect(device: RingDevice) {
        state.value = RingConnectionState.CONNECTING
        try {
            val response = api.bindViomi(
                ViomiBindRequestDto(device.address.trim(), bindingStore.activeBinding.value.productCode),
            ).getOrThrow()
            check(response.persisted) { "云米设备绑定未持久化" }
            val bound = RingDevice(device.address.trim(), device.name ?: "云米云端手表", null)
            bindingStore.recordConnectedDevice(WearableVendor.VIOMI_CLOUD, bound)
            this.device.value = bound
            state.value = RingConnectionState.CONNECTED
        } catch (error: Throwable) {
            state.value = RingConnectionState.ERROR
            throw error
        }
    }

    override suspend fun autoConnect(): Boolean {
        val saved = savedDevice() ?: return false
        device.value = saved
        state.value = RingConnectionState.CONNECTED
        return true
    }

    override suspend fun disconnect() {
        device.value = null
        state.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun syncAll(): RingSyncResult = sync(supportedMetrics)

    override suspend fun sync(metrics: Set<RingMetricType>, onProgress: (Int) -> Unit): RingSyncResult {
        val imei = device.value?.address ?: error("请先绑定云米手表")
        val selected = metrics.intersect(supportedMetrics).ifEmpty { supportedMetrics }
        val end = System.currentTimeMillis()
        state.value = RingConnectionState.SYNCING
        onProgress(10)
        return try {
            val response = api.syncViomi(
                ViomiSyncRequestDto(
                    imei = imei,
                    beginAt = end - 7L * 24 * 60 * 60 * 1000,
                    endAt = end,
                    metrics = selected.map { it.name }.toSet(),
                ),
            ).getOrThrow()
            check(response.persisted) { "云米数据尚未在服务端持久化" }
            dao.insertMeasurements(response.measurements.map {
                RingMeasurementEntity(
                    id = it.id,
                    metricType = it.metricType,
                    measuredAt = it.measuredAt,
                    primaryValue = it.primaryValue,
                    secondaryValue = it.secondaryValue,
                    unit = it.unit,
                    source = "viomi_cloud",
                )
            })
            onProgress(100)
            RingSyncResult(selected, response.measurements.size, end, requiresUpload = false)
        } finally {
            state.value = RingConnectionState.CONNECTED
        }
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult = sync(setOf(type))
    override suspend fun sendCommand(data: ByteArray): Boolean = false

    private fun savedDevice(): RingDevice? = bindingStore.activeBinding.value
        .takeIf { it.vendor == WearableVendor.VIOMI_CLOUD && !it.address.isNullOrBlank() }
        ?.let { RingDevice(it.address!!, it.deviceName ?: "云米云端手表", null) }
}

private fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Unauthorized -> error(message)
    is ApiResult.Forbidden -> error(message)
    is ApiResult.InvalidRequest -> error(message)
    is ApiResult.InvalidResponse -> error(message)
    is ApiResult.ServiceUnavailable -> error(message)
    is ApiResult.NetworkError -> error(message)
}
