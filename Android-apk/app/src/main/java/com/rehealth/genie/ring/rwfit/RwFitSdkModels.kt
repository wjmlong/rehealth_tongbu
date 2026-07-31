package com.rehealth.genie.ring.rwfit

import com.rehealth.genie.ring.RingConnectionState
import com.rehealth.genie.ring.RingDevice
import com.rehealth.genie.ring.RingMetricType
import kotlinx.coroutines.flow.StateFlow

internal data class RwFitCapabilities(
    val steps: Boolean = false,
    val sleep: Boolean = false,
    val heartRate: Boolean = false,
    val bloodOxygen: Boolean = false,
    val hrv: Boolean = false,
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
            if (hrv) add(RingMetricType.HRV)
        }
}

internal data class RwFitConnectionInfo(
    val device: RingDevice,
    val modelCode: String?,
    val firmwareVersion: String?,
    val capabilities: RwFitCapabilities,
)

internal data class RwFitMetricSample(
    val type: RingMetricType,
    val measuredAt: Long,
    val value: Double,
    val unit: String,
)

internal data class RwFitSleepRecord(
    val startedAt: Long,
    val endedAt: Long,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val awakeMinutes: Int,
)

internal data class RwFitActivityRecord(
    val startedAt: Long,
    val endedAt: Long?,
    val steps: Int,
    val distanceMeters: Double,
    val caloriesKcal: Double,
    val durationMinutes: Int,
)

internal data class RwFitPayload(
    val measurements: List<RwFitMetricSample> = emptyList(),
    val sleep: List<RwFitSleepRecord> = emptyList(),
    val activities: List<RwFitActivityRecord> = emptyList(),
) {
    operator fun plus(other: RwFitPayload): RwFitPayload = RwFitPayload(
        measurements = measurements + other.measurements,
        sleep = sleep + other.sleep,
        activities = activities + other.activities,
    )
}

internal interface RwFitSdkGateway {
    val connectionState: StateFlow<RingConnectionState>
    val connectedDevice: StateFlow<RingDevice?>
    val capabilities: StateFlow<RwFitCapabilities>

    suspend fun scan(): List<RingDevice>
    suspend fun connect(device: RingDevice): RwFitConnectionInfo?
    suspend fun disconnect()
    suspend fun syncSupported(): RwFitPayload
    suspend fun sync(metrics: Set<RingMetricType>): RwFitPayload = syncSupported()
    suspend fun measure(type: RingMetricType): RwFitPayload
}
