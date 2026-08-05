package com.rehealth.genie.ring

enum class RingMetricType {
    HEART_RATE,
    HRV,
    BLOOD_OXYGEN,
    BLOOD_PRESSURE,
    BLOOD_GLUCOSE,
    SLEEP,
    TEMPERATURE,
    STEPS,
    ACTIVITY,
    STRESS,
    MET,
    RRI,
    PPG,
    ECG,
    BLOOD_COMPONENT,
    URIC_ACID,
    TOTAL_CHOLESTEROL,
    TRIGLYCERIDES,
    HDL_CHOLESTEROL,
    LDL_CHOLESTEROL,
    BODY_COMPOSITION,
    BMI,
    BODY_FAT_PERCENT,
    FAT_MASS,
    FAT_FREE_MASS,
    MUSCLE_PERCENT,
    MUSCLE_MASS,
    SUBCUTANEOUS_FAT_PERCENT,
    BODY_WATER_PERCENT,
    WATER_MASS,
    SKELETAL_MUSCLE_PERCENT,
    BONE_MASS,
    PROTEIN_PERCENT,
    PROTEIN_MASS,
    BASAL_METABOLIC_RATE,
}

enum class RingFeatureType {
    REMOTE_CAMERA,
    BLOOD_GLUCOSE_CALIBRATION,
    WOMENS_HEALTH,
}

data class BloodGlucoseCalibration(
    val enabled: Boolean,
    val referenceValue: Double,
)

data class MenstrualCycleConfig(
    val periodLengthDays: Int,
    val cycleLengthDays: Int,
    val lastPeriodStartAt: Long,
)

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

enum class RingAcquisitionMode { BLUETOOTH, CLOUD }

data class RingDevice(
    val address: String,
    val name: String?,
    val rssi: Int?,
)

data class RingSyncResult(
    val collectedTypes: Set<RingMetricType>,
    val recordsWritten: Int,
    val completedAt: Long,
    val requiresUpload: Boolean = true,
)

val RequiredRingMetrics: Set<RingMetricType> = setOf(
    RingMetricType.HEART_RATE,
    RingMetricType.HRV,
    RingMetricType.BLOOD_OXYGEN,
    RingMetricType.BLOOD_PRESSURE,
    RingMetricType.SLEEP,
    RingMetricType.TEMPERATURE,
    RingMetricType.STEPS,
    RingMetricType.ACTIVITY,
    RingMetricType.STRESS,
    RingMetricType.RRI,
    RingMetricType.PPG,
)
val SupportedHardwareHealthMetrics: Set<RingMetricType> = setOf(
    RingMetricType.SLEEP,
    RingMetricType.BLOOD_PRESSURE,
    RingMetricType.BLOOD_GLUCOSE,
    RingMetricType.TEMPERATURE,
    RingMetricType.HEART_RATE,
    RingMetricType.STEPS,
    RingMetricType.BLOOD_OXYGEN,
    RingMetricType.ECG,
    RingMetricType.HRV,
    RingMetricType.STRESS,
    RingMetricType.MET,
    RingMetricType.URIC_ACID,
    RingMetricType.TOTAL_CHOLESTEROL,
    RingMetricType.TRIGLYCERIDES,
    RingMetricType.HDL_CHOLESTEROL,
    RingMetricType.LDL_CHOLESTEROL,
    RingMetricType.BMI,
    RingMetricType.BODY_FAT_PERCENT,
    RingMetricType.FAT_MASS,
    RingMetricType.FAT_FREE_MASS,
    RingMetricType.MUSCLE_PERCENT,
    RingMetricType.MUSCLE_MASS,
    RingMetricType.SUBCUTANEOUS_FAT_PERCENT,
    RingMetricType.BODY_WATER_PERCENT,
    RingMetricType.WATER_MASS,
    RingMetricType.SKELETAL_MUSCLE_PERCENT,
    RingMetricType.BONE_MASS,
    RingMetricType.PROTEIN_PERCENT,
    RingMetricType.PROTEIN_MASS,
    RingMetricType.BASAL_METABOLIC_RATE,
)
val SupportedRingFeatures: Set<RingFeatureType> = RingFeatureType.entries.toSet()
