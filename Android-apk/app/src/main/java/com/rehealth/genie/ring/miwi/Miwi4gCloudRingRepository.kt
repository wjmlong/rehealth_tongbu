package com.rehealth.genie.ring.miwi

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.WearableVendor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 云米/MiwiTracker 4G 云平台手表（S8 等）的 App 侧仓库。
 *
 * 该类手表不走手机蓝牙：手表通过自带 4G SIM 将健康数据上报到厂商云，
 * 厂商云再回调推送到 ReHealth 后端（/rehealth/miwi/push），后端按
 * deviceId（= "miwi4g-" + sha256(IMEI) 前 24 位）匹配已绑定用户后入库。
 *
 * 因此 App 侧职责仅为：
 * 1. 记录用户输入的 IMEI 作为"已连接设备"（触发云端 /devices/bind 绑定）；
 * 2. 云端健康数据经后端 API 查询展示，本地不产生 BLE 采集记录。
 */
class Miwi4gCloudRingRepository(
    private val activeWearableStore: ActiveWearableBindingStore,
) : RingRepository {

    private val mutableConnectionState = MutableStateFlow(initialState())
    private val mutableConnectedDevice = MutableStateFlow(restoreBoundDevice())

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState.asStateFlow()
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice.asStateFlow()

    override val supportedMetrics: Set<RingMetricType> = setOf(
        RingMetricType.HEART_RATE,
        RingMetricType.BLOOD_PRESSURE,
        RingMetricType.BLOOD_OXYGEN,
        RingMetricType.TEMPERATURE,
        RingMetricType.STEPS,
        RingMetricType.SLEEP,
        RingMetricType.HRV,
    )

    /** 云平台手表不做蓝牙扫描；若已录入 IMEI 则返回该设备便于 UI 重连。 */
    override suspend fun scan(): List<RingDevice> {
        val bound = restoreBoundDevice()
        return if (bound != null) listOf(bound) else emptyList()
    }

    /**
     * "连接"即登记 IMEI 绑定：device.address 必须是 IMEI（10-17 位数字）。
     * 云端绑定由上层（RingViewModel -> RingCloudRepository.bindDevice）完成。
     */
    override suspend fun connect(device: RingDevice) {
        val imei = device.address.trim()
        require(IMEI_REGEX.matches(imei)) { "请输入正确的手表 IMEI（10-17 位数字）。" }
        mutableConnectionState.value = RingConnectionState.CONNECTING
        val normalized = RingDevice(address = imei, name = device.name ?: DEFAULT_DEVICE_NAME, rssi = null)
        activeWearableStore.recordConnectedDevice(
            vendor = WearableVendor.MIWI4G,
            device = normalized,
            modelCode = MODEL_CODE,
        )
        mutableConnectedDevice.value = normalized
        mutableConnectionState.value = RingConnectionState.CONNECTED
    }

    override suspend fun autoConnect(): Boolean {
        val bound = restoreBoundDevice() ?: return false
        mutableConnectedDevice.value = bound
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return true
    }

    override suspend fun disconnect() {
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    /**
     * 数据链路为 手表 -> 厂商云 -> ReHealth 后端，App 无法从设备直接拉取。
     * 同步动作不产生本地记录；云端最新数据由后端查询接口在页面刷新时读取。
     */
    override suspend fun syncAll(): RingSyncResult {
        check(mutableConnectedDevice.value != null) { "请先输入 IMEI 绑定 4G 手表。" }
        return RingSyncResult(
            collectedTypes = emptySet(),
            recordsWritten = 0,
            completedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult {
        error("4G 云平台手表暂不支持从 App 端发起实时测量，请在手表端操作。")
    }

    override suspend fun sendCommand(data: ByteArray): Boolean = false

    private fun initialState(): RingConnectionState =
        if (restoreBoundDevice() != null) RingConnectionState.CONNECTED else RingConnectionState.DISCONNECTED

    private fun restoreBoundDevice(): RingDevice? {
        val binding = activeWearableStore.activeBinding.value
        if (binding.vendor != WearableVendor.MIWI4G) return null
        val address = binding.address?.takeIf { it.isNotBlank() } ?: return null
        return RingDevice(address = address, name = binding.deviceName ?: DEFAULT_DEVICE_NAME, rssi = null)
    }

    companion object {
        const val DEFAULT_DEVICE_NAME = "ReHealth 4G 手表 S8"
        const val MODEL_CODE = "S8"
        private val IMEI_REGEX = Regex("^\\d{10,17}$")
    }
}
