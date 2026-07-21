package com.rehealth.genie.ring

enum class RingMetricType {
    HEART_RATE,
    HRV,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    SLEEP,
    TEMPERATURE,
    STEPS,
    ACTIVITY,
    STRESS,
    RRI,
    PPG,
}

enum class RingFeatureType {
    REMOTE_CAMERA,
    WOMENS_HEALTH,
}

enum class RingConnectionState {
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    BLUETOOTH_OFF,
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    SYNCING,
    ERROR,
}

data class RingDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
)

data class RingSyncResult(
    val collectedTypes: Set<RingMetricType>,
    val recordsWritten: Int,
    val completedAt: Long,
)

val RequiredRingMetrics: Set<RingMetricType> = RingMetricType.entries.toSet()
val SupportedHardwareHealthMetrics: Set<RingMetricType> = setOf(
    RingMetricType.SLEEP,
    RingMetricType.BLOOD_PRESSURE,
    RingMetricType.TEMPERATURE,
    RingMetricType.HEART_RATE,
    RingMetricType.STEPS,
    RingMetricType.BLOOD_OXYGEN,
)
val SupportedRingFeatures: Set<RingFeatureType> = RingFeatureType.entries.toSet()
