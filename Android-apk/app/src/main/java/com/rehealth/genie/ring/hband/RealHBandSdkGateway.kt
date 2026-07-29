package com.rehealth.genie.ring.hband

import android.content.Context
import com.inuker.bluetooth.library.Code
import com.inuker.bluetooth.library.connect.response.BleWriteResponse
import com.inuker.bluetooth.library.model.BleGattProfile
import com.inuker.bluetooth.library.search.SearchResult
import com.inuker.bluetooth.library.search.response.SearchResponse
import com.rehealth.genie.ring.RingBleGuards
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.BloodGlucoseCalibration
import com.rehealth.genie.ring.MenstrualCycleConfig
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.listener.base.IConnectResponse
import com.veepoo.protocol.listener.base.INotifyResponse
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener
import com.veepoo.protocol.listener.data.IBPDetectDataListener
import com.veepoo.protocol.listener.data.IBloodComponentDetectListener
import com.veepoo.protocol.listener.data.IBloodGlucoseChangeListener
import com.veepoo.protocol.listener.data.IBodyComponentDetectListener
import com.veepoo.protocol.listener.data.IBodyComponentReadDataListener
import com.veepoo.protocol.listener.data.ICustomSettingDataListener
import com.veepoo.protocol.listener.data.IECGDetectListener
import com.veepoo.protocol.listener.data.IECGReadDataListener
import com.veepoo.protocol.listener.data.IDeviceManualDetectDataListener
import com.veepoo.protocol.listener.data.IHeartDataListener
import com.veepoo.protocol.listener.data.IHrvDetectListener
import com.veepoo.protocol.listener.data.IMetDetectListener
import com.veepoo.protocol.listener.data.IOriginDataListener
import com.veepoo.protocol.listener.data.IPersonInfoDataListener
import com.veepoo.protocol.listener.data.IPressureDetectListener
import com.veepoo.protocol.listener.data.IPwdDataListener
import com.veepoo.protocol.listener.data.ISleepDataListener
import com.veepoo.protocol.listener.data.ISocialMsgDataListener
import com.veepoo.protocol.listener.data.ISpo2hDataListener
import com.veepoo.protocol.listener.data.ISportDataListener
import com.veepoo.protocol.listener.data.ITemptureDataListener
import com.veepoo.protocol.listener.data.ITemptureDetectDataListener
import com.veepoo.protocol.listener.data.IWomenDataListener
import com.veepoo.protocol.model.datas.BloodComponent
import com.veepoo.protocol.model.datas.BloodComponentManualData
import com.veepoo.protocol.model.datas.BloodGlucoseManualData
import com.veepoo.protocol.model.datas.BloodOxygenManualData
import com.veepoo.protocol.model.datas.BloodPressureManualData
import com.veepoo.protocol.model.datas.BodyTemperatureManualData
import com.veepoo.protocol.model.datas.BodyComponent
import com.veepoo.protocol.model.datas.EmotionManualData
import com.veepoo.protocol.model.datas.FatigueManualData
import com.veepoo.protocol.model.datas.HeartRateManualData
import com.veepoo.protocol.model.datas.HrvManualData
import com.veepoo.protocol.model.datas.MealInfo
import com.veepoo.protocol.model.datas.MetoManualData
import com.veepoo.protocol.model.datas.MiniCheckupManualData
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5
import com.veepoo.protocol.model.datas.EcgDetectInfo
import com.veepoo.protocol.model.datas.EcgDetectResult
import com.veepoo.protocol.model.datas.EcgDetectState
import com.veepoo.protocol.model.datas.EcgDiagnosis
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData
import com.veepoo.protocol.model.datas.FunctionSocailMsgData
import com.veepoo.protocol.model.datas.HeartData
import com.veepoo.protocol.model.datas.OriginData
import com.veepoo.protocol.model.datas.OriginHalfHourData
import com.veepoo.protocol.model.datas.PersonInfoData
import com.veepoo.protocol.model.datas.PressureManualData
import com.veepoo.protocol.model.datas.PwdData
import com.veepoo.protocol.model.datas.SkinConductanceManualData
import com.veepoo.protocol.model.datas.SleepData
import com.veepoo.protocol.model.datas.SportData
import com.veepoo.protocol.model.datas.TemptureData
import com.veepoo.protocol.model.datas.TimeData
import com.veepoo.protocol.model.enums.DeviceManualDataType
import com.veepoo.protocol.model.enums.EFunctionStatus
import com.veepoo.protocol.model.enums.EBPDetectModel
import com.veepoo.protocol.model.enums.EBPDetectStatus
import com.veepoo.protocol.model.enums.EBloodGlucoseUnit
import com.veepoo.protocol.model.enums.EHeartStatus
import com.veepoo.protocol.model.enums.EBloodFatUnit
import com.veepoo.protocol.model.enums.EBloodGlucoseRiskLevel
import com.veepoo.protocol.model.enums.EBloodGlucoseStatus
import com.veepoo.protocol.model.enums.EEcgDataType
import com.veepoo.protocol.model.enums.EDeviceStatus
import com.veepoo.protocol.model.enums.ETemperatureUnit
import com.veepoo.protocol.model.enums.EUricAcidUnit
import com.veepoo.protocol.model.enums.HrvDetectState
import com.veepoo.protocol.model.enums.EWomenOprateStatus
import com.veepoo.protocol.model.enums.EWomenStatus
import com.veepoo.protocol.model.settings.CustomSettingData
import com.veepoo.protocol.model.settings.ReadOriginSetting
import com.veepoo.protocol.model.settings.WomenSetting
import com.veepoo.protocol.model.enums.EOprateStauts
import com.veepoo.protocol.model.enums.EPwdStatus
import com.veepoo.protocol.model.enums.ESex
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.TimeoutCancellationException

