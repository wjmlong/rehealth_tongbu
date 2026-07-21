package com.rehealth.genie.ring.mrd

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.rehealth.genie.ring.RequiredRingMetrics
import com.rehealth.genie.ring.RingBleGuards
import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.RingSyncResult
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class MrdBleRingRepository(
    private val context: Context,
    private val dao: RingDataDao,
    private val protocol: MrdProtocolAdapter,
) : RingRepository {
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter?
        get() = manager.adapter

    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = RequiredRingMetrics

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private val packets = mutableListOf<ByteArray>()
    private var connectReady: CompletableDeferred<Boolean>? = null

    override suspend fun scan(): List<RingDevice> = withContext(Dispatchers.Main) {
        if (!hasBlePermission()) {
            mutableConnectionState.value = RingConnectionState.PERMISSION_REQUIRED
            return@withContext emptyList()
        }
        val bluetoothAdapter = adapter ?: run {
            mutableConnectionState.value = RingConnectionState.UNSUPPORTED
            return@withContext emptyList()
        }
        if (!bluetoothAdapter.isEnabled) {
            mutableConnectionState.value = RingConnectionState.BLUETOOTH_OFF
            return@withContext emptyList()
        }

        mutableConnectionState.value = RingConnectionState.SCANNING
        val found = linkedMapOf<String, RingDevice>()
        val callback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            val name = runCatching { device.name }.getOrNull()
            val advertisesMrd = scanRecord?.containsUuid(WRITE_SERVICE_UUID) == true
            if (advertisesMrd || !name.isNullOrBlank() || rssi >= -88) {
                val displayName = when {
                    advertisesMrd && name.isNullOrBlank() -> "MRD 戒指候选（无名称）"
                    advertisesMrd -> "$name · MRD"
                    name.isNullOrBlank() -> "未知 BLE 设备"
                    else -> name
                }
                found[device.address] = RingDevice(device.address, displayName, rssi)
                Log.i(TAG, "scan ${device.address} $displayName $rssi adv=${scanRecord?.toHex()}")
            }
        }
        bluetoothAdapter.stopLeScan(callback)
        bluetoothAdapter.startLeScan(callback)
        delay(6_000)
        bluetoothAdapter.stopLeScan(callback)
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
        found.values.sortedWith(
            compareByDescending<RingDevice> { device ->
                val name = device.name.orEmpty()
                name.contains("MR11", ignoreCase = true) ||
                    name.contains("MRD", ignoreCase = true) ||
                    device.address.equals(KNOWN_RING_ADDRESS, ignoreCase = true)
            }.thenByDescending { it.rssi ?: -999 },
        ).take(12)
    }

    override suspend fun connect(device: RingDevice) = withContext(Dispatchers.Main) {
        if (!hasBlePermission()) {
            mutableConnectionState.value = RingConnectionState.PERMISSION_REQUIRED
            return@withContext
        }
        val remote = adapter?.getRemoteDevice(device.address) ?: run {
            mutableConnectionState.value = RingConnectionState.UNSUPPORTED
            return@withContext
        }
        closeGatt()
        packets.clear()
        mutableConnectionState.value = RingConnectionState.CONNECTING
        connectReady = CompletableDeferred()
        gatt = remote.connectGatt(context, false, callback)
        val ok = withTimeoutOrNull(15_000) { connectReady?.await() } == true
        mutableConnectedDevice.value = if (ok) device else null
        mutableConnectionState.value = if (ok) RingConnectionState.CONNECTED else RingConnectionState.ERROR
    }

    override suspend fun disconnect() = withContext(Dispatchers.Main) {
        closeGatt()
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun autoConnect(): Boolean = withContext(Dispatchers.Main) {
        if (mutableConnectionState.value == RingConnectionState.CONNECTED && gatt != null) return@withContext true
        connect(RingDevice(KNOWN_RING_ADDRESS, "MR11 智能戒指", null))
        mutableConnectionState.value == RingConnectionState.CONNECTED && gatt != null
    }

    override suspend fun sendCommand(data: ByteArray): Boolean = withContext(Dispatchers.Main) {
        val writer = writeCharacteristic
        val currentGatt = gatt
        if (writer == null || currentGatt == null) return@withContext false
        writeWithRetry(currentGatt, writer, data)
    }

    override suspend fun syncAll(): RingSyncResult = withContext(Dispatchers.Main) {
        if (writeCharacteristic == null || gatt == null) {
            Log.i(TAG, "sync reconnecting")
            connect(RingDevice(KNOWN_RING_ADDRESS, "MR11 鏅鸿兘鎴掓寚", null))
            delay(800)
        }
        val writer = writeCharacteristic
        val currentGatt = gatt
        if (writer == null || currentGatt == null) {
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return@withContext RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        packets.clear()
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
            write(currentGatt, writer, protocol.latestCommand(type))
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
        if (writeCharacteristic == null || gatt == null) {
            Log.i(TAG, "manual measure reconnecting before type=$type")
            connect(RingDevice(KNOWN_RING_ADDRESS, "MR11 智能戒指", null))
            delay(800)
        }
        val writer = writeCharacteristic
        val currentGatt = gatt
        if (writer == null || currentGatt == null) {
            Log.w(TAG, "manual measure no active gatt type=$type")
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return@withContext RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        Log.i(TAG, "manual measure start type=$type")
        packets.clear()
        mutableConnectionState.value = RingConnectionState.SYNCING
        writeWithRetry(currentGatt, writer, command)
        delay(if (type == RingMetricType.TEMPERATURE) 12_000 else 8_000)
        writeWithRetry(currentGatt, writer, protocol.stopManualTestCommand())
        delay(1_000)
        writeWithRetry(currentGatt, writer, protocol.latestCommand(type))
        delay(800)
        protocol.historyCountCommand(type)?.let { historyCommand ->
            writeWithRetry(currentGatt, writer, historyCommand)
            delay(450)
        }
        delay(1_200)
        persistPackets()
    }

    private suspend fun readTemperature(): RingSyncResult {
        if (writeCharacteristic == null || gatt == null) {
            Log.i(TAG, "temperature read reconnecting")
            connect(RingDevice(KNOWN_RING_ADDRESS, "MR11 智能戒指", null))
            delay(800)
        }
        val writer = writeCharacteristic
        val currentGatt = gatt
        if (writer == null || currentGatt == null) {
            Log.w(TAG, "temperature read no active gatt")
            mutableConnectionState.value = RingConnectionState.DISCONNECTED
            return RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        Log.i(TAG, "temperature read start")
        packets.clear()
        mutableConnectionState.value = RingConnectionState.SYNCING
        writeWithRetry(currentGatt, writer, protocol.enableTimingTemperatureCommand())
        delay(900)
        writeWithRetry(currentGatt, writer, protocol.getTimingTemperatureCommand())
        delay(900)
        protocol.temperatureReadCommands().forEach { command ->
            writeWithRetry(currentGatt, writer, command)
            delay(900)
        }
        delay(1_800)
        return persistPackets()
    }

    @SuppressLint("MissingPermission")
    private fun write(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, data: ByteArray): Boolean {
        characteristic.value = data
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val ok = gatt.writeCharacteristic(characteristic)
        Log.i(TAG, "write ok=$ok data=${data.toHex()}")
        return ok
    }

    private suspend fun writeWithRetry(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
    ): Boolean {
        if (write(gatt, characteristic, data)) return true
        delay(700)
        Log.i(TAG, "write retry data=${data.toHex()}")
        return write(gatt, characteristic, data)
    }

    private suspend fun persistPackets(): RingSyncResult {
        val now = System.currentTimeMillis()
        val parsedPackets = packets.mapNotNull { packet ->
            runCatching { protocol.parse(packet) to packet }
                .onSuccess { (request, _) ->
                    Log.i(TAG, "parsed enum=${request.mrdReadEnum} status=${request.status} json=${request.json}")
                }
                .onFailure { Log.w(TAG, "parse failed raw=${packet.toHex()}", it) }
                .getOrNull()
        }
        val parsedBatch = protocol.toDataBatch(parsedPackets, now)
        val rawChunks = packets.mapIndexed { index, packet ->
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

    private val callback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i(TAG, "connection status=$status state=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mutableConnectionState.value = RingConnectionState.CONNECTED
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mutableConnectedDevice.value = null
                mutableConnectionState.value = RingConnectionState.DISCONNECTED
                connectReady?.complete(false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            Log.i(TAG, "services status=$status")
            val service = gatt.getService(WRITE_SERVICE_UUID)
            gatt.services.forEach { svc ->
                Log.i(TAG, "service ${svc.uuid} chars=${svc.characteristics.joinToString { it.uuid.toString() }}")
            }
            writeCharacteristic = service?.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
            enableNotify(gatt, service)
            connectReady?.complete(writeCharacteristic != null)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            packets.add(value.copyOf())
            runCatching { protocol.parse(value) }
                .onSuccess { Log.i(TAG, "read enum=${it.mrdReadEnum} status=${it.status} json=${it.json} raw=${value.toHex()}") }
                .onFailure { Log.w(TAG, "read raw=${value.toHex()}", it) }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            Log.i(TAG, "write status=$status")
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotify(gatt: BluetoothGatt, service: BluetoothGattService?) {
        val notify = service?.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID) ?: return
        gatt.setCharacteristicNotification(notify, true)
        val descriptor = notify.getDescriptor(CLIENT_CONFIG_UUID) ?: return
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt.writeDescriptor(descriptor)
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        writeCharacteristic = null
        mutableConnectedDevice.value = null
    }

    private fun hasBlePermission(): Boolean {
        return RingBleGuards.hasCollectionPermission(context)
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    private fun ByteArray.containsUuid(uuid: UUID): Boolean {
        val target = uuid.toString().lowercase()
        return parseAdvertisedUuids().any { it.toString().lowercase() == target }
    }

    private fun ByteArray.parseAdvertisedUuids(): List<UUID> {
        val uuids = mutableListOf<UUID>()
        var index = 0
        while (index < size) {
            val length = this[index].toInt() and 0xFF
            if (length == 0 || index + length >= size) break
            val type = this[index + 1].toInt() and 0xFF
            val dataStart = index + 2
            val dataEnd = index + 1 + length
            when (type) {
                0x06, 0x07 -> {
                    var offset = dataStart
                    while (offset + 15 <= dataEnd) {
                        uuids += uuidFromLittleEndian128(copyOfRange(offset, offset + 16))
                        offset += 16
                    }
                }
            }
            index += length + 1
        }
        return uuids
    }

    private fun uuidFromLittleEndian128(bytes: ByteArray): UUID {
        val msb = ((bytes[15].toLong() and 0xff) shl 56) or
            ((bytes[14].toLong() and 0xff) shl 48) or
            ((bytes[13].toLong() and 0xff) shl 40) or
            ((bytes[12].toLong() and 0xff) shl 32) or
            ((bytes[11].toLong() and 0xff) shl 24) or
            ((bytes[10].toLong() and 0xff) shl 16) or
            ((bytes[9].toLong() and 0xff) shl 8) or
            (bytes[8].toLong() and 0xff)
        val lsb = ((bytes[7].toLong() and 0xff) shl 56) or
            ((bytes[6].toLong() and 0xff) shl 48) or
            ((bytes[5].toLong() and 0xff) shl 40) or
            ((bytes[4].toLong() and 0xff) shl 32) or
            ((bytes[3].toLong() and 0xff) shl 24) or
            ((bytes[2].toLong() and 0xff) shl 16) or
            ((bytes[1].toLong() and 0xff) shl 8) or
            (bytes[0].toLong() and 0xff)
        return UUID(msb, lsb)
    }

    private companion object {
        const val TAG = "MrdBleRingRepository"
        const val KNOWN_RING_ADDRESS = "D9:18:68:41:00:C6"
        val WRITE_SERVICE_UUID: UUID = UUID.fromString("f000efe0-0451-4000-0000-00000000b000")
        val WRITE_CHARACTERISTIC_UUID: UUID = UUID.fromString("f000efe1-0451-4000-0000-00000000b000")
        val NOTIFY_CHARACTERISTIC_UUID: UUID = UUID.fromString("f000efe3-0451-4000-0000-00000000b000")
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }
}
