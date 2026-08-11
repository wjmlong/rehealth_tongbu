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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    private val userIdProvider: () -> String? = { "local-device" },
) : RingRepository, SimulatedRingProfileSink {
    private val mutableConnectionState = MutableStateFlow(RingConnectionState.DISCONNECTED)
    private val mutableConnectedDevice = MutableStateFlow<RingDevice?>(null)
    private val baselineSeededOwners = mutableSetOf<String>()
    private val baselineSeedMutex = Mutex()

    /** Patient profile used to parameterize the simulated vitals. Set by the VM when a real profile is available. */
    override var profile: BaselineHealthProfile? = null
        set(value) { field = value }

    override val connectionState: StateFlow<RingConnectionState> = mutableConnectionState
    override val connectedDevice: StateFlow<RingDevice?> = mutableConnectedDevice
    override val supportedMetrics: Set<RingMetricType> = SupportedHardwareHealthMetrics + setOf(
        RingMetricType.ACTIVITY,
        RingMetricType.RRI,
        RingMetricType.PPG,
        RingMetricType.BLOOD_COMPONENT,
        RingMetricType.BODY_COMPOSITION,
    )

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
        dao.insertBatch(batch.forCurrentUser())
        mutableConnectionState.value = RingConnectionState.CONNECTED
        return RingSyncResult(
            collectedTypes = supportedMetrics,
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
        dao.insertBatch(batch.forCurrentUser())
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

    private suspend fun seedBaselineIfNeeded() = baselineSeedMutex.withLock {
        val ownerUserId = currentOwnerUserId()
        if (ownerUserId in baselineSeededOwners) return@withLock
        val deviceId = currentDeviceId()
        val now = System.currentTimeMillis()
        (MOCK_HISTORY_DAYS - 1 downTo 1).forEach { daysAgo ->
            val dayStart = startOfDay(now - daysAgo * DAY_MS)
            dao.insertBatch(generateDailyBatch(dayStart, daysAgo).ownedBy(ownerUserId, deviceId))
        }
        baselineSeededOwners += ownerUserId
    }

    private fun generateCurrentBatch(now: Long): RingDataBatch {
        val dayStart = startOfDay(now)
        val (hr, sbp, dbp) = baselines()
        val dayIndex = dayIndex(now)
        val steps = (8_500 + sin(dayIndex * 0.9) * 400 + sin(now / 3.0e5) * 150)
            .roundToInt().coerceAtLeast(2500)
        return RingDataBatch(
            measurements = listOf(
                measurement(RingMetricType.HEART_RATE, now, wobble(hr - 4.0, now, 1.2), "bpm"),
                measurement(RingMetricType.HRV, now, wobble(56.0, now, 1.5), "ms"),
                measurement(RingMetricType.BLOOD_OXYGEN, now, wobble(98.0, now, 1.2).coerceIn(94.0, 99.0), "%"),
                measurement(RingMetricType.BLOOD_PRESSURE, now, wobble(sbp - 2.0, now, 1.5), "mmHg", wobble(dbp - 1.0, now, 1.0)),
                measurement(RingMetricType.TEMPERATURE, now, wobble(36.5, now, 0.15), "°C"),
                measurement(RingMetricType.STEPS, now, steps.toDouble(), "steps"),
                measurement(RingMetricType.STRESS, now, wobble(34.0, now, 8.0).coerceIn(12.0, 75.0), "score"),
                measurement(RingMetricType.MET, now, wobble(2.4, now, 0.25).coerceAtLeast(1.0), "MET"),
                measurement(RingMetricType.BLOOD_GLUCOSE, now, wobble(5.1, now, 0.18).coerceIn(4.2, 6.5), "mmol/L"),
                measurement(RingMetricType.ECG, now, wobble(hr - 3.0, now, 1.2), "bpm"),
                measurement(RingMetricType.URIC_ACID, now, wobble(318.0, now, 8.0), "umol/L"),
                measurement(RingMetricType.TOTAL_CHOLESTEROL, now, wobble(4.5, now, 0.12), "mmol/L"),
                measurement(RingMetricType.TRIGLYCERIDES, now, wobble(1.1, now, 0.08), "mmol/L"),
                measurement(RingMetricType.HDL_CHOLESTEROL, now, wobble(1.35, now, 0.05), "mmol/L"),
                measurement(RingMetricType.LDL_CHOLESTEROL, now, wobble(2.5, now, 0.09), "mmol/L"),
                measurement(RingMetricType.BMI, now, 22.4, "kg/m2"),
                measurement(RingMetricType.BODY_FAT_PERCENT, now, 19.0, "%"),
                measurement(RingMetricType.FAT_MASS, now, 13.5, "kg"),
                measurement(RingMetricType.FAT_FREE_MASS, now, 57.5, "kg"),
                measurement(RingMetricType.MUSCLE_PERCENT, now, 76.0, "%"),
                measurement(RingMetricType.MUSCLE_MASS, now, 54.0, "kg"),
                measurement(RingMetricType.SUBCUTANEOUS_FAT_PERCENT, now, 16.2, "%"),
                measurement(RingMetricType.BODY_WATER_PERCENT, now, 58.5, "%"),
                measurement(RingMetricType.WATER_MASS, now, 41.5, "kg"),
                measurement(RingMetricType.SKELETAL_MUSCLE_PERCENT, now, 42.0, "%"),
                measurement(RingMetricType.BONE_MASS, now, 3.0, "kg"),
                measurement(RingMetricType.PROTEIN_PERCENT, now, 18.0, "%"),
                measurement(RingMetricType.PROTEIN_MASS, now, 12.8, "kg"),
                measurement(RingMetricType.BASAL_METABOLIC_RATE, now, 1_620.0, "kcal/day"),
            ),
            sleepSessions = listOf(sleepSession(dayStart, progress = 1.0)),
            activities = listOf(activity(dayStart, steps, progress = 1.0)),
            signalChunks = listOf(rriSignal(now), ppgSignal(now)),
        )
    }

    private fun generateDailyBatch(dayStart: Long, daysAgo: Int): RingDataBatch {
        val (hr, sbp, dbp) = baselines()
        val morning = dayStart + 8 * HOUR_MS + ((daysAgo * 137) % 45) * MINUTE_MS
        val evening = dayStart + 20 * HOUR_MS + ((daysAgo * 91) % 60) * MINUTE_MS
        val progress = (MOCK_HISTORY_DAYS - 1 - daysAgo).toDouble() / (MOCK_HISTORY_DAYS - 1)
        val steps = (1_500 + progress * 7_000.0 + sin(daysAgo * 1.3) * 400.0)
            .roundToInt().coerceAtLeast(800)
        val restingHr = hr + 10.0 - progress * 14.0
        val hrv = 24.0 + progress * 32.0
        return RingDataBatch(
            measurements = listOf(
                measurement(RingMetricType.HEART_RATE, morning, wobble(restingHr, morning, 1.2), "bpm"),
                measurement(RingMetricType.HEART_RATE, evening, wobble(restingHr + 1.5, evening, 1.5), "bpm"),
                measurement(RingMetricType.HRV, morning, wobble(hrv, morning, 1.5), "ms"),
                measurement(RingMetricType.BLOOD_OXYGEN, morning, wobble(98.0, morning, 1.2).coerceIn(94.0, 99.0), "%"),
                measurement(
                    RingMetricType.BLOOD_PRESSURE,
                    morning,
                    wobble(sbp + 3.0 - progress * 5.0, morning, 1.5),
                    "mmHg",
                    wobble(dbp + 2.0 - progress * 3.0, morning, 1.0),
                ),
                measurement(RingMetricType.TEMPERATURE, morning, wobble(36.45, morning, 0.14), "°C"),
                measurement(RingMetricType.STEPS, evening, steps.toDouble(), "steps"),
                measurement(
                    RingMetricType.STRESS,
                    evening,
                    wobble(50.0 - progress * 15.0, evening, 3.0).coerceIn(12.0, 80.0),
                    "score",
                ),
            ),
            sleepSessions = listOf(sleepSession(dayStart, progress)),
            // Preserve a meaningful 90-day improvement path while giving the
            // latest 7 days a visible, retention-friendly activity milestone.
            // RDI still evaluates the real rule engine output from these records.
            activities = listOf(
                activity(
                    dayStart,
                    steps,
                    progress = if (daysAgo <= 6) 1.0 else progress * 0.50,
                ),
            ),
        )
    }

    private fun sleepSession(dayStart: Long, progress: Double): RingSleepSessionEntity {
        val endAt = dayStart + 7 * HOUR_MS + ((dayIndex(dayStart) * 53) % 45) * MINUTE_MS
        val totalTarget = (300 + progress * 190.0).roundToInt()
        val awake = (totalTarget * (0.30 - progress * 0.22)).roundToInt()
        val asleep = totalTarget - awake
        val deep = (asleep * 0.22).roundToInt()
        val rem = (asleep * 0.20).roundToInt()
        val light = asleep - deep - rem
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

    private fun activity(dayStart: Long, steps: Int, progress: Double): RingActivityEntity {
        val startedAt = dayStart + 18 * HOUR_MS + ((dayIndex(dayStart) * 17) % 90) * MINUTE_MS
        val duration = (20 + progress * 28.0).roundToInt()
        return RingActivityEntity(
            id = stableId("activity", startedAt),
            startedAt = startedAt,
            endedAt = startedAt + duration * MINUTE_MS,
            activityType = "walking",
            // RHI/RDI consume RingActivityEntity as their daily activity summary.
            // Store the full daily steps here; the previous workout-only 42% value
            // understated the health trajectory even though STEPS measurements
            // already contained the correct total.
            steps = steps,
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

    private fun RingDataBatch.forCurrentUser(): RingDataBatch {
        return ownedBy(currentOwnerUserId(), currentDeviceId())
    }

    private fun currentOwnerUserId(): String =
        userIdProvider()?.takeIf(String::isNotBlank) ?: "local-device"

    private fun currentDeviceId(): String =
        mutableConnectedDevice.value?.address ?: "MOCK:RING:01"

    private fun startOfDay(timestamp: Long): Long = timestamp - timestamp % DAY_MS

    private fun dayIndex(timestamp: Long): Int = ((timestamp - DAY_MS / 2) / DAY_MS).toInt()

    private fun stableId(prefix: String, timestamp: Long): String = "${prefix}_${timestamp}_sim"

    private companion object {
        const val SOURCE = "ring_sim"
        const val MOCK_HISTORY_DAYS = 118
        const val MINUTE_MS = 60_000L
        const val HOUR_MS = 60 * MINUTE_MS
        const val DAY_MS = 24 * HOUR_MS
    }
}
