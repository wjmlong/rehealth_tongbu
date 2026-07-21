package com.rehealth.genie.ring

import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Offline stand-in for a real ring device. Implements [RingRepository] so the UI/VM keep
 * the same API surface when no Bluetooth ring is paired (Requirement C: keep the API
 * interface). Instead of random numbers, it simulates a *deterministic, physiologically
 * plausible* signal stream computed from an optional patient [profile] (age, hypertension
 * history, BMI) plus smooth time-based variation. Repeated syncs are stable and trend
 * realistically — the values are computed, never arbitrary — so the dashboard renders
 * simulated data derived from real profile inputs.
 *
 * Conservative, non-diagnostic: these are clearly simulated and must not be shown as
 * measured clinical values.
 */
class MockRingRepository(
    private val dao: RingDataDao,
) : RingRepository {
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    private var baselineSeeded = false

    /** Patient profile used to parameterize the simulated vitals. Set by the VM when a real profile is available. */
    var profile: BaselineHealthProfile? = null
        set(value) { field = value }

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = RequiredRingMetrics

    override suspend fun scan(): List<RingDevice> {
        mutableConnectionState.value = RingConnectionState.SCANNING
        delay(700)
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
        return listOf(
            RingDevice("MOCK:RING:01", "睿禾智能戒指 Mock", -42),
            RingDevice("MOCK:RING:02", "MRD Dev Ring Replay", -63),
        )
    }

    override suspend fun connect(device: RingDevice) {
        mutableConnectionState.value = RingConnectionState.CONNECTING
        delay(450)
        mutableConnectedDevice.value = device
        mutableConnectionState.value = RingConnectionState.CONNECTED
        seedBaselineIfNeeded()
    }

    override suspend fun disconnect() {
        mutableConnectedDevice.value = null
        mutableConnectionState.value = RingConnectionState.DISCONNECTED
    }

    override suspend fun autoConnect(): Boolean {
        if (mutableConnectionState.value == RingConnectionState.CONNECTED) return true
        connect(RingDevice("MOCK:RING:01", "睿禾智能戒指 Mock", -42))
        return mutableConnectionState.value == RingConnectionState.CONNECTED
    }

    override suspend fun sendCommand(data: ByteArray): Boolean {
        // Mock repository: commands are no-ops (no real BLE transport).
        return true
    }

    override suspend fun syncAll(): RingSyncResult {
        mutableConnectionState.value = RingConnectionState.SYNCING
        delay(900)
        seedBaselineIfNeeded()
        val now = System.currentTimeMillis()
        val batch = generateCurrentBatch(now)
        dao.insertBatch(batch)
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(
            collectedTypes = setOf(
                RingMetricType.SLEEP,
                RingMetricType.BLOOD_PRESSURE,
                RingMetricType.TEMPERATURE,
                RingMetricType.HEART_RATE,
                RingMetricType.STEPS,
                RingMetricType.BLOOD_OXYGEN,
                RingMetricType.HRV,
                RingMetricType.STRESS,
                RingMetricType.RRI,
                RingMetricType.PPG,
            ),
            recordsWritten = batch.size,
            completedAt = now,
        )
    }

    override suspend fun measure(type: RingMetricType): RingSyncResult {
        mutableConnectionState.value = RingConnectionState.SYNCING
        delay(1_200)
        val now = System.currentTimeMillis()
        val (hr, sbp, dbp) = baselines()
        val batch = when (type) {
            RingMetricType.HEART_RATE -> RingDataBatch(
                measurements = listOf(measurement(type, now, wobble(hr, now, 4.0), "bpm")),
            )
            RingMetricType.HRV -> RingDataBatch(
                measurements = listOf(measurement(type, now, wobble(50.0 + (hr - 74) * -0.6, now, 6.0), "ms")),
            )
            RingMetricType.BLOOD_OXYGEN -> RingDataBatch(
                measurements = listOf(measurement(type, now, wobble(98.0, now, 1.2).coerceIn(94.0, 99.0), "%")),
            )
            RingMetricType.BLOOD_PRESSURE -> RingDataBatch(
                measurements = listOf(
                    measurement(
                        type = type,
                        measuredAt = now,
                        value = wobble(sbp, now, 5.0),
                        unit = "mmHg",
                        secondaryValue = wobble(dbp, now, 3.0),
                    ),
                ),
            )
            RingMetricType.TEMPERATURE -> RingDataBatch(
                measurements = listOf(measurement(type, now, wobble(36.5, now, 0.15), "°C")),
            )
            RingMetricType.RRI -> RingDataBatch(signalChunks = listOf(rriSignal(now)))
            RingMetricType.PPG -> RingDataBatch(signalChunks = listOf(ppgSignal(now)))
            else -> RingDataBatch()
        }
        dao.insertBatch(batch)
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(
            collectedTypes = if (batch.size > 0) setOf(type) else emptySet(),
            recordsWritten = batch.size,
            completedAt = now,
        )
    }

    // ---- deterministic physiologic simulation ----

    private fun baselines(): Triple<Double, Double, Double> {
        val p = profile
        val age = p?.age?.takeIf { it in 18..120 } ?: 40
        val hyper = p?.hypertensionHistory == true
        val bmiBoost = ((p?.bmi ?: 22.0) - 22.0) * 0.3
        val hr = (74.0 - (age - 40) * 0.25 + (if (hyper) 4 else 0) + bmiBoost * 0.2)
            .coerceIn(55.0, 96.0)
        val sbp = (116.0 + (age - 40) * 0.35 + (if (hyper) 16 else 0) + bmiBoost)
            .coerceIn(95.0, 178.0)
        val dbp = (74.0 + (age - 40) * 0.15 + (if (hyper) 9 else 0) + bmiBoost * 0.4)
            .coerceIn(60.0, 112.0)
        return Triple(hr, sbp, dbp)
    }

    /** Smooth, reproducible perturbation around [base] derived from [seed] (timestamp). */
    private fun wobble(base: Double, seed: Long, spread: Double): Double =
        base + sin(seed / 60000.0 + base) * spread

    private suspend fun seedBaselineIfNeeded() {
        if (baselineSeeded) return
        baselineSeeded = true
        val now = System.currentTimeMillis()
        (6 downTo 1).forEach { daysAgo ->
            val dayStart = startOfDay(now - daysAgo * DAY_MS)
            dao.insertBatch(generateDailyBatch(dayStart, daysAgo))
        }
    }

    private fun generateCurrentBatch(now: Long): RingDataBatch {
        val dayStart = startOfDay(now)
        val (hr, sbp, dbp) = baselines()
        val dayIndex = dayIndex(now)
        val steps = (6200 + sin(dayIndex * 0.9) * 1500 + sin(now / 3.0e5) * 300)
            .roundToInt().coerceAtLeast(2500)
        return RingDataBatch(
            measurements = listOf(
                measurement(RingMetricType.HEART_RATE, now, wobble(hr, now, 4.0), "bpm"),
                measurement(RingMetricType.HRV, now, wobble(50.0 + (hr - 74) * -0.6, now, 6.0), "ms"),
                measurement(RingMetricType.BLOOD_OXYGEN, now, wobble(98.0, now, 1.2).coerceIn(94.0, 99.0), "%"),
                measurement(RingMetricType.BLOOD_PRESSURE, now, wobble(sbp, now, 5.0), "mmHg", wobble(dbp, now, 3.0)),
                measurement(RingMetricType.TEMPERATURE, now, wobble(36.5, now, 0.15), "°C"),
                measurement(RingMetricType.STEPS, now, steps.toDouble(), "steps"),
                measurement(RingMetricType.STRESS, now, wobble(34.0, now, 8.0).coerceIn(12.0, 75.0), "score"),
            ),
            sleepSessions = listOf(sleepSession(dayStart)),
            activities = listOf(activity(dayStart, steps)),
            signalChunks = listOf(rriSignal(now), ppgSignal(now)),
        )
    }

    private fun generateDailyBatch(dayStart: Long, daysAgo: Int): RingDataBatch {
        val (hr, sbp, dbp) = baselines()
        val morning = dayStart + 8 * HOUR_MS + ((daysAgo * 137) % 45) * MINUTE_MS
        val evening = dayStart + 20 * HOUR_MS + ((daysAgo * 91) % 60) * MINUTE_MS
        val trend = (6 - daysAgo) * 0.7
        val steps = (5600 + sin(daysAgo * 1.3) * 1600 + trend * 160).roundToInt().coerceAtLeast(2500)
        return RingDataBatch(
            measurements = listOf(
                measurement(RingMetricType.HEART_RATE, morning, wobble(hr - trend * 0.6, morning, 4.0), "bpm"),
                measurement(RingMetricType.HEART_RATE, evening, wobble(hr + trend * 0.4, evening, 5.0), "bpm"),
                measurement(RingMetricType.HRV, morning, wobble(46.0 + trend + (hr - 74) * -0.5, morning, 5.0), "ms"),
                measurement(RingMetricType.BLOOD_OXYGEN, morning, wobble(98.0, morning, 1.2).coerceIn(94.0, 99.0), "%"),
                measurement(RingMetricType.BLOOD_PRESSURE, morning, wobble(sbp - trend, morning, 5.0), "mmHg", wobble(dbp - trend * 0.5, morning, 3.0)),
                measurement(RingMetricType.TEMPERATURE, morning, wobble(36.45, morning, 0.14), "°C"),
                measurement(RingMetricType.STEPS, evening, steps.toDouble(), "steps"),
                measurement(RingMetricType.STRESS, evening, wobble(40.0 - trend, evening, 8.0).coerceIn(12.0, 80.0), "score"),
            ),
            sleepSessions = listOf(sleepSession(dayStart)),
            activities = listOf(activity(dayStart, steps)),
        )
    }

    private fun sleepSession(dayStart: Long): RingSleepSessionEntity {
        val endAt = dayStart + 7 * HOUR_MS + ((dayIndex(dayStart) * 53) % 45) * MINUTE_MS
        val deep = 88 + ((dayIndex(dayStart) * 7) % 40)
        val rem = 52 + ((dayIndex(dayStart) * 11) % 30)
        val awake = 14 + ((dayIndex(dayStart) * 5) % 20)
        val light = 250 + ((dayIndex(dayStart) * 13) % 70)
        val total = deep + rem + awake + light
        return RingSleepSessionEntity(
            id = stableId("sleep", endAt),
            startedAt = endAt - total * MINUTE_MS,
            endedAt = endAt,
            deepMinutes = deep,
            lightMinutes = light,
            awakeMinutes = awake,
            remMinutes = rem,
            interruptionMinutes = awake / 3,
            source = SOURCE,
            rawPayload = """{"simulated":true,"totalMinutes":$total}""",
        )
    }

    private fun activity(dayStart: Long, steps: Int): RingActivityEntity {
        val startedAt = dayStart + 18 * HOUR_MS + ((dayIndex(dayStart) * 17) % 90) * MINUTE_MS
        val duration = 30 + ((dayIndex(dayStart) * 19) % 22)
        return RingActivityEntity(
            id = stableId("activity", startedAt),
            startedAt = startedAt,
            endedAt = startedAt + duration * MINUTE_MS,
            activityType = "walking",
            steps = (steps * 0.42).roundToInt(),
            distanceMeters = steps * 0.68,
            caloriesKcal = steps * 0.036,
            durationMinutes = duration,
            averageHeartRate = wobble(baselines().first + 28, startedAt, 8.0),
            source = SOURCE,
            rawPayload = """{"simulated":true,"type":"walking"}""",
        )
    }

    private fun rriSignal(startedAt: Long): RingSignalChunkEntity {
        val values = IntArray(48) { index ->
            (815 + sin(index / 4.0 + startedAt / 6.0e5) * 22 + sin(index / 11.0) * 6).roundToInt()
        }
        return RingSignalChunkEntity(
            id = stableId("rri", startedAt),
            signalType = RingMetricType.RRI.name,
            startedAt = startedAt,
            sampleRateHz = null,
            sampleCount = values.size,
            payload = SignalEncoding.int32LittleEndian(values),
            source = SOURCE,
        )
    }

    private fun ppgSignal(startedAt: Long): RingSignalChunkEntity {
        val values = IntArray(250) { index ->
            val wave = sin(index / 25.0 * 2.0 * PI)
            (1_850 + wave * 260 + sin(index / 9.0 + startedAt / 5.0e5) * 30).roundToInt()
        }
        return RingSignalChunkEntity(
            id = stableId("ppg", startedAt),
            signalType = RingMetricType.PPG.name,
            startedAt = startedAt,
            sampleRateHz = 25,
            sampleCount = values.size,
            payload = SignalEncoding.int32LittleEndian(values),
            source = SOURCE,
        )
    }

    private fun measurement(
        type: RingMetricType,
        measuredAt: Long,
        value: Double,
        unit: String,
        secondaryValue: Double? = null,
    ) = RingMeasurementEntity(
        id = stableId(type.name.lowercase(), measuredAt),
        metricType = type.name,
        measuredAt = measuredAt,
        primaryValue = round(value),
        secondaryValue = secondaryValue?.let(::round),
        unit = unit,
        quality = 92 + ((measuredAt / MINUTE_MS) % 8).toInt(),
        source = SOURCE,
        rawPayload = """{"simulated":true,"metric":"${type.name}"}""",
    )

    private fun round(value: Double): Double = (value * 10.0).roundToInt() / 10.0

    private fun startOfDay(timestamp: Long): Long = timestamp - timestamp % DAY_MS

    private fun dayIndex(timestamp: Long): Int = ((timestamp - DAY_MS / 2) / DAY_MS).toInt()

    private fun stableId(prefix: String, timestamp: Long): String = "${prefix}_${timestamp}_sim"

    private companion object {
        const val SOURCE = "ring_sim"
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 60 * MINUTE_MS
        const val DAY_MS = 24 * HOUR_MS
    }
}
