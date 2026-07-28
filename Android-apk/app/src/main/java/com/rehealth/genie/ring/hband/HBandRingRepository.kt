package com.rehealth.genie.ring.hband

import com.google.gson.Gson
import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.BloodGlucoseCalibration
import com.rehealth.genie.ring.MenstrualCycleConfig
import com.rehealth.genie.ring.RingFeatureRepository
import com.rehealth.genie.ring.RingFeatureType
import com.rehealth.genie.ring.WearableUserProfileSink
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.WearableVendor
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.StateFlow

class HBandRingRepository internal constructor(
    private val dao: RingDataDao,
    private val activeWearableStore: ActiveWearableBindingStore,
    private val gateway: HBandSdkGateway,
    private val modelNameHints: Set<String>,
    private val expectedMetrics: Set<RingMetricType>,
) : RingRepository, WearableUserProfileSink, RingFeatureRepository {
    override var wearableUserProfile: BaselineHealthProfile? = null
    override val connectionState: StateFlow<RingConnectionState> = gateway.connectionState
    override val connectedDevice: StateFlow<RingDevice?> = gateway.connectedDevice
    override val supportedMetrics: Set<RingMetricType>
        get() = gateway.capabilities.value.supportedMetrics intersect expectedMetrics
    override val supportedFeatures: Set<RingFeatureType>
        get() = gateway.capabilities.value.supportedFeatures

    override suspend fun scan(): List<RingDevice> {
        val boundAddress = activeBindingAddress()
        return gateway.scan().sortedWith(
            compareByDescending<RingDevice> { it.address.equals(boundAddress, ignoreCase = true) }
                .thenByDescending { device -> modelNameHints.any { device.name.orEmpty().contains(it, true) } }
                .thenByDescending { it.rssi ?: Int.MIN_VALUE },
        )
    }

    override suspend fun connect(device: RingDevice) {
        val profile = wearableUserProfile.toHBandProfile()
            ?: error("连接 HBand 设备前请先完善性别、年龄、身高和体重")
        val info = gateway.connect(device, profile) ?: error("HBand 设备连接或初始化失败")
        activeWearableStore.recordConnectedDevice(
            vendor = WearableVendor.HBAND,
            device = info.device,
            modelCode = info.modelCode,
            firmwareVersion = info.firmwareVersion,
            capabilityJson = Gson().toJson(info.capabilities),
        )
    }

    override suspend fun autoConnect(): Boolean {
        if (connectionState.value == RingConnectionState.CONNECTED) return true
        val binding = activeWearableStore.activeBinding.value
        if (binding.vendor != WearableVendor.HBAND || binding.address.isNullOrBlank()) return false
        return runCatching {
            connect(RingDevice(binding.address, binding.deviceName, null))
            connectionState.value == RingConnectionState.CONNECTED
        }.getOrDefault(false)
    }

    override suspend fun disconnect() = gateway.disconnect()

    override suspend fun syncAll(): RingSyncResult {
        if (connectionState.value != RingConnectionState.CONNECTED && !autoConnect()) return emptyResult()
        return persist(gateway.sync(supportedMetrics))
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult {
        if (type !in supportedMetrics || type !in MANUAL_METRICS) return emptyResult()
        return persist(gateway.measure(type))
    }

    override suspend fun sendCommand(data: ByteArray): Boolean = false

    override suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean {
        if (RingFeatureType.BLOOD_GLUCOSE_CALIBRATION !in supportedFeatures) return false
        return gateway.setBloodGlucoseCalibration(config)
    }

    override suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean {
        if (RingFeatureType.WOMENS_HEALTH !in supportedFeatures) return false
        return gateway.setMenstrualCycle(config)
    }

    private suspend fun persist(payload: HBandPayload): RingSyncResult {
        val deviceKey = connectedDevice.value?.address ?: activeBindingAddress()
        if (deviceKey.isNullOrBlank()) return emptyResult()
        val batch = HBandDataMapper.toEntities(payload, deviceKey)
        if (batch.size > 0) dao.insertBatch(batch)
        return result(batch)
    }

    private fun result(batch: RingDataBatch) = RingSyncResult(
        HBandDataMapper.collectedTypes(batch),
        batch.size,
        System.currentTimeMillis(),
    )

    private fun emptyResult() = RingSyncResult(emptySet(), 0, System.currentTimeMillis())

    private fun activeBindingAddress(): String? = activeWearableStore.activeBinding.value
        .takeIf { it.vendor == WearableVendor.HBAND }
        ?.address

    private fun BaselineHealthProfile?.toHBandProfile(): HBandUserProfile? {
        val source = this ?: return null
        val age = source.age?.takeIf { it in 1..120 } ?: return null
        val height = source.heightCm?.roundToInt()?.takeIf { it in 50..250 } ?: return null
        val weight = source.weightKg?.roundToInt()?.takeIf { it in 10..300 } ?: return null
        val sex = when (source.gender?.trim()?.lowercase()) {
            "male", "man", "m", "男" -> HBandSex.MALE
            "female", "woman", "f", "女" -> HBandSex.FEMALE
            else -> return null
        }
        return HBandUserProfile(sex, height, weight, age, DEFAULT_STEP_GOAL)
    }

    private companion object {
        // A product control setting required by PersonInfoData, not generated health telemetry.
        const val DEFAULT_STEP_GOAL = 10_000
        val MANUAL_METRICS = setOf(
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_OXYGEN,
            RingMetricType.HRV,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.ECG,
            RingMetricType.BLOOD_COMPONENT,
            RingMetricType.BODY_COMPOSITION,
        )
    }
}
