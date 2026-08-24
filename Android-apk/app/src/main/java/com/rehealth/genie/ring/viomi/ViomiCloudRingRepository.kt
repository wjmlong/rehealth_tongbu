package com.rehealth.genie.ring.viomi

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.ViomiBindRequestDto
import com.rehealth.genie.network.dto.ViomiSyncRequestDto
import com.rehealth.genie.network.dto.ViomiMeasurementPlanRequestDto
import com.rehealth.genie.ring.RingAcquisitionMode
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.RingActiveMeasurementPlanRepository
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.WearableVendor
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ViomiCloudRingRepository(
    private val dao: RingDataDao,
    private val api: AuthenticatedApiClient,
    private val bindingStore: ActiveWearableBindingStore,
    private val userIdProvider: () -> String?,
) : RingRepository, RingActiveMeasurementPlanRepository {
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
            bindingStore.recordConnectedDevice(
                vendor = WearableVendor.VIOMI_CLOUD,
                device = bound,
                modelCode = response.deviceId,
            )
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
        val ownerUserId = userIdProvider()?.takeIf(String::isNotBlank) ?: error("请先登录后同步云米数据")
        val deviceId = bindingStore.activeBinding.value.modelCode
            ?: viomiDeviceId(imei)
        val latestMeasuredAt = dao.getLatestMeasuredAtForBinding(ownerUserId, deviceId, VIOMI_SOURCE)
        val earliestAllowed = end - MAX_SYNC_WINDOW_MILLIS
        val begin = latestMeasuredAt
            ?.minus(SYNC_OVERLAP_MILLIS)
            ?.coerceAtLeast(earliestAllowed)
            ?: earliestAllowed
        state.value = RingConnectionState.SYNCING
        onProgress(10)
        return try {
            val response = api.syncViomi(
                ViomiSyncRequestDto(
                    imei = imei,
                    beginAt = begin,
                    endAt = end,
                    metrics = selected.map { it.name }.toSet(),
                ),
            ).getOrThrow()
            check(response.persisted) { "云米数据尚未在服务端持久化" }
            val validMeasurements = response.measurements.mapNotNull { measurement ->
                measurement.toScopedEntityOrNull(ownerUserId, deviceId, end)
            }
            dao.insertMeasurements(validMeasurements)
            onProgress(100)
            RingSyncResult(selected, validMeasurements.size, end, requiresUpload = false)
        } finally {
            state.value = RingConnectionState.CONNECTED
        }
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult = sync(setOf(type))
    override suspend fun sendCommand(data: ByteArray): Boolean = false

    override suspend fun configureActiveMeasurement(intervalMinutes: Int, enabled: Boolean) {
        val imei = device.value?.address ?: error("请先绑定云米手表")
        val response = api.saveViomiMeasurementPlan(
            ViomiMeasurementPlanRequestDto(
                imei = imei,
                enabled = enabled,
                intervalMinutes = intervalMinutes,
            ),
        ).getOrThrow()
        check(response.intervalMinutes == intervalMinutes && response.enabled == enabled) {
            "云米主动测量计划未正确保存"
        }
    }

    private fun savedDevice(): RingDevice? = bindingStore.activeBinding.value
        .takeIf {
            it.vendor == WearableVendor.VIOMI_CLOUD &&
                !it.address.isNullOrBlank() &&
                bindingStore.boundToCurrentUser()
        }
        ?.let { RingDevice(it.address!!, it.deviceName ?: "云米云端手表", null) }
}

internal fun com.rehealth.genie.network.dto.ViomiMeasurementDto.toScopedEntityOrNull(
    ownerUserId: String,
    deviceId: String,
    now: Long,
): RingMeasurementEntity? {
    if (measuredAt <= 0L || measuredAt > now + MAX_FUTURE_SKEW_MILLIS) return null
    val valid = when (metricType) {
        RingMetricType.HEART_RATE.name -> primaryValue in 20.0..250.0
        RingMetricType.BLOOD_OXYGEN.name -> primaryValue in 50.0..100.0
        RingMetricType.BLOOD_PRESSURE.name -> {
            val diastolic = secondaryValue
            diastolic != null && primaryValue in 50.0..260.0 &&
                diastolic in 30.0..180.0 && primaryValue > diastolic
        }
        else -> false
    }
    if (!valid) return null
    val normalizedUnit = when (metricType) {
        RingMetricType.HEART_RATE.name -> "bpm"
        RingMetricType.BLOOD_OXYGEN.name -> "%"
        RingMetricType.BLOOD_PRESSURE.name -> "mmHg"
        else -> return null
    }
    return RingMeasurementEntity(
        id = "viomi-measurement-${UUID.nameUUIDFromBytes(
            "$ownerUserId|$deviceId|$id".toByteArray(StandardCharsets.UTF_8),
        )}",
        metricType = metricType,
        measuredAt = measuredAt,
        primaryValue = primaryValue,
        secondaryValue = secondaryValue,
        unit = normalizedUnit,
        quality = 100,
        source = VIOMI_SOURCE,
        ownerUserId = ownerUserId,
        deviceId = deviceId,
    )
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private const val MAX_SYNC_WINDOW_MILLIS = 31L * DAY_MILLIS
private const val SYNC_OVERLAP_MILLIS = 2L * DAY_MILLIS
private const val MAX_FUTURE_SKEW_MILLIS = 5L * 60 * 1000

private fun <T> ApiResult<T>.getOrThrow(): T = when (this) {
    is ApiResult.Success -> data
    is ApiResult.Unauthorized -> error(message)
    is ApiResult.Forbidden -> error(message)
    is ApiResult.InvalidRequest -> error(message)
    is ApiResult.InvalidResponse -> error(message)
    is ApiResult.ServiceUnavailable -> error(message)
    is ApiResult.NetworkError -> error(message)
}
