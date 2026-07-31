package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.BloodGlucoseCalibration
import com.rehealth.genie.ring.MenstrualCycleConfig
import com.rehealth.genie.ring.RingFeatureType
import com.rehealth.genie.ring.RingEcgContactStatus
import com.rehealth.genie.ring.RingEcgLead
import com.rehealth.genie.ring.RingEcgLiveState
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

internal data class HBandCapabilities(
    val steps: Boolean = true,
    val sleep: Boolean = true,
    val watchDataDays: Int = 0,
    val temperatureType: Int = 0,
    val heartRate: Boolean = false,
    val bloodOxygen: Boolean = false,
    val hrv: Boolean = false,
    val hrvHistory: Boolean = false,
    val bloodPressure: Boolean = false,
    val bloodGlucose: Boolean = false,
    val temperature: Boolean = false,
    val stress: Boolean = false,
    val stressHistory: Boolean = false,
    val met: Boolean = false,
    val metHistory: Boolean = false,
    val miniCheckup: Boolean = false,
    val ecg: Boolean = false,
    val bloodComponent: Boolean = false,
    val bodyComposition: Boolean = false,
    val bloodGlucoseCalibration: Boolean = false,
    val womensHealth: Boolean = false,
) {
    val supportedMetrics: Set<RingMetricType>
        get() = buildSet {
            if (steps) {
                add(RingMetricType.STEPS)
                add(RingMetricType.ACTIVITY)
            }
            if (sleep) add(RingMetricType.SLEEP)
            if (heartRate) add(RingMetricType.HEART_RATE)
            if (bloodOxygen) add(RingMetricType.BLOOD_OXYGEN)
            if (hrv || hrvHistory || miniCheckup) add(RingMetricType.HRV)
            if (bloodPressure) add(RingMetricType.BLOOD_PRESSURE)
            if (bloodGlucose) add(RingMetricType.BLOOD_GLUCOSE)
            if (temperature) add(RingMetricType.TEMPERATURE)
            if (stress || stressHistory || miniCheckup) add(RingMetricType.STRESS)
            if (met || metHistory) add(RingMetricType.MET)
            if (ecg) add(RingMetricType.ECG)
            if (bloodComponent) add(RingMetricType.BLOOD_COMPONENT)
            if (bodyComposition) add(RingMetricType.BODY_COMPOSITION)
        }

    val supportedFeatures: Set<RingFeatureType>
        get() = buildSet {
            if (bloodGlucoseCalibration) add(RingFeatureType.BLOOD_GLUCOSE_CALIBRATION)
            if (womensHealth) add(RingFeatureType.WOMENS_HEALTH)
    }
}

/**
 * Nullable capability fields reported by one of the SDK's numbered function packages.
 * A package value overrides the deprecated aggregate callback; a missing field preserves
 * the best value reported by the other sources.
 */
internal data class HBandCapabilityPatch(
    val watchDataDays: Int? = null,
    val temperatureType: Int? = null,
    val heartRate: Boolean? = null,
    val bloodOxygen: Boolean? = null,
    val hrv: Boolean? = null,
    val hrvHistory: Boolean? = null,
    val bloodPressure: Boolean? = null,
    val bloodGlucose: Boolean? = null,
    val temperature: Boolean? = null,
    val stress: Boolean? = null,
    val stressHistory: Boolean? = null,
    val met: Boolean? = null,
    val metHistory: Boolean? = null,
    val miniCheckup: Boolean? = null,
    val ecg: Boolean? = null,
    val bloodComponent: Boolean? = null,
    val bodyComposition: Boolean? = null,
    val bloodGlucoseCalibration: Boolean? = null,
    val womensHealth: Boolean? = null,
)

internal fun supportsHBandHrvHistory(deviceFeature: Boolean, hrvType: Int): Boolean =
    deviceFeature || hrvType > 0

internal fun supportsHBandMetricHistory(protocolType: Int): Boolean = protocolType > 0

internal fun HBandCapabilities.withOfficialAppMeasurementCapabilities(
    hrvFeature: Boolean,
    hrvAppDetect: Boolean,
    metFeature: Boolean,
    metAppDetect: Boolean,
): HBandCapabilities = copy(
    // The vendor documents both checks as prerequisites. Do not treat the base
    // feature or history protocol type as proof that the dedicated command works.
    hrv = hrvFeature && hrvAppDetect,
    met = metFeature && metAppDetect,
)

