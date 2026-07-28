package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSignalChunkEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import kotlinx.coroutines.flow.first

data class RingHealthHistory(
    val measurements: Map<RingMetricType, List<RingMeasurementEntity>> = emptyMap(),
    val sleepSessions: List<RingSleepSessionEntity> = emptyList(),
    val activities: List<RingActivityEntity> = emptyList(),
    val signals: Map<RingMetricType, List<RingSignalChunkEntity>> = emptyMap(),
)

internal suspend fun RingDataDao.loadRingHealthHistory(limitPerType: Int = 50): RingHealthHistory {
    require(limitPerType > 0)
    val measurements = RingMetricType.entries.mapNotNull { type ->
        observeMeasurements(type.name, limitPerType).first()
            .takeIf { it.isNotEmpty() }
            ?.let { type to it }
    }.toMap()
    val signals = RingMetricType.entries.mapNotNull { type ->
        observeSignalChunks(type.name, limitPerType).first()
            .takeIf { it.isNotEmpty() }
            ?.let { type to it }
    }.toMap()
    return RingHealthHistory(
        measurements = measurements,
        sleepSessions = observeSleepSessions(limitPerType).first(),
        activities = observeActivities(limitPerType).first(),
        signals = signals,
    )
}