/** The sole Android source file allowed to reference HBand/VeePoo SDK types. */
internal class RealHBandSdkGateway(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val queue: HBandCommandQueue = HBandCommandQueue(),
) : HBandSdkGateway {
    private val appContext = context.applicationContext
    private val manager = VPOperateManager.getMangerInstance(appContext)
    private val stateMachine = HBandConnectionStateMachine()
    private val scanned = ConcurrentHashMap<String, RingDevice>()
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    private val mutableCapabilities = MutableStateFlow(HBandCapabilities())

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState.asStateFlow()
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice.asStateFlow()
    override val capabilities: StateFlow<HBandCapabilities> = mutableCapabilities.asStateFlow()

    init {
        manager.init(appContext)
    }

    override suspend fun scan(): List<RingDevice> {
        if (!prepareBle()) return emptyList()
        stateMachine.startScan()
        publishState()
        val finished = CompletableDeferred<Unit>()
        val callback = object : SearchResponse {
            override fun onSearchStarted() = Unit
            override fun onDeviceFounded(device: SearchResult) {
                if (!runCatching { manager.isVPDevice(device.scanRecord) }.getOrDefault(false)) return
                val address = device.address?.takeIf(String::isNotBlank) ?: return
                val mapped = RingDevice(address, device.name?.takeIf(String::isNotBlank), device.rssi)
                scanned[mapped.address] = mapped
            }
            override fun onSearchStopped() { finished.complete(Unit) }
            override fun onSearchCanceled() { finished.complete(Unit) }
        }
        try {
            queue.execute(SCAN_TIMEOUT_MILLIS) {
                withContext(Dispatchers.Main.immediate) { manager.startScanDevice(callback) }
                finished.await()
            }
        } catch (error: Exception) {
            error.rethrowIfExternalCancellation()
            // A bounded scan returns candidates found before timeout.
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopScanDevice() }
            stateMachine.finishScan(manager.isCurrentDeviceConnected)
            publishState()
        }
        return scanned.values.sortedByDescending { it.rssi ?: Int.MIN_VALUE }.take(MAX_SCAN_RESULTS)
    }

    override suspend fun connect(device: RingDevice, profile: HBandUserProfile): HBandConnectionInfo? {
        if (!prepareBle()) return null
        return try {
            queue.execute(CONNECT_TIMEOUT_MILLIS) { connectSerial(device, profile) }
        } catch (error: Exception) {
            resetAfterFailure()
            error.rethrowIfExternalCancellation()
            null
        }
    }

    private suspend fun connectSerial(device: RingDevice, profile: HBandUserProfile): HBandConnectionInfo {
        stateMachine.startConnect()
        stateMachine.waitForNotify()
        publishState()
        val notified = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main.immediate) {
            manager.connectDevice(
                device.address,
                object : IConnectResponse {
                    override fun connectState(code: Int, profile: BleGattProfile?, isOadModel: Boolean) {
                        if (code != Code.REQUEST_SUCCESS) notified.complete(false)
                    }
                },
                object : INotifyResponse {
                    override fun notifyState(code: Int) { notified.complete(code == Code.REQUEST_SUCCESS) }
                },
            )
        }
        check(notified.await()) { "HBand notify channel failed" }
        stateMachine.verifyPassword()
        publishState()

        val password = CompletableDeferred<PwdSnapshot>()
        val capability = CompletableDeferred<HBandCapabilities>()
        withContext(Dispatchers.Main.immediate) {
            manager.confirmDevicePwd(
                writeResponse,
                passwordListener(password),
                capabilityListener(capability),
                socialListener,
                DEFAULT_DEVICE_PASSWORD,
                true,
            )
        }
        val pwd = password.await()
        check(pwd.success) { "HBand password confirmation failed" }
        stateMachine.readCapabilities()
        publishState()
        val supported = capability.await()
        stateMachine.syncProfile()
        publishState()
        syncPersonProfile(profile)

        mutableCapabilities.value = supported
        mutableConnectedDevice.value = device
        stateMachine.ready()
        publishState()
        return HBandConnectionInfo(device, pwd.deviceNumber?.let { "device:$it" }, pwd.firmwareVersion, supported)
    }

    override suspend fun disconnect() {
        try {
            queue.execute(COMMAND_TIMEOUT_MILLIS) {
                withContext(Dispatchers.Main.immediate) { manager.disconnectWatch(writeResponse) }
            }
        } catch (error: Exception) {
            if (error.isExternalCancellation()) {
                mutableConnectedDevice.value = null
                mutableCapabilities.value = HBandCapabilities()
                stateMachine.disconnect()
                publishState()
                throw error
            }
        }
        mutableConnectedDevice.value = null
        mutableCapabilities.value = HBandCapabilities()
        stateMachine.disconnect()
        publishState()
    }

    override suspend fun sync(metrics: Set<RingMetricType>): HBandPayload {
        if (!manager.isCurrentDeviceConnected || stateMachine.phase.value != HBandConnectionPhase.READY) return HBandPayload()
        var accumulated = HBandPayload()
        return try {
            queue.execute(HISTORY_TIMEOUT_MILLIS) {
                stateMachine.startSync()
                publishState()
                // ECG is attempted first because later vendor history reads can be long-running.
                if (RingMetricType.ECG in metrics) accumulated += optionalHistory { readEcgHistory() }
                if (RingMetricType.STEPS in metrics || RingMetricType.ACTIVITY in metrics) {
                    accumulated += readDailySport()
                }
                if (RingMetricType.SLEEP in metrics) {
                    // Some HBand firmware returns origin data but omits sleep from readAllHealthData.
                    // Use the vendor's dedicated command and await completion before the next long read.
                    accumulated += readSleepHistory()
                }
                if (metrics.any { it in ORIGIN_HISTORY_METRICS }) {
                    accumulated += readOriginHistory(metrics)
                }
                if (metrics.any { it in DEVICE_MANUAL_HISTORY_METRICS }) {
                    accumulated += optionalHistory { readManualMeasurementHistory(metrics) }
                }
                if (RingMetricType.BODY_COMPOSITION in metrics) {
                    accumulated += optionalHistory { readBodyCompositionHistory() }
                }
                stateMachine.ready()
                publishState()
                accumulated
            }
        } catch (error: Exception) {
            if (error.isExternalCancellation()) {
                resetAfterFailure()
                throw error
            }
            if (manager.isCurrentDeviceConnected) stateMachine.recoverReady() else stateMachine.fail()
            publishState()
            // Preserve completed reads when a later vendor command times out.
            accumulated
        }
    }

    override suspend fun measure(type: RingMetricType): HBandPayload {
        if (type !in MANUAL_METRICS || type !in capabilities.value.supportedMetrics) return HBandPayload()
        return try {
            queue.execute(MEASUREMENT_TIMEOUT_MILLIS) {
                stateMachine.startSync()
                publishState()
                try {
                    when (type) {
                        RingMetricType.HEART_RATE -> measureHeartRate()
                        RingMetricType.BLOOD_OXYGEN -> measureBloodOxygen()
                        RingMetricType.HRV -> measureHrv()
                        RingMetricType.BLOOD_PRESSURE -> measureBloodPressure()
                        RingMetricType.BLOOD_GLUCOSE -> measureBloodGlucose()
                        RingMetricType.TEMPERATURE -> measureTemperature()
                        RingMetricType.STRESS -> measureStress()
                        RingMetricType.MET -> measureMet()
                        RingMetricType.ECG -> measureEcg()
                        RingMetricType.BLOOD_COMPONENT -> measureBloodComponent()
                        RingMetricType.BODY_COMPOSITION -> measureBodyComposition()
                        else -> HBandPayload()
                    }
                } finally {
                    stateMachine.ready()
                    publishState()
                }
            }
        } catch (error: Exception) {
            if (error.isExternalCancellation()) {
                resetAfterFailure()
                throw error
            }
            if (manager.isCurrentDeviceConnected) stateMachine.recoverReady() else stateMachine.fail()
            publishState()
            HBandPayload()
        }
    }

    override suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean {
        if (!manager.isCurrentDeviceConnected || !config.referenceValue.isFinite() || config.referenceValue <= 0.0) return false
        if (!capabilities.value.bloodGlucoseCalibration) return false
        return runCatching {
            queue.execute(COMMAND_TIMEOUT_MILLIS) {
                val result = CompletableDeferred<Boolean>()
                val listener = object : IBloodGlucoseChangeListener {
                    override fun onDetectError(operation: Int, status: EBloodGlucoseStatus) = Unit
                    override fun onBloodGlucoseDetect(progress: Int, value: Float, risk: EBloodGlucoseRiskLevel) = Unit
                    override fun onBloodGlucoseStopDetect() = Unit
                    override fun onBloodGlucoseAdjustingSettingSuccess(enabled: Boolean, value: Float) {
                        result.complete(enabled == config.enabled)
                    }
                    override fun onBloodGlucoseAdjustingSettingFailed() { result.complete(false) }
                    override fun onBloodGlucoseAdjustingReadSuccess(enabled: Boolean, value: Float) = Unit
                    override fun onBloodGlucoseAdjustingReadFailed() = Unit
                    override fun onBGMultipleAdjustingReadSuccess(
                        enabled: Boolean,
                        breakfast: MealInfo,
                        lunch: MealInfo,
                        dinner: MealInfo,
                    ) = Unit
                    override fun onBGMultipleAdjustingReadFailed() = Unit
                    override fun onBGMultipleAdjustingSettingSuccess() = Unit
                    override fun onBGMultipleAdjustingSettingFailed() = Unit
                }
                withContext(Dispatchers.Main.immediate) {
                    manager.setBloodGlucoseAdjustingData(
                        config.referenceValue.toFloat(),
                        config.enabled,
                        writeResponse,
                        listener,
                    )
                }
                result.await()
            }
        }.getOrDefault(false)
    }

    override suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean {
        if (!manager.isCurrentDeviceConnected || !capabilities.value.womensHealth) return false
        if (config.periodLengthDays !in 4..28 || config.cycleLengthDays < config.periodLengthDays || config.lastPeriodStartAt <= 0L) return false
        return runCatching {
            queue.execute(COMMAND_TIMEOUT_MILLIS) {
                val result = CompletableDeferred<Boolean>()
                val setting = WomenSetting(
                    EWomenStatus.MENES,
                    config.periodLengthDays,
                    config.cycleLengthDays,
                    TimeData(config.lastPeriodStartAt),
                )
                withContext(Dispatchers.Main.immediate) {
                    manager.settingWomenState(
                        writeResponse,
                        IWomenDataListener { data ->
                            when (data.oprateStatus) {
                                EWomenOprateStatus.SETTING_SUCCESS -> result.complete(true)
                                EWomenOprateStatus.SETTING_FAIL -> result.complete(false)
                                else -> Unit
                            }
                        },
                        setting,
                    )
                }
                result.await()
            }
        }.getOrDefault(false)
    }

    private suspend fun measureHeartRate(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample>()
        val listener = IHeartDataListener { data: HeartData ->
            if (data.heartStatus == EHeartStatus.STATE_HEART_NORMAL && data.data > 0) {
                result.complete(HBandMetricSample(RingMetricType.HEART_RATE, clock(), data.data.toDouble(), "bpm"))
            }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectHeart(writeResponse, listener) }
        return try {
            HBandPayload(measurements = listOf(result.await()))
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectHeart(writeResponse) }
        }
    }

    private suspend fun measureBloodOxygen(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = ISpo2hDataListener { data ->
            val value = data?.value ?: return@ISpo2hDataListener
            if (value in MIN_VALID_SPO2..MAX_VALID_SPO2) {
                result.complete(HBandMetricSample(RingMetricType.BLOOD_OXYGEN, clock(), value.toDouble(), "%"))
            }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectSPO2H(writeResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectSPO2H(writeResponse, listener) }
        }
    }

    private suspend fun measureHrv(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = object : IHrvDetectListener {
            override fun onHrvDetect(hrv: Int) {
                if (hrv > 0) result.complete(HBandMetricSample(RingMetricType.HRV, clock(), hrv.toDouble(), "ms"))
            }

            override fun onDetectFailed(detectState: HrvDetectState) {
                result.complete(null)
            }

            override fun onDetectStop() {
                result.complete(null)
            }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectHrv(sdkWriteResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectHrv(sdkWriteResponse, listener) }
        }
    }

    private suspend fun measureBloodPressure(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = IBPDetectDataListener { data ->
            when (data.status) {
                EBPDetectStatus.STATE_BP_NORMAL -> {
                    if (validBloodPressure(data.highPressure, data.lowPressure)) {
                        result.complete(
                            HBandMetricSample(
                                RingMetricType.BLOOD_PRESSURE,
                                clock(),
                                data.highPressure.toDouble(),
                                "mmHg",
                                data.lowPressure.toDouble(),
                            ),
                        )
                    }
                }
                EBPDetectStatus.STATE_BP_BUSY,
                EBPDetectStatus.STATE_BP_LOW_BATTERY,
                EBPDetectStatus.STATE_BP_CHARGING,
                EBPDetectStatus.STATE_BP_WEAR_OFF,
                -> result.complete(null)
                null -> Unit
            }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.startDetectBP(writeResponse, listener, EBPDetectModel.DETECT_MODEL_PUBLIC)
        }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) {
                manager.stopDetectBP(writeResponse, EBPDetectModel.DETECT_MODEL_PUBLIC)
            }
        }
    }

    private suspend fun measureBloodGlucose(): HBandPayload {
        val unit = readBloodGlucoseUnit()
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = object : IBloodGlucoseChangeListener {
            override fun onDetectError(operation: Int, status: EBloodGlucoseStatus) {
                result.complete(null)
            }

            override fun onBloodGlucoseDetect(
                progress: Int,
                value: Float,
                risk: EBloodGlucoseRiskLevel,
            ) {
                if (progress >= MEASUREMENT_COMPLETE_PROGRESS && value.isFinite() && value > 0f) {
                    result.complete(
                        HBandMetricSample(
                            RingMetricType.BLOOD_GLUCOSE,
                            clock(),
                            value.toDouble(),
                            unit,
                        ),
                    )
                }
            }

            override fun onBloodGlucoseStopDetect() { result.complete(null) }
            override fun onBloodGlucoseAdjustingSettingSuccess(enabled: Boolean, value: Float) = Unit
            override fun onBloodGlucoseAdjustingSettingFailed() = Unit
            override fun onBloodGlucoseAdjustingReadSuccess(enabled: Boolean, value: Float) = Unit
            override fun onBloodGlucoseAdjustingReadFailed() = Unit
            override fun onBGMultipleAdjustingReadSuccess(
                enabled: Boolean,
                breakfast: MealInfo,
                lunch: MealInfo,
                dinner: MealInfo,
            ) = Unit
            override fun onBGMultipleAdjustingReadFailed() = Unit
            override fun onBGMultipleAdjustingSettingSuccess() = Unit
            override fun onBGMultipleAdjustingSettingFailed() = Unit
        }
        withContext(Dispatchers.Main.immediate) { manager.startBloodGlucoseDetect(writeResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopBloodGlucoseDetect(writeResponse, listener) }
        }
    }

    private suspend fun measureTemperature(): HBandPayload {
        val reportsFahrenheit = readTemperatureUnit() == ETemperatureUnit.FAHRENHEIT
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = ITemptureDetectDataListener { data ->
            val celsius = data?.tempture?.takeIf(Float::isFinite)?.let { value ->
                if (reportsFahrenheit) (value - 32f) * 5f / 9f else value
            }
            if (
                data?.deviceState == TEMPERATURE_DEVICE_READY &&
                data.progress >= MEASUREMENT_COMPLETE_PROGRESS &&
                celsius != null &&
                celsius in MIN_BODY_TEMPERATURE_C..MAX_BODY_TEMPERATURE_C
            ) {
                result.complete(HBandMetricSample(RingMetricType.TEMPERATURE, clock(), celsius.toDouble(), "°C"))
            } else if (data?.deviceState != null && data.deviceState != TEMPERATURE_DEVICE_READY) {
                result.complete(null)
            }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectTempture(writeResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectTempture(writeResponse, listener) }
        }
    }

    private suspend fun measureStress(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = object : IPressureDetectListener {
            override fun onDetecting(progress: Int) = Unit
            override fun onDetectSuccess(pressure: Int) {
                result.complete(
                    pressure.takeIf { it in MIN_STRESS_SCORE..MAX_STRESS_SCORE }?.let {
                        HBandMetricSample(RingMetricType.STRESS, clock(), it.toDouble(), "score")
                    },
                )
            }
            override fun onDetectFailed(detectState: com.veepoo.protocol.model.enums.PressureDetectState) {
                result.complete(null)
            }
            override fun onDetectStop() { result.complete(null) }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectPressure(sdkWriteResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectPressure(sdkWriteResponse) }
        }
    }

    private suspend fun measureMet(): HBandPayload {
        val result = CompletableDeferred<HBandMetricSample?>()
        val listener = object : IMetDetectListener {
            override fun onMetDetect(progress: Int, met: Float) {
                if (progress >= MEASUREMENT_COMPLETE_PROGRESS && met.isFinite() && met > 0f) {
                    result.complete(HBandMetricSample(RingMetricType.MET, clock(), met.toDouble(), "MET"))
                }
            }
            override fun onDetectFailed(detectState: com.veepoo.protocol.model.enums.MetDetectState) {
                result.complete(null)
            }
            override fun onDetectStop() { result.complete(null) }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectMet(sdkWriteResponse, listener) }
        return try {
            result.await()?.let { HBandPayload(measurements = listOf(it)) } ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectMet(sdkWriteResponse) }
        }
    }

    private suspend fun measureEcg(): HBandPayload {
        val result = CompletableDeferred<HBandEcgRecord?>()
        val callbackSamples = mutableListOf<Int>()
        var reportedFrequency: Int? = null
        val measurementStartedAt = clock()

        fun appendSamples(values: IntArray?) {
            val available = values?.takeIf { it.isNotEmpty() } ?: return
            synchronized(callbackSamples) {
                val remaining = (MAX_ECG_SAMPLES - callbackSamples.size).coerceAtLeast(0)
                available.take(remaining).forEach(callbackSamples::add)
            }
        }

        fun completeCapture(
            measuredAt: Long?,
            frequency: Int?,
            averageHeartRate: Int?,
            preferredSamples: IntArray?,
            fallbackSamples: IntArray?,
        ) {
            if (result.isCompleted) return
            val observed = synchronized(callbackSamples) { callbackSamples.toIntArray() }
            val samples = preferredSamples?.takeIf { it.isNotEmpty() }
                ?: fallbackSamples?.takeIf { it.isNotEmpty() }
                ?: observed.takeIf { it.isNotEmpty() }
                ?: IntArray(0)
            val validAverageHeartRate = averageHeartRate?.takeIf { it > 0 }
            if (samples.isEmpty() && validAverageHeartRate == null) return
            result.complete(
                HBandEcgRecord(
                    measuredAt = measuredAt?.takeIf { it > 0 } ?: clock(),
                    sampleRateHz = frequency?.takeIf { it > 0 } ?: reportedFrequency,
                    samples = samples.copyOf(MAX_ECG_SAMPLES.coerceAtMost(samples.size)),
                    averageHeartRate = validAverageHeartRate,
                ),
            )
        }

        val listener = object : IECGDetectListener {
            override fun onEcgDetectInfoChange(data: EcgDetectInfo?) {
                reportedFrequency = data?.frequency?.takeIf { it > 0 }
            }

            override fun onEcgDetectStateChange(data: EcgDetectState?) {
                if (data == null) return
                when {
                    data.dataType == ECG_NORMAL_END_DATA_TYPE || data.deviceState == EDeviceStatus.FINISH -> {
                        completeCapture(
                            measurementStartedAt,
                            reportedFrequency,
                            data.hr2,
                            null,
                            null,
                        )
                    }
                    data.dataType == ECG_FAILURE_DATA_TYPE || data.deviceState in ECG_TERMINAL_FAILURE_STATES -> {
                        result.complete(null)
                    }
                }
            }

            override fun onEcgDetectResultChange(data: EcgDetectResult?) {
                if (data?.isSuccess != true) return
                completeCapture(
                    data.timeBean?.toEpochMillis(),
                    data.frequency,
                    data.aveHeart,
                    data.filterSignals,
                    data.originSign,
                )
            }

            override fun onEcgDetectDiagnosisChange(data: EcgDiagnosis?) {
                if (data?.isSuccess != true) return
                completeCapture(
                    data.timeBean?.toEpochMillis(),
                    data.frequency,
                    data.heartRate,
                    data.filterSignals,
                    null,
                )
            }

            override fun onEcgADCChange(origin: IntArray?, filtered: IntArray?) {
                appendSamples(filtered?.takeIf { it.isNotEmpty() } ?: origin)
            }
        }

        withContext(Dispatchers.Main.immediate) { manager.startDetectECG(sdkWriteResponse, true, listener) }
        return try {
            val capture = result.await() ?: return HBandPayload()
            val summary = capture.averageHeartRate?.let {
                HBandMetricSample(RingMetricType.ECG, capture.measuredAt, it.toDouble(), "bpm")
            }
            HBandPayload(
                measurements = summary?.let(::listOf).orEmpty(),
                ecgRecords = listOf(capture),
            )
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectECG(sdkWriteResponse, true, listener) }
        }
    }

    private suspend fun measureBloodComponent(): HBandPayload {
        val units = readBloodComponentUnits()
        val result = CompletableDeferred<BloodComponent?>()
        val listener = object : IBloodComponentDetectListener {
            override fun onDetectFailed(errorState: com.veepoo.protocol.model.enums.EBloodComponentDetectState) {
                result.complete(null)
            }

            override fun onDetecting(progress: Int, bloodComponent: BloodComponent) = Unit

            override fun onDetectStop() {
                result.complete(null)
            }

            override fun onDetectComplete(bloodComponent: BloodComponent) {
                result.complete(bloodComponent)
            }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.startDetectBloodComponent(sdkWriteResponse, false, listener)
        }
        return try {
            result.await()?.toPayload(clock(), units) ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectBloodComponent(sdkWriteResponse) }
        }
    }

    private suspend fun measureBodyComposition(): HBandPayload {
        val result = CompletableDeferred<BodyComponent?>()
        var consecutiveLeadDrops = 0
        val listener = object : IBodyComponentDetectListener {
            override fun onDetecting(progress: Int, leadState: Int) {
                consecutiveLeadDrops = if (leadState == BODY_LEAD_DROPPED) consecutiveLeadDrops + 1 else 0
                if (consecutiveLeadDrops >= MAX_CONSECUTIVE_LEAD_DROPS) result.complete(null)
            }

            override fun onDetectSuccess(bodyComponent: BodyComponent) {
                result.complete(bodyComponent)
            }

            override fun onDetectFailed(detectState: com.veepoo.protocol.model.enums.DetectState) {
                result.complete(null)
            }

            override fun onDetectStop() {
                result.complete(null)
            }
        }
        withContext(Dispatchers.Main.immediate) { manager.startDetectBodyComponent(sdkWriteResponse, listener) }
        return try {
            result.await()?.toPayload(clock()) ?: HBandPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectBodyComponent(sdkWriteResponse) }
        }
    }

    private suspend fun readBloodComponentUnits(): BloodComponentUnits {
        val result = CompletableDeferred<CustomSettingData>()
        withContext(Dispatchers.Main.immediate) {
            manager.readCustomSetting(
                writeResponse,
                ICustomSettingDataListener { data -> result.complete(data) },
            )
        }
        val setting = withTimeoutOrNull(UNIT_READ_TIMEOUT_MILLIS) { result.await() }
            ?: return BloodComponentUnits()
        return BloodComponentUnits(
            uricAcid = when (setting.uricAcidUnit) {
                EUricAcidUnit.umol_L -> "µmol/L"
                EUricAcidUnit.mg_dl -> "mg/dL"
                else -> ""
            },
            bloodFat = when (setting.bloodFatUnit) {
                EBloodFatUnit.mmol_L -> "mmol/L"
                EBloodFatUnit.mg_dl -> "mg/dL"
                else -> ""
            },
        )
    }

    private suspend fun readBloodGlucoseUnit(): String {
        val result = CompletableDeferred<CustomSettingData>()
        withContext(Dispatchers.Main.immediate) {
            manager.readCustomSetting(writeResponse, ICustomSettingDataListener { data -> result.complete(data) })
        }
        val setting = withTimeoutOrNull(UNIT_READ_TIMEOUT_MILLIS) { result.await() }
        return when (setting?.bloodGlucoseUnit) {
            EBloodGlucoseUnit.mmol_L -> "mmol/L"
            EBloodGlucoseUnit.mg_dl -> "mg/dL"
            else -> ""
        }
    }

    private suspend fun readTemperatureUnit(): ETemperatureUnit {
        val result = CompletableDeferred<CustomSettingData>()
        withContext(Dispatchers.Main.immediate) {
            manager.readCustomSetting(writeResponse, ICustomSettingDataListener { data -> result.complete(data) })
        }
        return withTimeoutOrNull(UNIT_READ_TIMEOUT_MILLIS) { result.await() }
            ?.temperatureUnit
            ?: ETemperatureUnit.CELSIUS
    }

    private fun BloodComponent.toPayload(measuredAt: Long, units: BloodComponentUnits): HBandPayload = HBandPayload(
        measurements = listOfNotNull(
            positiveSample(RingMetricType.URIC_ACID, measuredAt, uricAcid, units.uricAcid),
            positiveSample(RingMetricType.TOTAL_CHOLESTEROL, measuredAt, tCHO, units.bloodFat),
            positiveSample(RingMetricType.TRIGLYCERIDES, measuredAt, tAG, units.bloodFat),
            positiveSample(RingMetricType.HDL_CHOLESTEROL, measuredAt, hDL, units.bloodFat),
            positiveSample(RingMetricType.LDL_CHOLESTEROL, measuredAt, lDL, units.bloodFat),
        ),
    )

    private fun BodyComponent.toPayload(measuredAt: Long): HBandPayload = HBandPayload(
        measurements = listOfNotNull(
            positiveSample(RingMetricType.BMI, measuredAt, BMI, "kg/m²"),
            positiveSample(RingMetricType.BODY_FAT_PERCENT, measuredAt, bodyFatRate, "%"),
            positiveSample(RingMetricType.FAT_MASS, measuredAt, fatRate, "kg"),
            positiveSample(RingMetricType.FAT_FREE_MASS, measuredAt, FFM, "kg"),
            positiveSample(RingMetricType.MUSCLE_PERCENT, measuredAt, muscleRate, "%"),
            positiveSample(RingMetricType.MUSCLE_MASS, measuredAt, muscleMass, "kg"),
            positiveSample(RingMetricType.SUBCUTANEOUS_FAT_PERCENT, measuredAt, subcutaneousFat, "%"),
            positiveSample(RingMetricType.BODY_WATER_PERCENT, measuredAt, bodyWater, "%"),
            positiveSample(RingMetricType.WATER_MASS, measuredAt, waterContent, "kg"),
            positiveSample(RingMetricType.SKELETAL_MUSCLE_PERCENT, measuredAt, skeletalMuscleRate, "%"),
            positiveSample(RingMetricType.BONE_MASS, measuredAt, boneMass, "kg"),
            positiveSample(RingMetricType.PROTEIN_PERCENT, measuredAt, proteinProportion, "%"),
            positiveSample(RingMetricType.PROTEIN_MASS, measuredAt, proteinMass, "kg"),
            positiveSample(RingMetricType.BASAL_METABOLIC_RATE, measuredAt, basalMetabolicRate, "kcal/day"),
        ),
    )

    private fun positiveSample(type: RingMetricType, measuredAt: Long, value: Float, unit: String): HBandMetricSample? =
        value.takeIf { it.isFinite() && it > 0f }?.let {
            HBandMetricSample(type, measuredAt, it.toDouble(), unit)
        }

    private suspend fun optionalHistory(read: suspend () -> HBandPayload): HBandPayload =
        withTimeoutOrNull(HISTORY_OPERATION_TIMEOUT_MILLIS) { read() } ?: HBandPayload()

    private suspend fun readTemperatureHistory(): HBandPayload {
        val records = mutableListOf<HBandMetricSample>()
        val complete = CompletableDeferred<Unit>()
        val reportsFahrenheit = readTemperatureUnit() == ETemperatureUnit.FAHRENHEIT
        val listener = object : ITemptureDataListener {
            override fun onTemptureDataListDataChange(data: List<TemptureData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.getmTime()?.toEpochMillis() ?: return@forEach
                    val raw = item.tempture.takeIf { it.isFinite() && it > 0f } ?: return@forEach
                    val celsius = if (reportsFahrenheit) (raw - 32f) * 5f / 9f else raw
                    if (celsius in MIN_BODY_TEMPERATURE_C..MAX_BODY_TEMPERATURE_C) {
                        records += HBandMetricSample(
                            RingMetricType.TEMPERATURE,
                            measuredAt,
                            celsius.toDouble(),
                            "°C",
                        )
                    }
                }
            }
            override fun onReadOriginProgressDetail(day: Int, date: String?, total: Int, current: Int) = Unit
            override fun onReadOriginProgress(progress: Float) = Unit
            override fun onReadOriginComplete() { complete.complete(Unit) }
        }
        val watchDataDays = watchDataDays()
        withContext(Dispatchers.Main.immediate) {
            manager.readTemptureDataBySetting(
                writeResponse,
                listener,
                ReadOriginSetting(watchDataDays, 0, false, 0),
            )
        }
        complete.await()
        return HBandPayload(measurements = records)
    }

    private suspend fun readManualMeasurementHistory(metrics: Set<RingMetricType>): HBandPayload {
        val requested = buildList {
            if (RingMetricType.BLOOD_PRESSURE in metrics) add(DeviceManualDataType.BLOOD_PRESSURE)
            if (RingMetricType.HEART_RATE in metrics) add(DeviceManualDataType.HEART_RATE)
            if (RingMetricType.BLOOD_GLUCOSE in metrics) add(DeviceManualDataType.BLOOD_GLUCOSE)
            if (RingMetricType.STRESS in metrics) add(DeviceManualDataType.STRESS)
            if (RingMetricType.BLOOD_OXYGEN in metrics) add(DeviceManualDataType.BLOOD_OXYGEN)
            if (RingMetricType.TEMPERATURE in metrics) add(DeviceManualDataType.BODY_TEMPERATURE)
            if (RingMetricType.MET in metrics) add(DeviceManualDataType.MET)
            if (RingMetricType.HRV in metrics) add(DeviceManualDataType.HRV)
            if (RingMetricType.BLOOD_COMPONENT in metrics) add(DeviceManualDataType.BLOOD_COMPOSITION)
        }
        if (requested.isEmpty()) return HBandPayload()

        val records = mutableListOf<HBandMetricSample>()
        val complete = CompletableDeferred<Boolean>()
        val bloodGlucoseUnit = if (RingMetricType.BLOOD_GLUCOSE in metrics) readBloodGlucoseUnit() else ""
        val bloodComponentUnits = if (RingMetricType.BLOOD_COMPONENT in metrics) {
            readBloodComponentUnits()
        } else {
            BloodComponentUnits()
        }
        val reportsFahrenheit = RingMetricType.TEMPERATURE in metrics &&
            readTemperatureUnit() == ETemperatureUnit.FAHRENHEIT

        val listener = object : IDeviceManualDetectDataListener {
            override fun onBloodPressureDataChange(data: List<BloodPressureManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    if (validBloodPressure(item.systolic, item.diastolic)) {
                        records += HBandMetricSample(
                            RingMetricType.BLOOD_PRESSURE,
                            measuredAt,
                            item.systolic.toDouble(),
                            "mmHg",
                            item.diastolic.toDouble(),
                        )
                    }
                }
            }

            override fun onHeartRateDataChange(data: List<HeartRateManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    item.rate?.lastOrNull { it > 0 }?.let { value ->
                        records += HBandMetricSample(RingMetricType.HEART_RATE, measuredAt, value.toDouble(), "bpm")
                    }
                }
            }

            override fun onBloodGlucoseDataChange(data: List<BloodGlucoseManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    positiveSample(
                        RingMetricType.BLOOD_GLUCOSE,
                        measuredAt,
                        item.bloodGlucoseValue,
                        bloodGlucoseUnit,
                    )?.let(records::add)
                }
            }

            override fun onPressureManualDataChange(data: List<PressureManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    if (item.pressure in MIN_STRESS_SCORE..MAX_STRESS_SCORE) {
                        records += HBandMetricSample(RingMetricType.STRESS, measuredAt, item.pressure.toDouble(), "score")
                    }
                }
            }

            override fun onBloodOxygenDataChange(data: List<BloodOxygenManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    item.oxygen?.lastOrNull { it in MIN_VALID_SPO2..MAX_VALID_SPO2 }?.let { value ->
                        records += HBandMetricSample(RingMetricType.BLOOD_OXYGEN, measuredAt, value.toDouble(), "%")
                    }
                }
            }

            override fun onBodyTemperatureDataChange(data: List<BodyTemperatureManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    val raw = item.temperature.takeIf { it.isFinite() && it > 0f } ?: return@forEach
                    val celsius = if (reportsFahrenheit) (raw - 32f) * 5f / 9f else raw
                    if (celsius in MIN_BODY_TEMPERATURE_C..MAX_BODY_TEMPERATURE_C) {
                        records += HBandMetricSample(RingMetricType.TEMPERATURE, measuredAt, celsius.toDouble(), "°C")
                    }
                }
            }

            override fun onMetoManualDataChange(data: List<MetoManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    if (item.meto > 0) {
                        records += HBandMetricSample(RingMetricType.MET, measuredAt, item.meto.toDouble(), "MET")
                    }
                }
            }

            override fun onHrvManualDataChange(data: List<HrvManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    item.hrv?.lastOrNull { it > 0 }?.let { value ->
                        records += HBandMetricSample(RingMetricType.HRV, measuredAt, value.toDouble(), "ms")
                    }
                }
            }

            override fun onBloodComponentManualDataChange(data: List<BloodComponentManualData>?) {
                data.orEmpty().forEach { item ->
                    val measuredAt = item.timeStamp.toEpochMillisFromSeconds() ?: return@forEach
                    records += item.toPayload(measuredAt, bloodComponentUnits).measurements
                }
            }

            override fun onMiniCheckupManualDataChange(data: List<MiniCheckupManualData>?) = Unit
            override fun onEmotionManualDataChange(data: List<EmotionManualData>?) = Unit
            override fun onFatigueManualDataChange(data: List<FatigueManualData>?) = Unit
            override fun onSkinConductanceManualDataChange(data: List<SkinConductanceManualData>?) = Unit
            override fun onReadProgress(progress: Float) = Unit
            override fun onReadComplete() { complete.complete(true) }
            override fun onReadFail() { complete.complete(false) }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.readDeviceManualData(writeResponse, 0L, requested, emptyList(), listener)
        }
        return if (complete.await()) HBandPayload(measurements = records) else HBandPayload()
    }

    private suspend fun readEcgHistory(): HBandPayload {
        val result = CompletableDeferred<HBandPayload>()
        val listener = object : IECGReadDataListener {
            override fun readDataFinish(data: List<EcgDetectResult>?) {
                result.complete(ecgHistoryPayload(data.orEmpty().mapNotNull { it.toDomainEcgRecord() }))
            }

            override fun readDiagnosisDataFinish(data: List<EcgDiagnosis>?) {
                result.complete(ecgHistoryPayload(data.orEmpty().mapNotNull { it.toDomainEcgRecord() }))
            }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.readECGData(
                sdkWriteResponse,
                TimeData(0, 0, 0, 0, 0, 0, 0),
                EEcgDataType.ALL,
                listener,
            )
        }
        return result.await()
    }

    private suspend fun readBodyCompositionHistory(): HBandPayload {
        val result = CompletableDeferred<HBandPayload>()
        withContext(Dispatchers.Main.immediate) {
            manager.readBodyComponentData(
                sdkWriteResponse,
                object : IBodyComponentReadDataListener {
                    override fun readBodyComponentDataFinish(bodyComponentList: List<BodyComponent>?) {
                        result.complete(
                            bodyComponentList.orEmpty().fold(HBandPayload()) { payload, item ->
                                val measuredAt = item.timeBean?.toEpochMillis() ?: return@fold payload
                                payload + item.toPayload(measuredAt)
                            },
                        )
                    }
                },
            )
        }
        return result.await()
    }

    private fun BloodComponentManualData.toPayload(
        measuredAt: Long,
        units: BloodComponentUnits,
    ): HBandPayload = HBandPayload(
        measurements = listOfNotNull(
            positiveSample(RingMetricType.URIC_ACID, measuredAt, uricAcid, units.uricAcid),
            positiveSample(RingMetricType.TOTAL_CHOLESTEROL, measuredAt, gettCHO(), units.bloodFat),
            positiveSample(RingMetricType.TRIGLYCERIDES, measuredAt, gettAG(), units.bloodFat),
            positiveSample(RingMetricType.HDL_CHOLESTEROL, measuredAt, gethDL(), units.bloodFat),
            positiveSample(RingMetricType.LDL_CHOLESTEROL, measuredAt, getlDL(), units.bloodFat),
        ),
    )

    private fun EcgDetectResult.toDomainEcgRecord(): HBandEcgRecord? {
        if (!isSuccess) return null
        val samples = filterSignals?.takeIf { it.isNotEmpty() }
            ?: originSign?.takeIf { it.isNotEmpty() }
            ?: IntArray(0)
        val averageHeartRate = aveHeart.takeIf { it > 0 }
        if (samples.isEmpty() && averageHeartRate == null) return null
        return HBandEcgRecord(
            measuredAt = timeBean?.toEpochMillis() ?: return null,
            sampleRateHz = frequency.takeIf { it > 0 },
            samples = samples.copyOf(minOf(samples.size, MAX_ECG_SAMPLES)),
            averageHeartRate = averageHeartRate,
        )
    }

    private fun EcgDiagnosis.toDomainEcgRecord(): HBandEcgRecord? {
        if (!isSuccess) return null
        val samples = filterSignals?.takeIf { it.isNotEmpty() } ?: IntArray(0)
        val averageHeartRate = heartRate.takeIf { it > 0 }
        if (samples.isEmpty() && averageHeartRate == null) return null
        return HBandEcgRecord(
            measuredAt = timeBean?.toEpochMillis() ?: return null,
            sampleRateHz = frequency.takeIf { it > 0 },
            samples = samples.copyOf(minOf(samples.size, MAX_ECG_SAMPLES)),
            averageHeartRate = averageHeartRate,
        )
    }

    private fun ecgHistoryPayload(records: List<HBandEcgRecord>): HBandPayload = HBandPayload(
        measurements = records.mapNotNull { record ->
            record.averageHeartRate?.let { heartRate ->
                HBandMetricSample(RingMetricType.ECG, record.measuredAt, heartRate.toDouble(), "bpm")
            }
        },
        ecgRecords = records,
    )

    private suspend fun readDailySport(): HBandPayload {
        val deferred = CompletableDeferred<SportData>()
        withContext(Dispatchers.Main.immediate) {
            manager.readSportStep(writeResponse, ISportDataListener { deferred.complete(it) })
        }
        val data = deferred.await()
        if (data.step <= 0 && data.dis <= 0.0 && data.kcal <= 0.0) return HBandPayload()
        val observedAt = clock()
        val start = Instant.ofEpochMilli(observedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return HBandPayload(
            // HBand SportUtil expresses distance in kilometres; Room stores metres.
            activities = listOf(HBandActivityRecord(start, observedAt, data.step, data.dis * METRES_PER_KILOMETRE, data.kcal)),
        )
    }

    private suspend fun readSleepHistory(): HBandPayload {
        val sleep = mutableListOf<HBandSleepRecord>()
        val complete = CompletableDeferred<Unit>()
        val listener = object : ISleepDataListener {
            override fun onSleepDataChange(day: String?, data: SleepData?) {
                data?.toDomainSleep()?.let(sleep::add)
            }
            override fun onSleepProgress(progress: Float) = Unit
            override fun onSleepProgressDetail(day: String?, progress: Int) = Unit
            override fun onReadSleepComplete() { complete.complete(Unit) }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.readSleepData(writeResponse, listener, watchDataDays())
        }
        complete.await()
        return HBandPayload(sleep = sleep)
    }

    private suspend fun readOriginHistory(metrics: Set<RingMetricType>): HBandPayload {
        val records = mutableListOf<HBandMetricSample>()
        val activities = HBandDailyActivityAccumulator()
        val complete = CompletableDeferred<Unit>()
        val reportsFahrenheit = RingMetricType.TEMPERATURE in metrics &&
            readTemperatureUnit() == ETemperatureUnit.FAHRENHEIT
        val listener = object : IOriginDataListener {
            override fun onOringinFiveMinuteDataChange(data: OriginData?) {
                if (data == null) return
                val measuredAt = data.getmTime()?.toEpochMillis() ?: return
                if (RingMetricType.STEPS in metrics || RingMetricType.ACTIVITY in metrics) {
                    activities.add(
                        measuredAt = measuredAt,
                        steps = data.stepValue,
                        distanceMeters = data.disValue * METRES_PER_KILOMETRE,
                        caloriesKcal = data.calValue,
                    )
                }
                if (RingMetricType.HEART_RATE in metrics && data.rateValue > 0) {
                    records += HBandMetricSample(RingMetricType.HEART_RATE, measuredAt, data.rateValue.toDouble(), "bpm")
                }
                if (RingMetricType.BLOOD_PRESSURE in metrics && validBloodPressure(data.highValue, data.lowValue)) {
                    records += HBandMetricSample(
                        RingMetricType.BLOOD_PRESSURE,
                        measuredAt,
                        data.highValue.toDouble(),
                        "mmHg",
                        data.lowValue.toDouble(),
                    )
                }
                if (RingMetricType.TEMPERATURE in metrics) {
                    val raw = data.temperature.takeIf { it.isFinite() && it > 0.0 }
                    val celsius = raw?.let { if (reportsFahrenheit) (it - 32.0) * 5.0 / 9.0 else it }
                    if (celsius != null && celsius in MIN_BODY_TEMPERATURE_C.toDouble()..MAX_BODY_TEMPERATURE_C.toDouble()) {
                        records += HBandMetricSample(RingMetricType.TEMPERATURE, measuredAt, celsius, "°C")
                    }
                }
            }
            override fun onOringinHalfHourDataChange(data: OriginHalfHourData?) {
                if (data == null) return
                if (RingMetricType.HEART_RATE in metrics) {
                    data.halfHourRateDatas.orEmpty().forEach { item ->
                        val measuredAt = item.time?.toEpochMillis() ?: return@forEach
                        if (item.rateValue > 0) {
                            records += HBandMetricSample(RingMetricType.HEART_RATE, measuredAt, item.rateValue.toDouble(), "bpm")
                        }
                    }
                }
                if (RingMetricType.BLOOD_PRESSURE in metrics) {
                    data.halfHourBps.orEmpty().forEach { item ->
                        val measuredAt = item.time?.toEpochMillis() ?: return@forEach
                        if (validBloodPressure(item.highValue, item.lowValue)) {
                            records += HBandMetricSample(
                                RingMetricType.BLOOD_PRESSURE,
                                measuredAt,
                                item.highValue.toDouble(),
                                "mmHg",
                                item.lowValue.toDouble(),
                            )
                        }
                    }
                }
            }
            override fun onReadOriginProgressDetail(day: Int, date: String?, total: Int, current: Int) = Unit
            override fun onReadOriginProgress(progress: Float) = Unit
            override fun onReadOriginComplete() { complete.complete(Unit) }
        }
        withContext(Dispatchers.Main.immediate) {
            manager.readOriginData(writeResponse, listener, watchDataDays())
        }
        complete.await()
        return HBandPayload(
            measurements = records,
            activities = activities.records(),
        )
    }

    private suspend fun syncPersonProfile(profile: HBandUserProfile) {
        val complete = CompletableDeferred<Boolean>()
        val data = PersonInfoData(
            if (profile.sex == HBandSex.MALE) ESex.MAN else ESex.WOMEN,
            profile.heightCm,
            profile.weightKg,
            profile.age,
            profile.stepGoal,
        )
        withContext(Dispatchers.Main.immediate) {
            manager.syncPersonInfo(writeResponse, IPersonInfoDataListener { complete.complete(it == EOprateStauts.OPRATE_SUCCESS) }, data)
        }
        check(complete.await()) { "HBand user profile sync failed" }
    }

    private fun passwordListener(result: CompletableDeferred<PwdSnapshot>) = object : IPwdDataListener {
        override fun onPwdDataChange(data: PwdData?) {
            if (data == null) return
            val terminal = data.getmStatus() in PASSWORD_TERMINAL_STATES
            if (terminal) {
                result.complete(
                    PwdSnapshot(
                        success = data.getmStatus() in PASSWORD_SUCCESS_STATES,
                        deviceNumber = data.deviceNumber.takeIf { it > 0 },
                        firmwareVersion = data.deviceVersion?.takeIf(String::isNotBlank),
                    ),
                )
            }
        }
        override fun onConnectionConfirmTimeout() { result.complete(PwdSnapshot(false, null, null)) }
    }

    private fun capabilityListener(result: CompletableDeferred<HBandCapabilities>) = object : IDeviceFuctionDataListener {
        override fun onFunctionSupportDataChange(data: FunctionDeviceSupportData?) {
            if (data != null) {
                result.complete(
                    HBandCapabilities(
                        steps = true,
                        sleep = true,
                        watchDataDays = data.wathcDay,
                        temperatureType = data.temptureType,
                        heartRate = data.heartDetect.hasFunction(),
                        bloodOxygen = data.spo2H.hasFunction(),
                        hrv = data.hrvAppDetectFunction.hasFunction(),
                        bloodPressure = data.bp.hasFunction(),
                        bloodGlucose = data.bloodGlucose.hasFunction(),
                        temperature = data.temperatureFunction.hasFunction(),
                        stress = data.stress.hasFunction(),
                        met = data.met.hasFunction(),
                        ecg = data.ecg.hasFunction(),
                        bloodComponent = data.bloodComponent.hasFunction(),
                        bodyComposition = data.bodyComponent.hasFunction(),
                        // This MVP exposes the single-reference private calibration API only.
                        bloodGlucoseCalibration = data.bloodGlucoseAdjusting.hasFunction(),
                        womensHealth = data.women.hasFunction(),
                    ),
                )
            }
        }
        override fun onDeviceFunctionPackage1Report(data: DeviceFunctionPackage1?) = Unit
        override fun onDeviceFunctionPackage2Report(data: DeviceFunctionPackage2?) = Unit
        override fun onDeviceFunctionPackage3Report(data: DeviceFunctionPackage3?) = Unit
        override fun onDeviceFunctionPackage4Report(data: DeviceFunctionPackage4?) = Unit
        override fun onDeviceFunctionPackage5Report(data: DeviceFunctionPackage5?) = Unit
    }

    private val socialListener = object : ISocialMsgDataListener {
        override fun onSocialMsgSupportDataChange(data: FunctionSocailMsgData?) = Unit
        override fun onSocialMsgSupportDataChange2(data: FunctionSocailMsgData?) = Unit
    }

    private fun EFunctionStatus?.hasFunction(): Boolean = this?.isHaveFunction == true

    private fun validBloodPressure(systolic: Int, diastolic: Int): Boolean =
        systolic in 70..250 && diastolic in 40..150 && systolic > diastolic

    private fun SleepData.toDomainSleep(): HBandSleepRecord? {
        val start = sleepDown?.toEpochMillis() ?: return null
        var end = sleepUp?.toEpochMillis() ?: return null
        if (end <= start) end += ONE_DAY_MILLIS
        val deep = deepSleepTime.coerceAtLeast(0)
        val light = lowSleepTime.coerceAtLeast(0)
        val total = allSleepTime.coerceAtLeast(deep + light)
        if (total <= 0) return null
        val awake = if (deep + light > 0) (total - deep - light).coerceAtLeast(0) else 0
        return HBandSleepRecord(start, end, deep, light, awake, total)
    }

    private fun TimeData.toEpochMillis(): Long? = runCatching { toCalendar().timeInMillis }.getOrNull()?.takeIf { it > 0 }

    private fun Int.toEpochMillisFromSeconds(): Long? =
        takeIf { it > 0 }?.toLong()?.times(MILLIS_PER_SECOND)

    private fun watchDataDays(): Int = capabilities.value.watchDataDays
        .takeIf { it > 0 }
        ?.coerceAtMost(MAX_WATCH_DATA_DAYS)
        ?: DEFAULT_WATCH_DATA_DAYS

    private fun prepareBle(): Boolean {
        mutableConnectionState.value = when {
            !RingBleGuards.isBluetoothAvailable(appContext) -> RingConnectionState.UNSUPPORTED
            !RingBleGuards.hasCollectionPermission(appContext) -> RingConnectionState.PERMISSION_REQUIRED
            !RingBleGuards.isBluetoothEnabled(appContext) -> RingConnectionState.BLUETOOTH_OFF
            else -> return true
        }
        return false
    }

    private fun publishState() { mutableConnectionState.value = stateMachine.ringState }

    private fun Throwable.rethrowIfExternalCancellation() {
        if (isExternalCancellation()) throw this
    }

    private fun Throwable.isExternalCancellation(): Boolean =
        this is CancellationException && this !is TimeoutCancellationException

    private suspend fun resetAfterFailure() {
        runCatching {
            withContext(NonCancellable + Dispatchers.Main.immediate) {
                manager.disconnectWatch(writeResponse)
            }
        }
        mutableConnectedDevice.value = null
        mutableCapabilities.value = HBandCapabilities()
        stateMachine.fail()
        publishState()
    }

    private data class PwdSnapshot(val success: Boolean, val deviceNumber: Int?, val firmwareVersion: String?)
    private data class BloodComponentUnits(val uricAcid: String = "", val bloodFat: String = "")

    private companion object {
        const val DEFAULT_DEVICE_PASSWORD = "0000" // Official SDK demo default; physical-device QA is still required.
        const val DEFAULT_WATCH_DATA_DAYS = 3
        const val MAX_WATCH_DATA_DAYS = 30
        const val MAX_SCAN_RESULTS = 30
        const val SCAN_TIMEOUT_MILLIS = 10_000L
        const val CONNECT_TIMEOUT_MILLIS = 35_000L
        const val COMMAND_TIMEOUT_MILLIS = 8_000L
        const val HISTORY_TIMEOUT_MILLIS = 180_000L
        const val HISTORY_OPERATION_TIMEOUT_MILLIS = 25_000L
        const val MEASUREMENT_TIMEOUT_MILLIS = 45_000L
        const val UNIT_READ_TIMEOUT_MILLIS = 5_000L
        const val ONE_DAY_MILLIS = 86_400_000L
        const val MILLIS_PER_SECOND = 1_000L
        const val METRES_PER_KILOMETRE = 1_000.0
        const val MAX_ECG_SAMPLES = 120_000
        const val MIN_VALID_SPO2 = 1
        const val MAX_VALID_SPO2 = 100
        const val MIN_BODY_TEMPERATURE_C = 25f
        const val MAX_BODY_TEMPERATURE_C = 45f
        const val TEMPERATURE_DEVICE_READY = 0
        const val MIN_STRESS_SCORE = 0
        const val MAX_STRESS_SCORE = 100
        const val MEASUREMENT_COMPLETE_PROGRESS = 100
        const val ECG_FAILURE_DATA_TYPE = 3
        const val ECG_NORMAL_END_DATA_TYPE = 4
        const val BODY_LEAD_DROPPED = 1
        const val MAX_CONSECUTIVE_LEAD_DROPS = 4
        val PASSWORD_SUCCESS_STATES = setOf(EPwdStatus.CHECK_SUCCESS, EPwdStatus.CHECK_AND_TIME_SUCCESS)
        val PASSWORD_TERMINAL_STATES = PASSWORD_SUCCESS_STATES + setOf(EPwdStatus.CHECK_FAIL, EPwdStatus.UNKNOW)
        val ECG_TERMINAL_FAILURE_STATES = setOf(
            EDeviceStatus.BUSY,
            EDeviceStatus.CHARGING,
            EDeviceStatus.CHARG_LOW,
            EDeviceStatus.UNPASS_WEAR,
            EDeviceStatus.KEEP_QUIT,
        )
        val MANUAL_METRICS = setOf(
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_OXYGEN,
            RingMetricType.HRV,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.BLOOD_GLUCOSE,
            RingMetricType.TEMPERATURE,
            RingMetricType.STRESS,
            RingMetricType.MET,
            RingMetricType.ECG,
            RingMetricType.BLOOD_COMPONENT,
            RingMetricType.BODY_COMPOSITION,
        )
        val DEVICE_MANUAL_HISTORY_METRICS = setOf(
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_OXYGEN,
            RingMetricType.HRV,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.BLOOD_GLUCOSE,
            RingMetricType.TEMPERATURE,
            RingMetricType.STRESS,
            RingMetricType.MET,
            RingMetricType.BLOOD_COMPONENT,
        )
        val ORIGIN_HISTORY_METRICS = setOf(
            RingMetricType.STEPS,
            RingMetricType.ACTIVITY,
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.TEMPERATURE,
        )
        val writeResponse = IBleWriteResponse { }
        val sdkWriteResponse = BleWriteResponse { }
    }
}
