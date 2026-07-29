package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingSignalChunkEntity
import kotlin.math.abs
import kotlinx.coroutines.flow.StateFlow

enum class RingEcgLead(val displayName: String) {
    LEAD_I("I 导联"),
    LEAD_V1("V1 导联"),
    UNKNOWN("导联待设备确认"),
    ;

    companion object {
        fun fromStored(value: String?): RingEcgLead = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class RingEcgContactStatus(val displayName: String) {
    UNKNOWN("接触状态未知"),
    GOOD("电极接触良好"),
    POOR("电极接触不良"),
    ;

    companion object {
        fun fromStored(value: String?): RingEcgContactStatus = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class RingEcgMeasurementPhase {
    IDLE,
    PREPARING,
    MEASURING,
    COMPLETE,
    FAILED,
}

data class RingEcgLiveState(
    val phase: RingEcgMeasurementPhase = RingEcgMeasurementPhase.IDLE,
    val startedAt: Long? = null,
    val sampleRateHz: Int? = null,
    val drawFrequencyHz: Int? = null,
    val lead: RingEcgLead = RingEcgLead.UNKNOWN,
    val samplesMv: FloatArray = FloatArray(0),
    val currentHeartRate: Int? = null,
    val averageHeartRate: Int? = null,
    val progress: Int = 0,
    val contactStatus: RingEcgContactStatus = RingEcgContactStatus.UNKNOWN,
    val isCalibrated: Boolean = false,
    val message: String? = null,
)

/** Optional real-time ECG surface implemented only by providers that expose calibrated waveform callbacks. */
interface RingEcgRepository {
    val liveEcg: StateFlow<RingEcgLiveState>
}

data class RingEcgWaveform(
    val samples: FloatArray,
    val isMillivolts: Boolean,
)

object RingEcgWaveformDecoder {
    fun decode(record: RingSignalChunkEntity): RingEcgWaveform = when (record.encoding) {
        "FLOAT32_LE" -> RingEcgWaveform(
            samples = SignalEncoding.decodeFloat32LittleEndian(record.payload).filterFinite(),
            isMillivolts = record.calibrationType != null,
        )
        "INT32_LE" -> RingEcgWaveform(
            samples = normalizeRelative(SignalEncoding.decodeInt32LittleEndian(record.payload)),
            isMillivolts = false,
        )
        else -> RingEcgWaveform(FloatArray(0), false)
    }

    fun downsample(samples: FloatArray, maxPoints: Int): FloatArray {
        if (samples.size <= maxPoints || maxPoints < 2) return samples
        val result = FloatArray(maxPoints)
        val step = (samples.size - 1).toDouble() / (maxPoints - 1)
        for (index in result.indices) result[index] = samples[(index * step).toInt()]
        return result
    }

    private fun normalizeRelative(values: IntArray): FloatArray {
        if (values.isEmpty()) return FloatArray(0)
        val maxMagnitude = values.maxOfOrNull { abs(it.toLong()) }?.coerceAtLeast(1L)?.toFloat() ?: 1f
        return FloatArray(values.size) { index -> values[index] / maxMagnitude }
    }

    private fun FloatArray.filterFinite(): FloatArray = filter(Float::isFinite).toFloatArray()
}