internal fun HBandCapabilities.apply(patch: HBandCapabilityPatch): HBandCapabilities = copy(
    watchDataDays = patch.watchDataDays ?: watchDataDays,
    temperatureType = patch.temperatureType ?: temperatureType,
    heartRate = patch.heartRate ?: heartRate,
    bloodOxygen = patch.bloodOxygen ?: bloodOxygen,
    hrv = patch.hrv ?: hrv,
    hrvHistory = patch.hrvHistory ?: hrvHistory,
    bloodPressure = patch.bloodPressure ?: bloodPressure,
    bloodGlucose = patch.bloodGlucose ?: bloodGlucose,
    temperature = patch.temperature ?: temperature,
    stress = patch.stress ?: stress,
    stressHistory = patch.stressHistory ?: stressHistory,
    met = patch.met ?: met,
    metHistory = patch.metHistory ?: metHistory,
    miniCheckup = patch.miniCheckup ?: miniCheckup,
    ecg = patch.ecg ?: ecg,
    bloodComponent = patch.bloodComponent ?: bloodComponent,
    bodyComposition = patch.bodyComposition ?: bodyComposition,
    bloodGlucoseCalibration = patch.bloodGlucoseCalibration ?: bloodGlucoseCalibration,
    womensHealth = patch.womensHealth ?: womensHealth,
)

internal fun HBandCapabilities.supportsDirectMeasurement(type: RingMetricType): Boolean = when (type) {
    RingMetricType.HRV -> hrv
    RingMetricType.STRESS -> stress
    RingMetricType.MET -> met
    else -> type in supportedMetrics
}

internal enum class HBandMeasurementRoute { DIRECT, MINI_CHECKUP, HISTORY, UNSUPPORTED }

internal fun HBandCapabilities.measurementRoute(
    type: RingMetricType,
    allowHistoryFallback: Boolean,
): HBandMeasurementRoute = when {
    // Newer protocol releases expose dedicated HRV/MET App measurement commands.
    // Prefer them only after the official base + App-detect capability checks pass.
    type in setOf(RingMetricType.HRV, RingMetricType.MET) && supportsDirectMeasurement(type) ->
        HBandMeasurementRoute.DIRECT
    // Keep the MT116 fallback for older firmware that does not advertise the new
    // App-detect capability. Stress still uses the previously verified route.
    miniCheckup && type in setOf(RingMetricType.HRV, RingMetricType.STRESS) ->
        HBandMeasurementRoute.MINI_CHECKUP
    allowHistoryFallback && type in setOf(RingMetricType.HRV, RingMetricType.STRESS, RingMetricType.MET) ->
        HBandMeasurementRoute.HISTORY
    supportsDirectMeasurement(type) -> HBandMeasurementRoute.DIRECT
    else -> HBandMeasurementRoute.UNSUPPORTED
}

internal fun hBandMiniCheckupPayload(
    type: RingMetricType,
    measuredAt: Long,
    hrv: Int,
    stress: Int,
): HBandPayload {
    val sample = when (type) {
        RingMetricType.HRV -> hrv.takeIf { it > 0 }
            ?.let { HBandMetricSample(type, measuredAt, it.toDouble(), "ms") }
        RingMetricType.STRESS -> stress.takeIf { it in 1..100 }
            ?.let { HBandMetricSample(type, measuredAt, it.toDouble(), "score") }
        else -> null
    }
    return HBandPayload(measurements = listOfNotNull(sample))
}

/**
 * Collects the burst of capability callbacks emitted during password confirmation.
 *
 * The SDK deprecates FunctionDeviceSupportData because it can be invoked repeatedly while
 * fields are still being initialized. Numbered packages are authoritative for their fields,
 * so they are overlaid on the latest aggregate report after the callback burst becomes quiet.
 */
internal class HBandCapabilityReports(
    private val quietPeriodMillis: Long = DEFAULT_QUIET_PERIOD_MILLIS,
) {
    private val firstReport = CompletableDeferred<Unit>()
    private val lock = Any()
    private var aggregate = HBandCapabilities()
    private val packagePatches = linkedMapOf<Int, HBandCapabilityPatch>()
    private var revision = 0L

    fun reportAggregate(capabilities: HBandCapabilities) = report {
        aggregate = capabilities
    }

    fun reportPackage(packageNumber: Int, patch: HBandCapabilityPatch) = report {
        packagePatches[packageNumber] = patch
    }

    suspend fun awaitSettled(): HBandCapabilities {
        firstReport.await()
        while (true) {
            val observedRevision = synchronized(lock) { revision }
            if (quietPeriodMillis > 0) delay(quietPeriodMillis)
            synchronized(lock) {
                if (observedRevision == revision) {
                    return packagePatches.values.fold(aggregate, HBandCapabilities::apply)
                }
            }
        }
    }

    private fun report(update: () -> Unit) {
        synchronized(lock) {
            update()
            revision += 1
        }
        firstReport.complete(Unit)
    }

    private companion object {
        const val DEFAULT_QUIET_PERIOD_MILLIS = 750L
    }
}

