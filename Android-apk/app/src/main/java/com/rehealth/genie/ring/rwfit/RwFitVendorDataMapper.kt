package com.rehealth.genie.ring.rwfit

import com.example.blesdk.bean.sync.BloodOxySyncBean
import com.example.blesdk.bean.sync.HeartRateSyncBean
import com.example.blesdk.bean.sync.HrvSyncBean
import com.example.blesdk.bean.sync.SleepSyncBean
import com.example.blesdk.bean.sync.StepSyncBean
import com.rehealth.genie.ring.RingMetricType

internal object RwFitVendorDataMapper {
    fun steps(records: List<StepSyncBean>): RwFitPayload = RwFitPayload(
        activities = records.mapNotNull(::activity),
    )

    fun sleep(records: List<SleepSyncBean>): RwFitPayload = RwFitPayload(
        sleep = records.mapNotNull(::sleepSession),
    )

    fun heartRate(records: List<HeartRateSyncBean>): RwFitPayload = RwFitPayload(
        measurements = records.flatMap { record ->
            record.items.orEmpty().mapNotNull { item ->
                metric(
                    type = RingMetricType.HEART_RATE,
                    timestamp = item.timeMills,
                    value = item.hr,
                    unit = "bpm",
                )
            }
        },
    )

    fun hrv(records: List<HrvSyncBean>): RwFitPayload = RwFitPayload(
        measurements = records.flatMap { record ->
            record.items.orEmpty().mapNotNull { item ->
                metric(
                    type = RingMetricType.HRV,
                    timestamp = item.timeMills,
                    value = item.hrv,
                    // The official SDK exposes an integer HRV field but does not
                    // document its unit. Preserve the real value without claiming ms.
                    unit = "rwfit_raw",
                )
            }
        },
    )

    fun bloodOxygen(records: List<BloodOxySyncBean>): RwFitPayload = RwFitPayload(
        measurements = records.flatMap { record ->
            record.items.orEmpty().mapNotNull { item ->
                metric(
                    type = RingMetricType.BLOOD_OXYGEN,
                    timestamp = item.timeMills,
                    value = item.bloodOxy,
                    unit = "%",
                )
            }
        },
    )

    fun realTime(
        type: RingMetricType,
        timestamp: Long,
        value: Int,
        observedAt: Long,
    ): RwFitPayload {
        val unit = when (type) {
            RingMetricType.HEART_RATE -> "bpm"
            RingMetricType.BLOOD_OXYGEN -> "%"
            RingMetricType.HRV -> "rwfit_raw"
            else -> return RwFitPayload()
        }
        val sample = metric(type, timestamp, value, unit, observedAt) ?: return RwFitPayload()
        return RwFitPayload(measurements = listOf(sample))
    }

    internal fun normalizeEpochMillis(value: Long): Long? = when {
        value <= 0L -> null
        value < MILLIS_THRESHOLD -> value * 1_000L
        else -> value
    }

    private fun metric(
        type: RingMetricType,
        timestamp: Long,
        value: Int,
        unit: String,
        fallbackTimestamp: Long? = null,
    ): RwFitMetricSample? {
        if (value <= 0) return null
        val measuredAt = normalizeEpochMillis(timestamp) ?: fallbackTimestamp ?: return null
        return RwFitMetricSample(type, measuredAt, value.toDouble(), unit)
    }

    private fun activity(record: StepSyncBean): RwFitActivityRecord? {
        if (record.totalSteps <= 0 && record.totalDistance <= 0 && record.totalCalorie <= 0) return null
        val items = record.items.orEmpty()
        val itemTimes = items.mapNotNull { item -> normalizeEpochMillis(item.timestamp) }
        val startedAt = itemTimes.minOrNull() ?: normalizeEpochMillis(record.time) ?: return null
        val intervalMinutes = record.activityDataInterval.takeIf { it > 0 } ?: DEFAULT_ACTIVITY_INTERVAL_MINUTES
        val endedAt = itemTimes.maxOrNull()?.plus(intervalMinutes * MILLIS_PER_MINUTE)
        val detailCount = maxOf(items.size, record.itemCount)
        if (detailCount <= 0) return null
        return RwFitActivityRecord(
            startedAt = startedAt,
            endedAt = endedAt,
            steps = record.totalSteps,
            distanceMeters = record.totalDistance.toDouble(),
            // Official documentation defines the SDK field as calories (cal),
            // while the existing Room schema stores kilocalories.
            caloriesKcal = record.totalCalorie / 1_000.0,
            durationMinutes = detailCount * intervalMinutes,
        )
    }

    private fun sleepSession(record: SleepSyncBean): RwFitSleepRecord? {
        val startedAt = normalizeEpochMillis(record.asleepTime) ?: return null
        val endedAt = normalizeEpochMillis(record.awakeTime)?.takeIf { it > startedAt } ?: return null
        val finalItems = record.items.orEmpty().filter { item -> item.isTemporary == 0 && item.len > 0 }
        if (finalItems.isEmpty()) return null
        val deep = finalItems.filter { it.sleepType == SLEEP_DEEP }.sumOf { it.len }
        val light = finalItems.filter { it.sleepType == SLEEP_LIGHT }.sumOf { it.len }
        val awake = finalItems.filter { it.sleepType == SLEEP_AWAKE }.sumOf { it.len }
        if (deep + light + awake <= 0) return null
        return RwFitSleepRecord(startedAt, endedAt, deep, light, awake)
    }

    private const val MILLIS_THRESHOLD = 100_000_000_000L
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val DEFAULT_ACTIVITY_INTERVAL_MINUTES = 60
    private const val SLEEP_AWAKE = 0
    private const val SLEEP_LIGHT = 1
    private const val SLEEP_DEEP = 2
}
