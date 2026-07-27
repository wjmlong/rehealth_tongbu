package com.rehealth.genie.ring.hband

import android.content.Context
import com.inuker.bluetooth.library.Code
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
import com.veepoo.protocol.model.enums.EHeartStatus
import com.veepoo.protocol.model.enums.EOprateStauts
import com.veepoo.protocol.model.enums.EPwdStatus
import com.veepoo.protocol.model.enums.ESex
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

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
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            resetAfterFailure()
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
        runCatching {
            queue.execute(COMMAND_TIMEOUT_MILLIS) {
                withContext(Dispatchers.Main.immediate) { manager.disconnectWatch(writeResponse) }
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
                if (RingMetricType.HEART_RATE in metrics) result += readHeartHistory()
                stateMachine.ready()
                publishState()
                result
            }
        } catch (_: Exception) {
            if (manager.isCurrentDeviceConnected) stateMachine.recoverReady() else stateMachine.fail()
            publishState()
            HBandPayload()
        }
    }

    override suspend fun measure(type: RingMetricType): HBandPayload {
        if (type != RingMetricType.HEART_RATE || type !in capabilities.value.supportedMetrics) return HBandPayload()
        return try {
            queue.execute(MEASUREMENT_TIMEOUT_MILLIS) {
                stateMachine.startSync()
                publishState()
                val result = CompletableDeferred<HBandMetricSample>()
                val listener = IHeartDataListener { data: HeartData ->
                    if (data.heartStatus == EHeartStatus.STATE_HEART_NORMAL && data.data > 0) {
                        result.complete(HBandMetricSample(type, clock(), data.data.toDouble(), "bpm"))
                    }
                }
                withContext(Dispatchers.Main.immediate) { manager.startDetectHeart(writeResponse, listener) }
                try {
                    HBandPayload(measurements = listOf(result.await()))
                } finally {
                    withContext(Dispatchers.Main.immediate) { manager.stopDetectHeart(writeResponse) }
                    stateMachine.ready()
                    publishState()
                }
            }
        } catch (_: Exception) {
            if (manager.isCurrentDeviceConnected) stateMachine.recoverReady() else stateMachine.fail()
            publishState()
            HBandPayload()
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
        withContext(Dispatchers.Main.immediate) { manager.readSleepData(writeResponse, listener, TODAY) }
        complete.await()
        return HBandPayload(sleep = records)
    }

    private suspend fun readHeartHistory(): HBandPayload {
        val records = mutableListOf<HBandMetricSample>()
        val complete = CompletableDeferred<Unit>()
        val listener = object : IOriginDataListener {
            override fun onOringinFiveMinuteDataChange(data: OriginData?) {
                if (data != null && data.rateValue > 0) {
                    data.getmTime()?.toEpochMillis()?.let {
                        records += HBandMetricSample(RingMetricType.HEART_RATE, it, data.rateValue.toDouble(), "bpm")
                    }
                }
            }
            override fun onOringinHalfHourDataChange(data: OriginHalfHourData?) = Unit
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
                        heartRate = data.heartDetect.hasFunction(),
                        bloodOxygen = data.spo2H.hasFunction(),
                        hrv = data.hrvFunction.hasFunction(),
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

    private suspend fun resetAfterFailure() {
        runCatching { withContext(Dispatchers.Main.immediate) { manager.disconnectWatch(writeResponse) } }
        mutableConnectedDevice.value = null
        mutableCapabilities.value = HBandCapabilities()
        stateMachine.fail()
        publishState()
    }

    private data class PwdSnapshot(val success: Boolean, val deviceNumber: Int?, val firmwareVersion: String?)

    private companion object {
        const val DEFAULT_DEVICE_PASSWORD = "0000" // Official SDK demo default; physical-device QA is still required.
        const val TODAY = 0
        const val ORIGIN_PROTOCOL_TYPE = 3
        const val MAX_SCAN_RESULTS = 30
        const val SCAN_TIMEOUT_MILLIS = 10_000L
        const val CONNECT_TIMEOUT_MILLIS = 35_000L
        const val COMMAND_TIMEOUT_MILLIS = 8_000L
        const val HISTORY_TIMEOUT_MILLIS = 60_000L
        const val MEASUREMENT_TIMEOUT_MILLIS = 45_000L
        const val ONE_DAY_MILLIS = 86_400_000L
        const val METRES_PER_KILOMETRE = 1_000.0
        val PASSWORD_SUCCESS_STATES = setOf(EPwdStatus.CHECK_SUCCESS, EPwdStatus.CHECK_AND_TIME_SUCCESS)
        val PASSWORD_TERMINAL_STATES = PASSWORD_SUCCESS_STATES + setOf(EPwdStatus.CHECK_FAIL, EPwdStatus.UNKNOW)
        val writeResponse = IBleWriteResponse { }
    }
}
