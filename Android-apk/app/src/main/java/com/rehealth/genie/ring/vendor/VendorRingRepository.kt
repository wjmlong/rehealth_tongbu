package com.rehealth.genie.ring.vendor

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.rehealth.genie.ring.RequiredRingMetrics
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ring repository backed by the vendor connection stack copied verbatim from the
 * MRD SDK demo ([BleAdapter], [SearchBle], [BleTool]). It fulfils the same
 * [RingRepository] contract the app already uses, so the ViewModel/UI layers stay
 * unchanged. Protocol packet creation/parsing is delegated to [MrdProtocolAdapter].
 */
class VendorRingRepository(
    context: Context,
    private val dao: RingDataDao,
    private val protocol: MrdProtocolAdapter,
) : RingRepository {
    private val appContext = context.applicationContext
    private val bleTool = BleTool(appContext)
    private val bleAdapter = BleAdapter(appContext)
    private val searchBle = SearchBle.getInstance(appContext)

    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = RequiredRingMetrics

    private val packets = mutableListOf<ByteArray>()
    private val packetsLock = Any()
    private val connectionMutex = Mutex()
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var autoReconnectEnabled = false
    private var servicesReady = false
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val legacyPreferences = appContext.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        migrateLegacyBinding()
        autoReconnectEnabled = preferences.getBoolean(AUTO_RECONNECT_ENABLED, false)
        bleAdapter.setListener(object : BluetoothStateListener {
            override fun onChange(device: BluetoothDevice?, state: BleState) {
                onConnectionChange(device, state)
            }

            override fun onReadChange(device: BluetoothDevice?, datas: ByteArray?) {
                val value = datas ?: return
                synchronized(packetsLock) { packets.add(value.copyOf()) }
                runCatching { protocol.parse(value) }
                    .onSuccess { Log.i(TAG, "read enum=${it.mrdReadEnum} status=${it.status} bytes=${value.size}") }
                    .onFailure { Log.w(TAG, "read parse failed bytes=${value.size}", it) }
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun onConnectionChange(device: BluetoothDevice?, state: BleState) {
        when (state) {
            BleState.CONNECTING -> mutableConnectionState.value = RingConnectionState.CONNECTING
            BleState.CONNECTED -> mutableConnectionState.value = RingConnectionState.CONNECTING
            BleState.SERVICES_DISCOVERED -> {
                servicesReady = true
                mutableConnectionState.value = RingConnectionState.CONNECTED
                val address = device?.address ?: savedAddress()
                if (address != null) {
                    val name = if (hasBlePermission()) {
                        runCatching { device?.name }.getOrNull()
                    } else {
                        null
                    }
                        ?: preferences.getString(SAVED_NAME, "MRD 智能戒指")
                    val resolved = RingDevice(address, name, null)
                    mutableConnectedDevice.value = resolved
                    saveDevice(resolved)
                }
            }
            BleState.DISCONNECTED -> {
                servicesReady = false
                mutableConnectedDevice.value = null
                mutableConnectionState.value = RingConnectionState.DISCONNECTED
                scheduleReconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun scan(): List<RingDevice> = withContext(Dispatchers.Main) {
        if (!hasBlePermission()) {
            mutableConnectionState.value = RingConnectionState.PERMISSION_REQUIRED
            return@withContext emptyList()
        }
        mutableConnectionState.value = RingConnectionState.SCANNING
        val found = linkedMapOf<String, RingDevice>()
        val listener = SearchListener.ScanListener { device, rssi ->
            val name = if (hasBlePermission()) runCatching { device.name }.getOrNull() else null
            val address = device.address ?: return@ScanListener
            val isKnownDevice = address.equals(savedAddress(), ignoreCase = true)
            val isMrdName = name?.contains("MR11", ignoreCase = true) == true ||
                name?.contains("MRD", ignoreCase = true) == true
            if (isKnownDevice || isMrdName || !name.isNullOrBlank()) {
                val displayName = name ?: "未知 BLE 设备"
                found[address] = RingDevice(address, displayName, rssi)
                Log.i(TAG, "scan candidate name=$name rssi=$rssi")
            }
        }
        searchBle.addListener(listener)
        val started = searchBle.search()
        if (!started) {
            searchBle.removeListener(listener)
            mutableConnectionState.value = RingConnectionState.BLUETOOTH_OFF
            return@withContext emptyList()
        }
        delay(6_000)
        searchBle.removeListener(listener)
        searchBle.stop()
        if (mutableConnectionState.value == RingConnectionState.SCANNING) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
        }
        found.values.sortedWith(
            compareByDescending<RingDevice> { device ->
                val name = device.name.orEmpty()
                device.address.equals(savedAddress(), ignoreCase = true) ||
                    name.contains("MR11", ignoreCase = true) ||
                    name.contains("MRD", ignoreCase = true) ||
                    name.contains("ring", ignoreCase = true)
            }.thenByDescending { it.rssi ?: -999 },
        ).take(12)
    }

    override suspend fun connect(device: RingDevice) = withContext(Dispatchers.Main) {
        if (!hasBlePermission()) {
            mutableConnectionState.value = RingConnectionState.PERMISSION_REQUIRED
            return@withContext
        }
        reconnectJob?.cancel()
        autoReconnectEnabled = true
        connectionMutex.withLock { connectInternal(device) }
        Unit
    }

    override suspend fun autoConnect(): Boolean = withContext(Dispatchers.Main) {
        if (!hasBlePermission()) {
            mutableConnectionState.value = RingConnectionState.PERMISSION_REQUIRED
            return@withContext false
        }
        if (isReady()) return@withContext true
        autoReconnectEnabled = true
        val connected = connectionMutex.withLock {
            if (isReady()) return@withLock true
            val saved = savedDevice()
            if (saved != null && connectInternal(saved)) return@withLock true
            val candidate = scan().firstOrNull() ?: return@withLock false
            connectInternal(candidate)
        }
        if (!connected) scheduleReconnect()
        connected
    }

    override suspend fun disconnect() = withContext(Dispatchers.Main) {
        autoReconnectEnabled = false
        reconnectJob?.cancel()
        preferences.edit().putBoolean(AUTO_RECONNECT_ENABLED, false).apply()
        bleAdapter.close()
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun syncAll(): RingSyncResult = withContext(Dispatchers.Main) {
        if (!isReady()) autoConnect()
        if (!isReady()) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return@withContext RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        clearPackets()
        mutableConnectionState.value = RingConnectionState.SYNCING
        val readCommands = listOf(
            RingMetricType.SLEEP,
            RingMetricType.STEPS,
            RingMetricType.HEART_RATE,
            RingMetricType.HRV,
            RingMetricType.BLOOD_OXYGEN,
            RingMetricType.BLOOD_PRESSURE,
            RingMetricType.TEMPERATURE,
            RingMetricType.STRESS,
        )
        readCommands.forEach { type ->
            bleAdapter.LostWriteData(protocol.latestCommand(type))
            delay(450)
        }
        delay(2_000)
        persistPackets()
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult = withContext(Dispatchers.Main) {
        if (type == RingMetricType.TEMPERATURE) {
            return@withContext readTemperature()
        }
        val command = protocol.manualTestCommand(type)
        if (command == null) {
            Log.w(TAG, "manual measure unsupported type=$type")
            return@withContext RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        if (!isReady()) autoConnect()
        if (!isReady()) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return@withContext RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        clearPackets()
        mutableConnectionState.value = RingConnectionState.SYNCING
        writeWithRetry(command)
        delay(8_000)
        writeWithRetry(protocol.stopManualTestCommand())
        delay(1_000)
        writeWithRetry(protocol.latestCommand(type))
        delay(800)
        protocol.historyCountCommand(type)?.let { historyCommand ->
            writeWithRetry(historyCommand)
            delay(450)
        }
        delay(1_200)
        persistPackets()
    }

    override suspend fun sendCommand(data: ByteArray): Boolean = withContext(Dispatchers.Main) {
        if (!isReady()) autoConnect()
        if (!isReady()) return@withContext false
        writeWithRetry(data)
    }

    private suspend fun readTemperature(): RingSyncResult {
        if (!isReady()) autoConnect()
        if (!isReady()) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        clearPackets()
        mutableConnectionState.value = RingConnectionState.SYNCING
        writeWithRetry(protocol.enableTimingTemperatureCommand())
        delay(900)
        writeWithRetry(protocol.getTimingTemperatureCommand())
        delay(900)
        protocol.temperatureReadCommands().forEach { command ->
            writeWithRetry(command)
            delay(900)
        }
        delay(1_800)
        return persistPackets()
    }

    private suspend fun writeWithRetry(data: ByteArray): Boolean {
        if (bleAdapter.LostWriteData(data)) return true
        delay(700)
        Log.i(TAG, "write retry bytes=${data.size}")
        return bleAdapter.LostWriteData(data)
    }

    private suspend fun persistPackets(): RingSyncResult {
        val now = System.currentTimeMillis()
        val packetSnapshot = packetSnapshot()
        val parsedPackets = packetSnapshot.mapNotNull { packet ->
            runCatching { protocol.parse(packet) to packet }
                .onSuccess { (request, _) ->
                    Log.i(TAG, "parsed enum=${request.mrdReadEnum} status=${request.status}")
                }
                .onFailure { Log.w(TAG, "parse failed bytes=${packet.size}", it) }
                .getOrNull()
        }
        val parsedBatch = protocol.toDataBatch(parsedPackets, now)
        val rawChunks = packetSnapshot.mapIndexed { index, packet ->
            RingSignalChunkEntity(
                id = "mrd_raw_${now}_$index",
                signalType = "MRD_RAW",
                startedAt = now,
                sampleRateHz = null,
                sampleCount = packet.size,
                encoding = "RAW",
                payload = packet,
                source = "mrd_ring",
            )
        }
        dao.insertBatch(
            RingDataBatch(
                measurements = parsedBatch.measurements,
                sleepSessions = parsedBatch.sleepSessions,
                activities = parsedBatch.activities,
                signalChunks = rawChunks,
            ),
        )
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(
            collectedTypes = parsedBatch.measurements.mapNotNull { record ->
                runCatching { RingMetricType.valueOf(record.metricType) }.getOrNull()
            }.toSet() + if (parsedBatch.sleepSessions.isNotEmpty()) setOf(RingMetricType.SLEEP) else emptySet(),
            recordsWritten = parsedBatch.size + rawChunks.size,
            completedAt = now,
        )
    }

    private suspend fun connectInternal(device: RingDevice): Boolean {
        val remote = runCatching { bleTool.GetAdapter()?.getRemoteDevice(device.address) }.getOrNull()
        if (remote == null) {
            mutableConnectionState.value = RingConnectionState.ERROR
            return false
        }
        bleAdapter.close()
        clearPackets()
        servicesReady = false
        mutableConnectionState.value = RingConnectionState.CONNECTING
        if (!bleAdapter.connect(remote)) {
            mutableConnectionState.value = RingConnectionState.ERROR
            return false
        }
        val ok = withTimeoutOrNull(CONNECT_TIMEOUT_MS) {
            while (!servicesReady) delay(120)
            true
        } == true
        if (!ok) {
            bleAdapter.close()
            mutableConnectionState.value = RingConnectionState.ERROR
        } else {
            saveDevice(device)
        }
        return ok
    }

    private fun scheduleReconnect() {
        if (!autoReconnectEnabled || reconnectJob?.isActive == true) return
        val device = savedDevice() ?: return
        reconnectJob = reconnectScope.launch {
            listOf(1_000L, 3_000L, 10_000L).forEach { delayMillis ->
                delay(delayMillis)
                if (!autoReconnectEnabled || isReady()) return@launch
                val connected = connectionMutex.withLock {
                    if (isReady()) true else connectInternal(device)
                }
                if (connected) return@launch
            }
        }
    }

    private fun isReady(): Boolean =
        servicesReady && mutableConnectionState.value == RingConnectionState.CONNECTED

    private fun clearPackets() = synchronized(packetsLock) { packets.clear() }

    private fun packetSnapshot(): List<ByteArray> = synchronized(packetsLock) { packets.map(ByteArray::copyOf) }

    private fun saveDevice(device: RingDevice) {
        preferences.edit()
            .putString(SAVED_ADDRESS, device.address)
            .putString(SAVED_NAME, device.name)
            .putBoolean(AUTO_RECONNECT_ENABLED, true)
            .apply()
    }

    private fun migrateLegacyBinding() {
        if (preferences.contains(SAVED_ADDRESS)) return
        val legacyAddress = legacyPreferences.getString(LEGACY_SAVED_ADDRESS, null) ?: return
        preferences.edit()
            .putString(SAVED_ADDRESS, legacyAddress)
            .putString(SAVED_NAME, "已绑定的 MRD 戒指")
            .putBoolean(AUTO_RECONNECT_ENABLED, true)
            .apply()
    }

    private fun savedAddress(): String? = preferences.getString(SAVED_ADDRESS, null)

    private fun savedDevice(): RingDevice? = savedAddress()?.let { address ->
        RingDevice(address, preferences.getString(SAVED_NAME, "MRD 智能戒指"), null)
    }

    private fun hasBlePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private companion object {
        const val TAG = "VendorRingRepository"
        const val PREFERENCES_NAME = "rehealth_ring_connection"
        const val LEGACY_PREFERENCES_NAME = "ring_connection"
        const val SAVED_ADDRESS = "saved_gatt_address"
        const val SAVED_NAME = "saved_gatt_name"
        const val LEGACY_SAVED_ADDRESS = "last_ring_address"
        const val AUTO_RECONNECT_ENABLED = "auto_reconnect_enabled"
        const val CONNECT_TIMEOUT_MS = 15_000L
    }
}
