package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.SignalEncoding
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.security.MessageDigest

internal const val HBAND_DATA_SOURCE = "hband_wearable"

internal object HBandDataMapper {

    fun toEntities(payload: HBandPayload, deviceKey: String): RingDataBatch = RingDataBatch(
        measurements = payload.measurements
            .filter(::isPersistableMeasurement)
            .map { sample ->
                RingMeasurementEntity(
                    id = stableId(deviceKey, "measurement", sample.type.name, sample.measuredAt),
                    metricType = sample.type.name,
                    measuredAt = sample.measuredAt,
                    primaryValue = sample.value,
                    secondaryValue = sample.secondaryValue,
                    unit = sample.unit,
                    quality = null,
                    source = HBAND_DATA_SOURCE,
                    rawPayload = null,
                )
            }.distinctBy { it.id },
        sleepSessions = payload.sleep
            .filter { it.startedAt > 0 && it.endedAt > it.startedAt && it.totalMinutes > 0 }
            .map { record ->
                RingSleepSessionEntity(
                    id = stableId(deviceKey, "sleep", "session", record.startedAt),
                    startedAt = record.startedAt,
                    endedAt = record.endedAt,
                    deepMinutes = record.deepMinutes,
                    lightMinutes = record.lightMinutes,
                    awakeMinutes = record.awakeMinutes,
                    // The selected HBand API does not report REM as a separate stage.
                    remMinutes = 0,
                    interruptionMinutes = record.awakeMinutes,
                    source = HBAND_DATA_SOURCE,
                    rawPayload = null,
                    totalSleepMinutes = record.totalMinutes,
                )
            }.distinctBy { it.id },
        activities = payload.activities
            .filter { it.startedAt > 0 && it.endedAt > it.startedAt && (it.steps > 0 || it.distanceMeters > 0.0 || it.caloriesKcal > 0.0) }
            .map { record ->
                RingActivityEntity(
                    id = stableId(deviceKey, "activity", "daily_steps", record.startedAt),
                    startedAt = record.startedAt,
                    endedAt = record.endedAt,
                    activityType = "daily_steps",
                    steps = record.steps,
                    distanceMeters = record.distanceMeters,
                    caloriesKcal = record.caloriesKcal,
                    // Daily SportData has no workout-duration field; do not report elapsed wall-clock time as exercise.
                    durationMinutes = 0,
                    averageHeartRate = null,
                    source = HBAND_DATA_SOURCE,
                    rawPayload = null,
                )
            }.distinctBy { it.id },
        signalChunks = payload.ecgRecords
            .filter { it.measuredAt > 0 && it.samplesMv.isNotEmpty() }
            .map { record ->
                RingSignalChunkEntity(
                    id = stableId(deviceKey, "signal", RingMetricType.ECG.name, record.measuredAt),
                    signalType = RingMetricType.ECG.name,
                    startedAt = record.measuredAt,
                    sampleRateHz = record.sampleRateHz?.takeIf { it > 0 },
                    sampleCount = record.samplesMv.size,
                    encoding = "FLOAT32_LE",
                    payload = SignalEncoding.float32LittleEndian(record.samplesMv),
                    source = HBAND_DATA_SOURCE,
                    drawFrequencyHz = record.drawFrequencyHz,
                    durationSeconds = record.durationSeconds,
                    leadType = record.lead.takeUnless { it == com.rehealth.genie.ring.RingEcgLead.UNKNOWN }?.name,
                    ecgType = record.ecgType,
                    calibrationType = record.calibrationType,
                    averageHeartRate = record.averageHeartRate,
                    contactQuality = record.contactStatus.name,
                )
            }.distinctBy { it.id },
    )

    fun collectedTypes(batch: RingDataBatch): Set<RingMetricType> = buildSet {
        batch.measurements.mapNotNullTo(this) { runCatching { RingMetricType.valueOf(it.metricType) }.getOrNull() }
        if (batch.sleepSessions.isNotEmpty()) add(RingMetricType.SLEEP)
        if (batch.activities.isNotEmpty()) {
            add(RingMetricType.STEPS)
            add(RingMetricType.ACTIVITY)
        }
        if (batch.signalChunks.any { it.signalType == RingMetricType.ECG.name }) add(RingMetricType.ECG)
    }

    private fun isPersistableMeasurement(sample: HBandMetricSample): Boolean {
        if (sample.measuredAt <= 0 || !sample.value.isFinite() || sample.value <= 0.0) return false
        if (sample.secondaryValue?.let { !it.isFinite() || it <= 0.0 } == true) return false
        return sample.type != RingMetricType.STRESS || sample.value in 1.0..100.0
    }

    private fun stableId(deviceKey: String, kind: String, subtype: String, timestamp: Long): String {
        val input = "$HBAND_DATA_SOURCE|${deviceKey.lowercase()}|$kind|$subtype|$timestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
