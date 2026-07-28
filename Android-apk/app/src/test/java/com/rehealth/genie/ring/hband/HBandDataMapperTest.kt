package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.SignalEncoding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HBandDataMapperTest {
    @Test
    fun createsStableVendorNeutralRecordsInExistingTables() {
        val payload = HBandPayload(
            measurements = listOf(
                HBandMetricSample(RingMetricType.HEART_RATE, 1_700_000_000_000L, 72.0, "bpm"),
                HBandMetricSample(RingMetricType.BLOOD_PRESSURE, 1_700_000_000_100L, 120.0, "mmHg", 80.0),
                HBandMetricSample(RingMetricType.ECG, 1_700_000_000_200L, 70.0, "bpm"),
            ),
            sleep = listOf(HBandSleepRecord(1_700_000_000_000L, 1_700_020_000_000L, 100, 180, 20)),
            activities = listOf(HBandActivityRecord(1_700_000_000_000L, 1_700_010_000_000L, 1234, 800.0, 40.0)),
            ecgRecords = listOf(HBandEcgRecord(1_700_000_000_200L, 250, intArrayOf(10, -20, 30), 70)),
        )
        val first = HBandDataMapper.toEntities(payload, "AA:BB")
        val second = HBandDataMapper.toEntities(payload, "aa:bb")

        assertEquals(first.measurements.map { it.id }, second.measurements.map { it.id })
        assertTrue(first.measurements.all { it.source == "hband_wearable" })
        assertEquals(80.0, first.measurements.single { it.metricType == RingMetricType.BLOOD_PRESSURE.name }.secondaryValue)
        assertEquals("hband_wearable", first.sleepSessions.single().source)
        assertEquals("hband_wearable", first.activities.single().source)
        assertTrue(first.measurements.all { it.rawPayload == null })
        assertEquals(250, first.signalChunks.single().sampleRateHz)
        assertEquals(intArrayOf(10, -20, 30).toList(), SignalEncoding.decodeInt32LittleEndian(first.signalChunks.single().payload).toList())
        assertTrue(setOf(RingMetricType.HEART_RATE, RingMetricType.BLOOD_PRESSURE, RingMetricType.ECG, RingMetricType.SLEEP, RingMetricType.STEPS, RingMetricType.ACTIVITY)
            .all { it in HBandDataMapper.collectedTypes(first) })
    }

    @Test
    fun dropsInvalidOrZeroTelemetryInsteadOfCreatingSyntheticMeasurements() {
        val batch = HBandDataMapper.toEntities(
            HBandPayload(
                measurements = listOf(HBandMetricSample(RingMetricType.HEART_RATE, 1000, 0.0, "bpm")),
                activities = listOf(HBandActivityRecord(1000, 2000, 0, 0.0, 0.0)),
            ),
            "device",
        )
        assertEquals(0, batch.size)
    }
}
