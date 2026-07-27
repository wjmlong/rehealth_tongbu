package com.rehealth.genie.ring

import kotlinx.coroutines.flow.StateFlow

interface RingRepository {
    val connectionState: StateFlow<RingConnectionState>
    val connectedDevice: StateFlow<RingDevice?>
    val supportedMetrics: Set<RingMetricType>

    suspend fun scan(): List<RingDevice>
    suspend fun connect(device: RingDevice)
    suspend fun autoConnect(): Boolean
    suspend fun disconnect()
    suspend fun syncAll(): RingSyncResult
    suspend fun measure(type: RingMetricType): RingSyncResult
    suspend fun sendCommand(data: ByteArray): Boolean
}

/** Optional debug-only hook for parameterizing simulated device data from a profile. */
interface SimulatedRingProfileSink {
    var profile: com.rehealth.genie.features.BaselineHealthProfile?
}
