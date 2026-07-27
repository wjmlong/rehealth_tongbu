package com.rehealth.genie.ring.rwfit

import com.example.blesdk.bean.sync.BloodOxyItemBean
import com.example.blesdk.bean.sync.BloodOxySyncBean
import com.example.blesdk.bean.sync.HeartRateItemBean
import com.example.blesdk.bean.sync.HeartRateSyncBean
import com.example.blesdk.bean.sync.HrvItemBean
import com.example.blesdk.bean.sync.HrvSyncBean
import com.example.blesdk.bean.sync.SleepItemBean
import com.example.blesdk.bean.sync.SleepSyncBean
import com.example.blesdk.bean.sync.StepItemBean
import com.example.blesdk.bean.sync.StepSyncBean
import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RwFitVendorDataMapperTest {
    @Test
    fun mapsDocumentedHistoryFieldsAndSkipsInvalidZeroMeasurements() {
        val heartRate = HeartRateSyncBean().apply {
            items = listOf(
                HeartRateItemBean().apply { timeMills = 1_700_000_000L; hr = 72 },
                HeartRateItemBean().apply { timeMills = 1_700_000_060L; hr = 0 },
            )
        }
        val oxygen = BloodOxySyncBean().apply {
            items = listOf(BloodOxyItemBean().apply { timeMills = 1_700_000_100L; bloodOxy = 98 })
        }
        val hrv = HrvSyncBean().apply {
            items = listOf(HrvItemBean().apply { timeMills = 1_700_000_200L; hrv = 43 })
        }

        val payload = RwFitVendorDataMapper.heartRate(listOf(heartRate)) +
            RwFitVendorDataMapper.bloodOxygen(listOf(oxygen)) +
            RwFitVendorDataMapper.hrv(listOf(hrv))

        assertEquals(3, payload.measurements.size)
        assertEquals(1_700_000_000_000L, payload.measurements.first().measuredAt)
        assertEquals(setOf("bpm", "%", "rwfit_raw"), payload.measurements.map { it.unit }.toSet())
        assertTrue(payload.measurements.none { it.value == 0.0 })
    }

    @Test
    fun mapsStepTotalsUsingDocumentedMetersCaloriesAndInterval() {
        val record = StepSyncBean().apply {
            time = 1_700_000_000L
            totalSteps = 1_234
            totalDistance = 850
            totalCalorie = 12_500
            activityDataInterval = 60
            itemCount = 1
            items = listOf(StepItemBean().apply { timestamp = 1_700_000_000L; steps = 1_234 })
        }

        val activity = RwFitVendorDataMapper.steps(listOf(record)).activities.single()

        assertEquals(1_234, activity.steps)
        assertEquals(850.0, activity.distanceMeters)
        assertEquals(12.5, activity.caloriesKcal)
        assertEquals(60, activity.durationMinutes)
        assertEquals(1_700_000_000_000L, activity.startedAt)
    }

    @Test
    fun ignoresTemporarySleepAndMapsOnlyDocumentedStages() {
        val record = SleepSyncBean().apply {
            asleepTime = 1_700_000_000L
            awakeTime = 1_700_028_800L
            items = listOf(
                SleepItemBean().apply { sleepType = 2; len = 90; isTemporary = 0 },
                SleepItemBean().apply { sleepType = 1; len = 240; isTemporary = 0 },
                SleepItemBean().apply { sleepType = 0; len = 30; isTemporary = 0 },
                SleepItemBean().apply { sleepType = 2; len = 999; isTemporary = 1 },
            )
        }

        val sleep = RwFitVendorDataMapper.sleep(listOf(record)).sleep.single()

        assertEquals(90, sleep.deepMinutes)
        assertEquals(240, sleep.lightMinutes)
        assertEquals(30, sleep.awakeMinutes)
        assertEquals(1_700_000_000_000L, sleep.startedAt)
    }

    @Test
    fun manualMapperRejectsUnsupportedTypes() {
        val payload = RwFitVendorDataMapper.realTime(
            RingMetricType.TEMPERATURE,
            timestamp = 1_700_000_000L,
            value = 36,
            observedAt = 1_700_000_000_000L,
        )

        assertTrue(payload.measurements.isEmpty())
    }
}
