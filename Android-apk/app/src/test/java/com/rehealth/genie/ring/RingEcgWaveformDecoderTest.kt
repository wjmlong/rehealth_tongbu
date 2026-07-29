package com.rehealth.genie.ring

import com.rehealth.genie.ring.data.RingSignalChunkEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RingEcgWaveformDecoderTest {
    @Test
    fun decodesCalibratedMillivoltWaveform() {
        val record = signal(
            encoding = "FLOAT32_LE",
            payload = SignalEncoding.float32LittleEndian(floatArrayOf(-0.2f, 0.4f, 0.1f)),
            calibrationType = "HBAND_ECG_UTIL_MV_V1",
        )

        val waveform = RingEcgWaveformDecoder.decode(record)

        assertTrue(waveform.isMillivolts)
        assertEquals(listOf(-0.2f, 0.4f, 0.1f), waveform.samples.toList())
    }

    @Test
    fun keepsLegacyIntegerWaveformAsRelativeAmplitude() {
        val record = signal(
            encoding = "INT32_LE",
            payload = SignalEncoding.int32LittleEndian(intArrayOf(-20, 0, 10)),
        )

        val waveform = RingEcgWaveformDecoder.decode(record)

        assertFalse(waveform.isMillivolts)
        assertEquals(listOf(-1f, 0f, .5f), waveform.samples.toList())
    }

    @Test
    fun downsamplesLongHistoryForCanvasWithoutChangingEndpoints() {
        val source = FloatArray(10_000) { it.toFloat() }

        val result = RingEcgWaveformDecoder.downsample(source, 500)

        assertEquals(500, result.size)
        assertEquals(source.first(), result.first())
        assertEquals(source.last(), result.last())
    }

    private fun signal(
        encoding: String,
        payload: ByteArray,
        calibrationType: String? = null,
    ) = RingSignalChunkEntity(
        id = "ecg",
        signalType = RingMetricType.ECG.name,
        startedAt = 1L,
        sampleRateHz = 250,
        sampleCount = payload.size / 4,
        encoding = encoding,
        payload = payload,
        source = "test",
        calibrationType = calibrationType,
    )
}
