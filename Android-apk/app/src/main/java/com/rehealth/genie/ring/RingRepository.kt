package com.rehealth.genie.ring

import kotlinx.coroutines.flow.StateFlow

interface RingRepository {
    val acquisitionMode: RingAcquisitionMode
        get() = RingAcquisitionMode.BLUETOOTH
    val connectionState: StateFlow<RingConnectionState>
    val connectedDevice: StateFlow<RingDevice?>
    val supportedMetrics: Set<RingMetricType>
    val manuallyMeasurableMetrics: Set<RingMetricType>
        get() = supportedMetrics

    suspend fun scan(): List<RingDevice>
    suspend fun connect(device: RingDevice)
    suspend fun autoConnect(): Boolean
    suspend fun disconnect()
    suspend fun syncAll(): RingSyncResult
    /** Foreground, connected-only sync for the metrics currently requested by the UI. */
    suspend fun sync(
        metrics: Set<RingMetricType>,
        onProgress: (Int) -> Unit = {},
    ): RingSyncResult {
        if (connectionState.value != RingConnectionState.CONNECTED) {
            return RingSyncResult(emptySet(), 0, System.currentTimeMillis())
        }
        onProgress(5)
        return syncAll().also { onProgress(100) }
    }
    suspend fun measure(type: RingMetricType): RingSyncResult
    suspend fun sendCommand(data: ByteArray): Boolean
}

/** Optional capability-gated device settings. These operations do not create health measurements. */
interface RingFeatureRepository {
    val supportedFeatures: Set<RingFeatureType>

    suspend fun setBloodGlucoseCalibration(config: BloodGlucoseCalibration): Boolean
    suspend fun setMenstrualCycle(config: MenstrualCycleConfig): Boolean
}

/** Optional debug-only hook for parameterizing simulated device data from a profile. */
interface SimulatedRingProfileSink {
    var profile: com.rehealth.genie.features.BaselineHealthProfile?
}

/** Optional vendor-neutral hook for SDKs that require the user's real body profile. */
interface WearableUserProfileSink {
    var wearableUserProfile: com.rehealth.genie.features.BaselineHealthProfile?
}
