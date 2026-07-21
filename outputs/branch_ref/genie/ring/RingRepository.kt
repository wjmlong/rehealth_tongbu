package com.rehealth.genie.ring

import kotlinx.coroutines.flow.StateFlow

interface RingRepository {
    val connectionState: StateFlow<RingConnectionState>
    val connectedDevice: StateFlow<RingDevice?>
    val supportedMetrics: Set<RingMetricType>

    suspend fun scan(): List<RingDevice>
    suspend fun connect(device: RingDevice)
    suspend fun disconnect()
    suspend fun syncAll(): RingSyncResult
    suspend fun measure(type: RingMetricType): RingSyncResult
}
