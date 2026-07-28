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
import com.veepoo.protocol.VPOperateManager
import com.veepoo.protocol.listener.base.IBleWriteResponse
import com.veepoo.protocol.listener.base.IConnectResponse
import com.veepoo.protocol.listener.base.INotifyResponse
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener
import com.veepoo.protocol.listener.data.IBPDetectDataListener
import com.veepoo.protocol.listener.data.IECGDetectListener
import com.veepoo.protocol.listener.data.IHeartDataListener
import com.veepoo.protocol.listener.data.IOriginDataListener
import com.veepoo.protocol.listener.data.IPersonInfoDataListener
import com.veepoo.protocol.listener.data.IPwdDataListener
import com.veepoo.protocol.listener.data.ISleepDataListener
import com.veepoo.protocol.listener.data.ISocialMsgDataListener
import com.veepoo.protocol.listener.data.ISportDataListener
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
import com.veepoo.protocol.model.datas.PwdData
import com.veepoo.protocol.model.datas.SleepData
import com.veepoo.protocol.model.datas.SportData
import com.veepoo.protocol.model.datas.TimeData
import com.veepoo.protocol.model.enums.EFunctionStatus
import com.veepoo.protocol.model.enums.EBPDetectModel
import com.veepoo.protocol.model.enums.EBPDetectStatus
import com.veepoo.protocol.model.enums.EHeartStatus
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
        return try {
            queue.execute(HISTORY_TIMEOUT_MILLIS) {
                stateMachine.startSync()
                publishState()
                var result = HBandPayload()
                if (RingMetricType.STEPS in metrics || RingMetricType.ACTIVITY in metrics) result += readDailySport()
                if (RingMetricType.SLEEP in metrics) result += readSleep()
                if (RingMetricType.HEART_RATE in metrics || RingMetricType.BLOOD_PRESSURE in metrics) {
                    result += readOriginHistory(metrics)
                }
                stateMachine.ready()
                publishState()
                result
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

    override suspend fun measure(type: RingMetricType): HBandPayload {
        if (type !in MANUAL_METRICS || type !in capabilities.value.supportedMetrics) return HBandPayload()
        return try {
            queue.execute(MEASUREMENT_TIMEOUT_MILLIS) {
                stateMachine.startSync()
                publishState()
                try {
                    when (type) {
                        RingMetricType.HEART_RATE -> measureHeartRate()
                        RingMetricType.BLOOD_PRESSURE -> measureBloodPressure()
                        RingMetricType.ECG -> measureEcg()
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

    private suspend fun measureEcg(): HBandPayload {
        val result = CompletableDeferred<HBandEcgRecord>()
        val callbackSamples = mutableListOf<Int>()
        var reportedFrequency: Int? = null

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
                ?: return
            result.complete(
                HBandEcgRecord(
                    measuredAt = measuredAt?.takeIf { it > 0 } ?: clock(),
                    sampleRateHz = frequency?.takeIf { it > 0 } ?: reportedFrequency,
                    samples = samples.copyOf(MAX_ECG_SAMPLES.coerceAtMost(samples.size)),
                    averageHeartRate = averageHeartRate?.takeIf { it > 0 },
                ),
            )
        }

        val listener = object : IECGDetectListener {
            override fun onEcgDetectInfoChange(data: EcgDetectInfo?) {
                reportedFrequency = data?.frequency?.takeIf { it > 0 }
            }

            override fun onEcgDetectStateChange(data: EcgDetectState?) = Unit

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

        withContext(Dispatchers.Main.immediate) { manager.startDetectECG(ecgWriteResponse, true, listener) }
        return try {
            val capture = result.await()
            val summary = capture.averageHeartRate?.let {
                HBandMetricSample(RingMetricType.ECG, capture.measuredAt, it.toDouble(), "bpm")
            }
            HBandPayload(
                measurements = summary?.let(::listOf).orEmpty(),
                ecgRecords = listOf(capture),
            )
        } finally {
            withContext(Dispatchers.Main.immediate) { manager.stopDetectECG(ecgWriteResponse, true, listener) }
        }
    }

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

    private suspend fun readSleep(): HBandPayload {
        val records = mutableListOf<HBandSleepRecord>()
        val complete = CompletableDeferred<Unit>()
        val listener = object : ISleepDataListener {
            override fun onSleepDataChange(day: String?, data: SleepData?) {
                data?.toDomainSleep()?.let(records::add)
            }
            override fun onSleepProgress(progress: Float) = Unit
            override fun onSleepProgressDetail(day: String?, progress: Int) = Unit
            override fun onReadSleepComplete() { complete.complete(Unit) }
        }
        val watchDataDays = capabilities.value.watchDataDays
            .takeIf { it > 0 }
            ?.coerceAtMost(MAX_WATCH_DATA_DAYS)
            ?: DEFAULT_WATCH_DATA_DAYS
        withContext(Dispatchers.Main.immediate) { manager.readSleepData(writeResponse, listener, watchDataDays) }
        complete.await()
        return HBandPayload(sleep = records)
    }

    private suspend fun readOriginHistory(metrics: Set<RingMetricType>): HBandPayload {
        val records = mutableListOf<HBandMetricSample>()
        val complete = CompletableDeferred<Unit>()
        val listener = object : IOriginDataListener {
            override fun onOringinFiveMinuteDataChange(data: OriginData?) {
                if (data == null) return
                val measuredAt = data.getmTime()?.toEpochMillis() ?: return
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
            override fun onReadOriginProgressDetail(day: Int, date: String?, progress: Int, total: Int) = Unit
            override fun onReadOriginProgress(progress: Float) = Unit
            override fun onReadOriginComplete() { complete.complete(Unit) }
        }
        withContext(Dispatchers.Main.immediate) { manager.readOriginData(writeResponse, listener, ORIGIN_PROTOCOL_TYPE) }
        complete.await()
        return HBandPayload(measurements = records)
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
                        heartRate = data.heartDetect.hasFunction(),
                        bloodOxygen = data.spo2H.hasFunction(),
                        hrv = data.hrvFunction.hasFunction(),
                        bloodPressure = data.bp.hasFunction(),
                        ecg = data.ecg.hasFunction(),
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
        if (deep + light <= 0) return null
        return HBandSleepRecord(start, end, deep, light, (total - deep - light).coerceAtLeast(0))
    }

    private fun TimeData.toEpochMillis(): Long? = runCatching { toCalendar().timeInMillis }.getOrNull()?.takeIf { it > 0 }

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

    private companion object {
        const val DEFAULT_DEVICE_PASSWORD = "0000" // Official SDK demo default; physical-device QA is still required.
        const val ORIGIN_PROTOCOL_TYPE = 3
        const val DEFAULT_WATCH_DATA_DAYS = 3
        const val MAX_WATCH_DATA_DAYS = 30
        const val MAX_SCAN_RESULTS = 30
        const val SCAN_TIMEOUT_MILLIS = 10_000L
        const val CONNECT_TIMEOUT_MILLIS = 35_000L
        const val COMMAND_TIMEOUT_MILLIS = 8_000L
        const val HISTORY_TIMEOUT_MILLIS = 60_000L
        const val MEASUREMENT_TIMEOUT_MILLIS = 45_000L
        const val ONE_DAY_MILLIS = 86_400_000L
        const val METRES_PER_KILOMETRE = 1_000.0
        const val MAX_ECG_SAMPLES = 120_000
        val PASSWORD_SUCCESS_STATES = setOf(EPwdStatus.CHECK_SUCCESS, EPwdStatus.CHECK_AND_TIME_SUCCESS)
        val PASSWORD_TERMINAL_STATES = PASSWORD_SUCCESS_STATES + setOf(EPwdStatus.CHECK_FAIL, EPwdStatus.UNKNOW)
        val MANUAL_METRICS = setOf(
            RingMetricType.HEART_RATE,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.ECG,
        )
        val writeResponse = IBleWriteResponse { }
        val ecgWriteResponse = BleWriteResponse { }
    }
}