internal enum class HBandSex { MALE, FEMALE }

internal data class HBandUserProfile(
    val sex: HBandSex,
    val heightCm: Int,
    val weightKg: Int,
    val age: Int,
    val stepGoal: Int,
)

internal data class HBandConnectionInfo(
    val device: RingDevice,
    val modelCode: String?,
    val firmwareVersion: String?,
    val capabilities: HBandCapabilities,
)

internal data class HBandMetricSample(
    val type: RingMetricType,
    val measuredAt: Long,
    val value: Double,
    val unit: String,
    val secondaryValue: Double? = null,
)

internal data class HBandEcgRecord(
    val measuredAt: Long,
    val sampleRateHz: Int?,
    val drawFrequencyHz: Int?,
    val durationSeconds: Int?,
    val lead: RingEcgLead,
    val ecgType: Int?,
    val samplesMv: FloatArray,
    val averageHeartRate: Int?,
    val contactStatus: RingEcgContactStatus,
    val calibrationType: String?,
)

internal data class HBandSleepRecord(
    val startedAt: Long,
    val endedAt: Long,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val awakeMinutes: Int,
    val totalMinutes: Int = deepMinutes + lightMinutes + awakeMinutes,
)

internal data class HBandActivityRecord(
    val startedAt: Long,
    val endedAt: Long,
    val steps: Int,
    val distanceMeters: Double,
    val caloriesKcal: Double,
)

internal class HBandDailyActivityAccumulator(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    private val totalsByDay = linkedMapOf<Long, HBandActivityRecord>()

    fun add(measuredAt: Long, steps: Int, distanceMeters: Double, caloriesKcal: Double) {
        if (measuredAt <= 0) return
        val validSteps = steps.coerceAtLeast(0)
        val validDistance = distanceMeters.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val validCalories = caloriesKcal.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        if (validSteps == 0 && validDistance == 0.0 && validCalories == 0.0) return

        val dayStart = Instant.ofEpochMilli(measuredAt).atZone(zoneId).toLocalDate()
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        val previous = totalsByDay[dayStart]
        totalsByDay[dayStart] = HBandActivityRecord(
            startedAt = dayStart,
            endedAt = maxOf(previous?.endedAt ?: dayStart, measuredAt + FIVE_MINUTE_MILLIS),
            steps = ((previous?.steps ?: 0).toLong() + validSteps).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            distanceMeters = (previous?.distanceMeters ?: 0.0) + validDistance,
            caloriesKcal = (previous?.caloriesKcal ?: 0.0) + validCalories,
        )
    }

    fun records(): List<HBandActivityRecord> = totalsByDay.values.sortedBy(HBandActivityRecord::startedAt)

    private companion object {
        const val FIVE_MINUTE_MILLIS = 5 * 60 * 1_000L
    }
}

internal data class HBandPayload(
    val measurements: List<HBandMetricSample> = emptyList(),
    val sleep: List<HBandSleepRecord> = emptyList(),
    val activities: List<HBandActivityRecord> = emptyList(),
    val ecgRecords: List<HBandEcgRecord> = emptyList(),
) {
    operator fun plus(other: HBandPayload) = HBandPayload(
        measurements + other.measurements,
        sleep + other.sleep,
        activities + other.activities,
        ecgRecords + other.ecgRecords,
    )
}

internal data class HBandSyncOptions(
    val historyDays: Int? = null,
    val includeOriginHistory: Boolean = true,
    val onProgress: (Int) -> Unit = {},
)

internal interface HBandSdkGateway {
    val connectionState: StateFlow<RingConnectionState>
    val connectedDevice: StateFlow<RingDevice?>
    val capabilities: StateFlow<HBandCapabilities>
    val liveEcg: StateFlow<RingEcgLiveState>

    suspend fun scan(): List<RingDevice>
    suspend fun connect(device: RingDevice, profile: HBandUserProfile): HBandConnectionInfo?
    suspend fun disconnect()
    suspend fun sync(
        metrics: Set<RingMetricType>,
        options: HBandSyncOptions = HBandSyncOptions(),
    ): HBandPayload
    suspend fun measure(type: RingMetricType, allowHistoryFallback: Boolean = false): HBandPayload
    suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean = false
    suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean = false
}
