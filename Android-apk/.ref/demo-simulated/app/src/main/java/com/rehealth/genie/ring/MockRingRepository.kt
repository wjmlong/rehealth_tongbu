package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MockRingRepository(
    private val dao: RingDataDao,
) : RingRepository {
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = RequiredRingMetrics

    override suspend fun scan(): List<RingDevice> {
        mutableConnectionState.value = RingConnectionState.SCANNING
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
        return listOf(RingDevice("MOCK:RING:01", "睿禾智能戒指（模拟）", -42))
    }

    override suspend fun connect(device: RingDevice) {
        mutableConnectionState.value = RingConnectionState.CONNECTING
        mutableConnectedDevice.value = device
        mutableConnectionState.value = RingConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun syncAll(): RingSyncResult {
        mutableConnectionState.value = RingConnectionState.SYNCING
        val now = System.currentTimeMillis()
        val source = "ring_mock"
        val measurements = listOf(
            measurement(RingMetricType.HEART_RATE, now, 72.0, "bpm", source),
            measurement(RingMetricType.HRV, now, 48.0, "ms", source),
            measurement(RingMetricType.BLOOD_OXYGEN, now, 98.0, "%", source),
            measurement(RingMetricType.BLOOD_PRESSURE, now, 118.0, "mmHg", source, 76.0),
            measurement(RingMetricType.TEMPERATURE, now, 36.5, "°C", source),
            measurement(RingMetricType.STEPS, now, 6842.0, "steps", source),
            measurement(RingMetricType.STRESS, now, 32.0, "score", source),
        )
        val sleep = RingSleepSessionEntity(
            id = id("sleep"),
            startedAt = now - 8 * 60 * 60 * 1000,
            endedAt = now,
            deepMinutes = 102,
            lightMinutes = 286,
            awakeMinutes = 26,
            remMinutes = 58,
            interruptionMinutes = 8,
            source = source,
        )
        val activity = RingActivityEntity(
            id = id("activity"),
            startedAt = now - 35 * 60 * 1000,
            endedAt = now,
            activityType = "walking",
            steps = 3200,
            distanceMeters = 2450.0,
            caloriesKcal = 126.0,
            durationMinutes = 35,
            averageHeartRate = 104.0,
            source = source,
        )
        val rri = intArrayOf(812, 806, 821, 798, 815, 809, 824, 817)
        val ppg = IntArray(250) { index -> 1800 + ((index % 25) * 16) }
        val signals = listOf(
            RingSignalChunkEntity(
                id = id("rri"),
                signalType = RingMetricType.RRI.name,
                startedAt = now,
                sampleRateHz = null,
                sampleCount = rri.size,
                payload = SignalEncoding.int32LittleEndian(rri),
                source = source,
            ),
            RingSignalChunkEntity(
                id = id("ppg"),
                signalType = RingMetricType.PPG.name,
                startedAt = now,
                sampleRateHz = 25,
                sampleCount = ppg.size,
                payload = SignalEncoding.int32LittleEndian(ppg),
                source = source,
            ),
        )
        val batch = RingDataBatch(
            measurements = measurements,
            sleepSessions = listOf(sleep),
            activities = listOf(activity),
            signalChunks = signals,
        )
        dao.insertBatch(batch)
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(RequiredRingMetrics, batch.size, now)
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult {
        mutableConnectionState.value = RingConnectionState.SYNCING
        val now = System.currentTimeMillis()
        val source = "ring_mock"
        val record = when (type) {
            RingMetricType.HEART_RATE -> measurement(type, now, 72.0, "bpm", source)
            RingMetricType.BLOOD_OXYGEN -> measurement(type, now, 98.0, "%", source)
            RingMetricType.BLOOD_PRESSURE -> measurement(type, now, 118.0, "mmHg", source, 76.0)
            RingMetricType.TEMPERATURE -> measurement(type, now, 36.5, "°C", source)
            RingMetricType.HRV -> measurement(type, now, 48.0, "ms", source)
            else -> null
        }
        val batch = RingDataBatch(measurements = listOfNotNull(record))
        dao.insertBatch(batch)
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(if (record == null) emptySet() else setOf(type), batch.size, now)
    }

    private fun measurement(
        type: RingMetricType,
        measuredAt: Long,
        value: Double,
        unit: String,
        source: String,
        secondaryValue: Double? = null,
    ) = RingMeasurementEntity(
        id = id(type.name.lowercase()),
        metricType = type.name,
        measuredAt = measuredAt,
        primaryValue = value,
        secondaryValue = secondaryValue,
        unit = unit,
        source = source,
    )

    private fun id(prefix: String): String = "${prefix}_${UUID.randomUUID()}"
}
