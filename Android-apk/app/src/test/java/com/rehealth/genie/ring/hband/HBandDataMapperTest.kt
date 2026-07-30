package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.SignalEncoding
import com.rehealth.genie.ring.RingEcgContactStatus
import com.rehealth.genie.ring.RingEcgLead
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HBandDataMapperTest {
    @Test
    fun aggregatesFiveMinuteActivityIntoDailyStepRecords() {
        val accumulator = HBandDailyActivityAccumulator(ZoneId.of("UTC"))
        accumulator.add(1_700_000_000_000L, 120, 80.0, 4.0)
        accumulator.add(1_700_000_300_000L, 180, 120.0, 6.0)
        accumulator.add(1_700_086_400_000L, 50, 30.0, 2.0)

        val records = accumulator.records()

        assertEquals(2, records.size)
        assertEquals(300, records.first().steps)
        assertEquals(200.0, records.first().distanceMeters)
        assertEquals(10.0, records.first().caloriesKcal)
        assertEquals(50, records.last().steps)
    }

    @Test
    fun keepsEcgSummaryWhenDeviceReturnsNoCurve() {
        val measuredAt = 1_700_000_000_000L
        val batch = HBandDataMapper.toEntities(
            HBandPayload(
                measurements = listOf(HBandMetricSample(RingMetricType.ECG, measuredAt, 72.0, "bpm")),
                ecgRecords = listOf(
                    HBandEcgRecord(
                        measuredAt = measuredAt,
                        sampleRateHz = 250,
                        drawFrequencyHz = 250,
                        durationSeconds = 0,
                        lead = RingEcgLead.UNKNOWN,
                        ecgType = 1,
                        samplesMv = FloatArray(0),
                        averageHeartRate = 72,
                        contactStatus = RingEcgContactStatus.UNKNOWN,
                        calibrationType = null,
                    ),
                ),
            ),
            "device",
        )

        assertEquals(1, batch.measurements.size)
        assertTrue(batch.signalChunks.isEmpty())
        assertTrue(RingMetricType.ECG in HBandDataMapper.collectedTypes(batch))
    }

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
            ecgRecords = listOf(
                HBandEcgRecord(
                    measuredAt = 1_700_000_000_200L,
                    sampleRateHz = 250,
                    drawFrequencyHz = 125,
                    durationSeconds = 3,
                    lead = RingEcgLead.LEAD_I,
                    ecgType = 1,
                    samplesMv = floatArrayOf(0.1f, -0.2f, 0.3f),
                    averageHeartRate = 70,
                    contactStatus = RingEcgContactStatus.GOOD,
                    calibrationType = "HBAND_ECG_UTIL_MV_V1",
                ),
            ),
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
        assertEquals("FLOAT32_LE", first.signalChunks.single().encoding)
        assertEquals(floatArrayOf(0.1f, -0.2f, 0.3f).toList(), SignalEncoding.decodeFloat32LittleEndian(first.signalChunks.single().payload).toList())
        assertEquals(125, first.signalChunks.single().drawFrequencyHz)
        assertEquals(3, first.signalChunks.single().durationSeconds)
        assertEquals(RingEcgLead.LEAD_I.name, first.signalChunks.single().leadType)
        assertEquals("HBAND_ECG_UTIL_MV_V1", first.signalChunks.single().calibrationType)
        assertEquals(70, first.signalChunks.single().averageHeartRate)
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

    @Test
    fun keepsTotalOnlySleepWithoutInventingSleepStages() {
        val startedAt = 1_700_000_000_000L
        val batch = HBandDataMapper.toEntities(
            HBandPayload(
                sleep = listOf(
                    HBandSleepRecord(
                        startedAt = startedAt,
                        endedAt = startedAt + 8 * 60 * 60 * 1_000L,
                        deepMinutes = 0,
                        lightMinutes = 0,
                        awakeMinutes = 0,
                        totalMinutes = 420,
                    ),
                ),
            ),
            "device",
        )

        val sleep = batch.sleepSessions.single()
        assertEquals(startedAt + 420 * 60_000L, sleep.endedAt)
        assertEquals(0, sleep.deepMinutes)
        assertEquals(0, sleep.lightMinutes)
        assertEquals(0, sleep.awakeMinutes)
        assertTrue(RingMetricType.SLEEP in HBandDataMapper.collectedTypes(batch))
    }

    @Test
    fun preservesEveryAdvancedHealthValueAsAnIndependentMeasurement() {
        val measuredAt = 1_700_000_000_000L
        val types = listOf(
            RingMetricType.BLOOD_GLUCOSE,
            RingMetricType.TEMPERATURE,
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
        val payload = HBandPayload(
            measurements = types.mapIndexed { index, type ->
                HBandMetricSample(type, measuredAt, index + 1.0, if (type == RingMetricType.BASAL_METABOLIC_RATE) "kcal/day" else "vendor_unit")
            },
        )

        val batch = HBandDataMapper.toEntities(payload, "AA:BB")

        assertEquals(types.toSet(), batch.measurements.map { RingMetricType.valueOf(it.metricType) }.toSet())
        assertEquals(types.size, batch.measurements.map { it.id }.distinct().size)
        assertTrue(types.all { it in HBandDataMapper.collectedTypes(batch) })
    }
}
