package com.rehealth.genie.ring.rwfit

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataBatch
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.security.MessageDigest

internal object RwFitDataMapper {
    private const val SOURCE = "rwfit"

    fun toEntities(payload: RwFitPayload, deviceKey: String): RingDataBatch = RingDataBatch(
        measurements = payload.measurements.map { sample ->
            RingMeasurementEntity(
                id = stableId(deviceKey, "measurement", sample.type.name, sample.measuredAt),
                metricType = sample.type.name,
                measuredAt = sample.measuredAt,
                primaryValue = sample.value,
                unit = sample.unit,
                quality = null,
                source = SOURCE,
                rawPayload = null,
            )
        }.distinctBy { it.id },
        sleepSessions = payload.sleep.map { record ->
            RingSleepSessionEntity(
                id = stableId(deviceKey, "sleep", "session", record.startedAt),
                startedAt = record.startedAt,
                endedAt = record.endedAt,
                deepMinutes = record.deepMinutes,
                lightMinutes = record.lightMinutes,
                awakeMinutes = record.awakeMinutes,
                // RWFit's documented sleep stages are awake/light/deep. A zero REM
                // duration means no REM stage is present in this vendor record; it
                // is not a synthesized REM measurement.
                remMinutes = 0,
                interruptionMinutes = record.awakeMinutes,
                source = SOURCE,
                rawPayload = null,
            )
        }.distinctBy { it.id },
        activities = payload.activities.map { record ->
            RingActivityEntity(
                id = stableId(deviceKey, "activity", "daily_steps", record.startedAt),
                startedAt = record.startedAt,
                endedAt = record.endedAt,
                activityType = "daily_steps",
                steps = record.steps,
                distanceMeters = record.distanceMeters,
                caloriesKcal = record.caloriesKcal,
                durationMinutes = record.durationMinutes,
                averageHeartRate = null,
                source = SOURCE,
                rawPayload = null,
            )
        }.distinctBy { it.id },
    )

    fun collectedTypes(batch: RingDataBatch): Set<RingMetricType> = buildSet {
        batch.measurements.mapNotNullTo(this) { entity ->
            runCatching { RingMetricType.valueOf(entity.metricType) }.getOrNull()
        }
        if (batch.sleepSessions.isNotEmpty()) add(RingMetricType.SLEEP)
        if (batch.activities.isNotEmpty()) {
            add(RingMetricType.STEPS)
            add(RingMetricType.ACTIVITY)
        }
    }

    private fun stableId(
        deviceKey: String,
        recordKind: String,
        subtype: String,
        timestamp: Long,
    ): String {
        val input = "$SOURCE|${deviceKey.lowercase()}|$recordKind|$subtype|$timestamp"
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }
    }
}
