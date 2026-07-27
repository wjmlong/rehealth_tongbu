package com.rehealth.genie.ring.rwfit

import android.content.Context
import com.example.blesdk.DHBleSdk
import com.example.blesdk.bean.function.SupportMenuBean
import com.example.blesdk.bean.function.FirmVersionBean
import com.example.blesdk.bean.sync.BloodOxySyncBean
import com.example.blesdk.bean.sync.BloodPressSyncBean
import com.example.blesdk.bean.sync.BloodSugarSyncBean
import com.example.blesdk.bean.sync.BodyTempSyncBean
import com.example.blesdk.bean.sync.BreatheSyncBean
import com.example.blesdk.bean.sync.HealthDataSyncBean
import com.example.blesdk.bean.sync.HeartRateSyncBean
import com.example.blesdk.bean.sync.HrvSyncBean
import com.example.blesdk.bean.sync.MuslimCountSyncBean
import com.example.blesdk.bean.sync.PressureSyncBean
import com.example.blesdk.bean.sync.SleepSyncBean
import com.example.blesdk.bean.sync.StepSyncBean
import com.example.blesdk.ble.ScanBleService
import com.example.blesdk.ble.bean.BleDevice
import com.example.blesdk.blering.RingBleError
import com.example.blesdk.blering.RingConnectBleCallback
import com.example.blesdk.callback.HealthDataSyncCallback
import com.example.blesdk.callback.ScanDeviceCallback
import com.example.blesdk.callback.data.HealthDataBroCallback
import com.example.blesdk.callback.data.FirmwareCallback
import com.example.blesdk.callback.status.HealthDataControlCallback
import com.example.blesdk.utils.CmdConstants
import com.example.blesdk.utils.Constants
import com.rehealth.genie.ring.RingBleGuards
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class RealRwFitSdkGateway(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : RwFitSdkGateway {
    private val appContext = context.applicationContext
    private val sdk = DHBleSdk
    private val scanService = ScanBleService.getService()
    private val scannedDevices = ConcurrentHashMap<String, BleDevice>()
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    private val mutableCapabilities = MutableStateFlow(RwFitCapabilities())

    @Volatile
    private var pendingConnection: CompletableDeferred<RwFitConnectionInfo?>? = null

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState.asStateFlow()
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice.asStateFlow()
    override val capabilities: StateFlow<RwFitCapabilities> = mutableCapabilities.asStateFlow()

    override suspend fun scan(): List<RingDevice> {
        if (!prepareBle()) return emptyList()
        mutableConnectionState.value = RingConnectionState.SCANNING
        val found = ConcurrentHashMap<String, RingDevice>()
        val finished = CompletableDeferred<Unit>()
        var scanFailed = false
        val callback = object : ScanDeviceCallback {
            override fun onScanDevice(device: BleDevice) {
                val mapped = device.toRingDevice() ?: return
                scannedDevices[mapped.address] = device
                found[mapped.address] = mapped
            }

            override fun onScanFinish() {
                finished.complete(Unit)
            }

            override fun onError(errorCode: Int, exception: Exception) {
                scanFailed = true
                finished.complete(Unit)
            }
        }
        withContext(Dispatchers.Main.immediate) {
            scanService.registerScanBleCallback(callback)
            try {
                scanService.stopScan()
                scanService.startScan(true, null)
                withTimeoutOrNull(SCAN_WINDOW_MILLIS) { finished.await() }
            } finally {
                scanService.stopScan()
                scanService.unRegisterScanBleCallback(callback)
            }
        }
        mutableConnectionState.value = when {
            scanFailed -> RingConnectionState.ERROR
            sdk.isBleConnected() -> RingConnectionState.CONNECTED
            else -> RingConnectionState.DISCONNECTED
        }
        return found.values.sortedByDescending { device -> device.rssi ?: Int.MIN_VALUE }.take(MAX_SCAN_RESULTS)
    }

    override suspend fun connect(device: RingDevice): RwFitConnectionInfo? {
        if (!prepareBle()) return null
        val sdkDevice = scannedDevices[device.address] ?: BleDevice().apply {
            bleMac = device.address
            bleName = device.name
        }
        val deferred = CompletableDeferred<RwFitConnectionInfo?>()
        pendingConnection?.cancel()
        pendingConnection = deferred
        mutableConnectionState.value = RingConnectionState.CONNECTING
        withContext(Dispatchers.Main.immediate) {
            sdk.connectDeviceWithModel(sdkDevice)
        }
        val result = withTimeoutOrNull(CONNECT_TIMEOUT_MILLIS) { deferred.await() }
        if (result == null && pendingConnection === deferred) {
            pendingConnection = null
            withContext(Dispatchers.Main.immediate) { sdk.disconnect() }
            mutableConnectedDevice.value = null
            mutableCapabilities.value = RwFitCapabilities()
            mutableConnectionState.value = RingConnectionState.ERROR
        }
        return result?.copy(firmwareVersion = readFirmwareVersion())
    }

    override suspend fun disconnect() {
        pendingConnection?.cancel()
        pendingConnection = null
        withContext(Dispatchers.Main.immediate) { sdk.disconnect() }
        mutableConnectedDevice.value = null
        mutableCapabilities.value = RwFitCapabilities()
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun syncSupported(): RwFitPayload {
        if (!sdk.isBleConnected()) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return RwFitPayload()
        }
        mutableConnectionState.value = RingConnectionState.SYNCING
        return try {
            val supported = mutableCapabilities.value
            var payload = RwFitPayload()
            if (supported.steps) payload += syncType(Constants.RingHealthType.STEP)
            if (supported.sleep) payload += syncType(Constants.RingHealthType.SLEEP)
            if (supported.heartRate) payload += syncType(Constants.RingHealthType.HR)
            if (supported.bloodOxygen) payload += syncType(Constants.RingHealthType.BLOOD_OXY)
            if (supported.hrv) payload += syncType(Constants.RingHealthType.HRV)
            mutableConnectionState.value = RingConnectionState.CONNECTED
            payload
        } catch (_: RwFitSdkException) {
            mutableConnectionState.value = RingConnectionState.ERROR
            RwFitPayload()
        }
    }

    override suspend fun measure(type: RingMetricType): RwFitPayload {
        val command = manualCommand(type) ?: return RwFitPayload()
        if (!sdk.isBleConnected() || type !in mutableCapabilities.value.supportedMetrics) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return RwFitPayload()
        }
        mutableConnectionState.value = RingConnectionState.SYNCING
        var latest = RwFitPayload()
        val completed = CompletableDeferred<Unit>()
        val dataCallback = object : HealthDataBroCallback {
            override fun onResult(data: HealthDataSyncBean) {
                val observedAt = clock()
                latest = when (type) {
                    RingMetricType.HEART_RATE,
                    RingMetricType.HRV,
                    -> data.hrPartData.orEmpty().lastOrNull()?.let { item ->
                        RwFitVendorDataMapper.realTime(type, item.time, item.hr, observedAt)
                    } ?: latest
                    RingMetricType.BLOOD_OXYGEN -> data.boPartData.orEmpty().lastOrNull()?.let { item ->
                        RwFitVendorDataMapper.realTime(type, item.time, item.bo, observedAt)
                    } ?: latest
                    else -> latest
                }
            }

            override fun onFail(errorCode: Int) {
                completed.completeExceptionally(RwFitSdkException("measurement failed: $errorCode"))
            }

            override fun onSuccess() = Unit
        }
        val controlCallback = object : HealthDataControlCallback {
            override fun onResult(data: Int) {
                if (data >= MEASUREMENT_COMPLETE_VALUE) completed.complete(Unit)
            }

            override fun onFail(errorCode: Int) {
                completed.completeExceptionally(RwFitSdkException("measurement control failed: $errorCode"))
            }

            override fun onSuccess() = Unit
        }
        return try {
            withContext(Dispatchers.Main.immediate) {
                sdk.subscribeData(dataCallback)
                sdk.subscribeData(controlCallback)
                sdk.controlHealthDataJL(command, START_MEASUREMENT)
            }
            val success = withTimeoutOrNull(MEASUREMENT_TIMEOUT_MILLIS) {
                completed.await()
                true
            } == true
            mutableConnectionState.value = if (success) RingConnectionState.CONNECTED else RingConnectionState.ERROR
            if (success) latest else RwFitPayload()
        } catch (_: RwFitSdkException) {
            mutableConnectionState.value = RingConnectionState.ERROR
            RwFitPayload()
        } finally {
            withContext(Dispatchers.Main.immediate) {
                sdk.controlHealthDataJL(command, STOP_MEASUREMENT)
                sdk.dispose(dataCallback)
                sdk.dispose(controlCallback)
            }
        }
    }

    private suspend fun syncType(type: Int): RwFitPayload {
        var payload = RwFitPayload()
        val completed = CompletableDeferred<RwFitPayload>()
        val callback = object : HealthDataSyncCallback {
            override fun onSyncProgress(progress: Int) = Unit

            override fun onSyncFinish() {
                completed.complete(payload)
            }

            override fun onSyncError(errorCode: Int) {
                completed.completeExceptionally(RwFitSdkException("history sync failed: $errorCode"))
            }

            override fun onSyncStep(data: List<StepSyncBean>) {
                if (type == Constants.RingHealthType.STEP) payload += RwFitVendorDataMapper.steps(data)
            }

            override fun onSyncSleep(data: List<SleepSyncBean>) {
                if (type == Constants.RingHealthType.SLEEP) payload += RwFitVendorDataMapper.sleep(data)
            }

            override fun onSyncHr(data: List<HeartRateSyncBean>) {
                if (type == Constants.RingHealthType.HR) payload += RwFitVendorDataMapper.heartRate(data)
            }

            override fun onSyncBo(data: List<BloodOxySyncBean>) {
                if (type == Constants.RingHealthType.BLOOD_OXY) {
                    payload += RwFitVendorDataMapper.bloodOxygen(data)
                }
            }

            override fun onSyncHrv(data: List<HrvSyncBean>) {
                if (type == Constants.RingHealthType.HRV) payload += RwFitVendorDataMapper.hrv(data)
            }

            override fun onSyncBp(data: List<BloodPressSyncBean>) = Unit
            override fun onSyncTemp(data: List<BodyTempSyncBean>) = Unit
            override fun onSyncPressure(data: List<PressureSyncBean>) = Unit
            override fun onSyncBloodSugar(data: List<BloodSugarSyncBean>) = Unit
            override fun onSyncBreath(data: List<BreatheSyncBean>) = Unit
            override fun onSyncMuslimCount(data: List<MuslimCountSyncBean>) = Unit
        }
        return try {
            withContext(Dispatchers.Main.immediate) {
                sdk.syncHealthDataByType(type, callback)
            }
            withTimeoutOrNull(HISTORY_TIMEOUT_MILLIS) { completed.await() }
                ?: throw RwFitSdkException("history sync timed out")
        } finally {
            withContext(Dispatchers.Main.immediate) { sdk.removeHealthDataCallBack(callback) }
        }
    }

    private suspend fun readFirmwareVersion(): String? {
        val completed = CompletableDeferred<String?>()
        val callback = object : FirmwareCallback {
            override fun onResult(data: FirmVersionBean) {
                completed.complete(data.uiVersion?.takeIf { it.isNotBlank() })
            }

            override fun onFail(errorCode: Int) {
                completed.complete(null)
            }

            override fun onSuccess() = Unit
        }
        return try {
            withContext(Dispatchers.Main.immediate) {
                sdk.subscribeData(callback)
                sdk.getFirmwareVersionJL()
            }
            withTimeoutOrNull(FIRMWARE_TIMEOUT_MILLIS) { completed.await() }
        } finally {
            withContext(Dispatchers.Main.immediate) { sdk.dispose(callback) }
        }
    }

    private fun prepareBle(): Boolean {
        mutableConnectionState.value = when {
            !RingBleGuards.isBluetoothAvailable(appContext) -> RingConnectionState.UNSUPPORTED
            !RingBleGuards.hasCollectionPermission(appContext) -> RingConnectionState.PERMISSION_REQUIRED
            !RingBleGuards.isBluetoothEnabled(appContext) -> RingConnectionState.BLUETOOTH_OFF
            else -> return true
        }
        return false
    }

    private val connectionCallback = object : RingConnectBleCallback {
        override fun onRingConnecting(device: BleDevice?) {
            mutableConnectionState.value = RingConnectionState.CONNECTING
        }

        override fun onRingConnected(device: BleDevice?) {
            // The SDK is not ready for business commands until function-menu data arrives.
            mutableConnectionState.value = RingConnectionState.CONNECTING
        }

        override fun onRingConnectFailed(device: BleDevice?, error: RingBleError) {
            val state = when (error) {
                RingBleError.MANUAL_DISCONNECT,
                RingBleError.DISCONNECTED,
                -> RingConnectionState.DISCONNECTED
                RingBleError.BLUETOOTH_DISABLED -> RingConnectionState.BLUETOOTH_OFF
                else -> RingConnectionState.ERROR
            }
            mutableConnectedDevice.value = null
            mutableCapabilities.value = RwFitCapabilities()
            mutableConnectionState.value = state
            pendingConnection?.complete(null)
            pendingConnection = null
        }

        override fun onRingDidFunctionMenu(device: BleDevice?, menu: SupportMenuBean) {
            val sdkDevice = device ?: run {
                mutableConnectionState.value = RingConnectionState.ERROR
                pendingConnection?.complete(null)
                pendingConnection = null
                return
            }
            val mapped = sdkDevice.toRingDevice() ?: run {
                mutableConnectionState.value = RingConnectionState.ERROR
                pendingConnection?.complete(null)
                pendingConnection = null
                return
            }
            val deviceCapabilities = menu.toCapabilities()
            val info = RwFitConnectionInfo(
                device = mapped,
                modelCode = sdkDevice.pidType.takeIf { it > 0 }?.let { "pid:$it" },
                firmwareVersion = null,
                capabilities = deviceCapabilities,
            )
            scannedDevices[mapped.address] = sdkDevice
            mutableConnectedDevice.value = mapped
            mutableCapabilities.value = deviceCapabilities
            mutableConnectionState.value = RingConnectionState.CONNECTED
            pendingConnection?.complete(info)
            pendingConnection = null
        }
    }

    init {
        sdk.initSDK(appContext)
        scanService.initBle(appContext)
        sdk.setConnectBleCallback(connectionCallback)
    }

    private fun BleDevice.toRingDevice(): RingDevice? {
        val address = bleMac?.takeIf { it.isNotBlank() } ?: return null
        return RingDevice(
            address = address,
            name = bleName?.takeIf { it.isNotBlank() },
            rssi = bleRssi.takeUnless { it == 0 },
        )
    }

    private fun SupportMenuBean.toCapabilities() = RwFitCapabilities(
        steps = isStep,
        sleep = isSleep,
        heartRate = isHr,
        bloodOxygen = isBloodOxy,
        hrv = isHrv,
    )

    private fun manualCommand(type: RingMetricType): Byte? = when (type) {
        RingMetricType.HEART_RATE -> CmdConstants.JL_HR_DATA_TRANSFER_KEY
        RingMetricType.BLOOD_OXYGEN -> CmdConstants.JL_BO_DATA_TRANSFER_KEY
        RingMetricType.HRV -> CmdConstants.JL_HRV_DATA_TRANSFER_KEY
        else -> null
    }

    private companion object {
        const val SCAN_WINDOW_MILLIS = 6_000L
        const val CONNECT_TIMEOUT_MILLIS = 20_000L
        const val FIRMWARE_TIMEOUT_MILLIS = 5_000L
        const val HISTORY_TIMEOUT_MILLIS = 45_000L
        const val MEASUREMENT_TIMEOUT_MILLIS = 45_000L
        const val MAX_SCAN_RESULTS = 20
        const val MEASUREMENT_COMPLETE_VALUE = 10
        const val START_MEASUREMENT: Byte = 1
        const val STOP_MEASUREMENT: Byte = 0
    }
}

private class RwFitSdkException(message: String) : RuntimeException(message)
