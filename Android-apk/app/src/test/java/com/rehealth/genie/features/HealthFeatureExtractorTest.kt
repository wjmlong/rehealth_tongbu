package com.rehealth.genie.features

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthFeatureExtractorTest {
    private val now = 1_720_000_000_000L
    private val extractor = HealthFeatureExtractor(nowProvider = { now })

    @Test
    fun noDataReturnsMissingQualityForAllContractFields() {
        val vector = extractor.extract(HealthMemorySnapshot())

        assertTrue(vector.asModelInput().values.all { it == null })
        assertEquals(CvdFeatureFields.ALL.toSet(), vector.featureQuality.keys)
        assertTrue(vector.featureQuality.values.all { it.status == FeatureQualityStatus.MISSING })
    }

    @Test
    fun partialProfileDataProducesUserReportedAndDerivedFields() {
        val snapshot = HealthMemorySnapshot(
            profile = BaselineHealthProfile(
                age = 45,
                gender = "male",
                heightCm = 180.0,
                weightKg = 81.0,
                smoking = false,
                drinking = true,
                diabetesHistory = false,
                hypertensionHistory = true,
                familyHistory = false,
                updatedAt = now - 1_000,
            ),
        )

        val vector = extractor.extract(snapshot)

        assertEquals(45, vector.age)
        assertEquals(1, vector.gender)
        assertEquals(25.0, vector.bmi!!, 0.01)
        assertEquals(0, vector.smoking)
        assertEquals(1, vector.drinking)
        assertEquals(0, vector.diabetesHistory)
        assertEquals(1, vector.hypertensionHistory)
        assertEquals(0, vector.familyHistory)
        assertEquals(FeatureSource.DERIVED, vector.featureQuality.getValue(CvdFeatureFields.BMI).source)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.SBP).status)
    }

    @Test
    fun bloodPressurePresentUsesMostRecentPlausibleRingMeasurement() {
        val snapshot = HealthMemorySnapshot(
            ringMeasurements = listOf(
                bp("older", now - 20_000, 122.0, 80.0),
                bp("latest", now - 10_000, 118.0, 76.0),
            ),
        )

        val vector = extractor.extract(snapshot)

        assertEquals(118.0, vector.sbp!!, 0.0)
        assertEquals(76.0, vector.dbp!!, 0.0)
        assertEquals(FeatureSource.REAL_DEVICE, vector.featureQuality.getValue(CvdFeatureFields.SBP).source)
        assertEquals(FeatureQualityStatus.VALID, vector.featureQuality.getValue(CvdFeatureFields.DBP).status)
    }

    @Test
    fun activityPresentDerivesExerciseDaysFromActivitiesAndSteps() {
        val dayMs = 86_400_000L
        val snapshot = HealthMemorySnapshot(
            ringActivities = listOf(
                activity("walk", now - dayMs, steps = 4_000, durationMinutes = 25),
                activity("short", now - (dayMs * 2), steps = 2_000, durationMinutes = 10),
            ),
            ringMeasurements = listOf(
                steps("steps", now - (dayMs * 3), 8_200.0),
            ),
        )

        val vector = extractor.extract(snapshot)

        assertEquals(2, vector.exerciseDays)
        assertEquals(FeatureQualityStatus.VALID, vector.featureQuality.getValue(CvdFeatureFields.EXERCISE_DAYS).status)
    }

    @Test
    fun abnormalOutlierValuesAreRejectedWithLowConfidenceQuality() {
        val snapshot = HealthMemorySnapshot(
            profile = BaselineHealthProfile(age = 145, bmi = 120.0, gender = "not-known"),
            labs = ClinicalLabValues(fastingGlucose = -1.0),
            ringMeasurements = listOf(
                bp("invalid-bp", now - 1_000, 320.0, 40.0),
                steps("invalid-steps", now - 1_000, -500.0),
            ),
            ringActivities = listOf(activity("invalid-activity", now - 1_000, steps = -1, durationMinutes = 30)),
        )

        val vector = extractor.extract(snapshot)

        assertNull(vector.age)
        assertNull(vector.gender)
        assertNull(vector.bmi)
        assertNull(vector.sbp)
        assertNull(vector.exerciseDays)
        assertNull(vector.fastingGlucose)
        assertEquals(FeatureQualityStatus.LOW_CONFIDENCE, vector.featureQuality.getValue(CvdFeatureFields.AGE).status)
        assertEquals(FeatureQualityStatus.LOW_CONFIDENCE, vector.featureQuality.getValue(CvdFeatureFields.SBP).status)
        assertEquals(FeatureQualityStatus.LOW_CONFIDENCE, vector.featureQuality.getValue(CvdFeatureFields.EXERCISE_DAYS).status)
        assertEquals(FeatureQualityStatus.LOW_CONFIDENCE, vector.featureQuality.getValue(CvdFeatureFields.FASTING_GLUCOSE).status)
    }

    @Test
    fun duplicateMeasurementsDoNotChangeLatestBloodPressureSelection() {
        val duplicateA = bp("dup-a", now - 5_000, 128.0, 82.0)
        val duplicateB = duplicateA.copy(id = "dup-b")
        val snapshot = HealthMemorySnapshot(
            ringMeasurements = listOf(
                bp("older", now - 50_000, 130.0, 84.0),
                duplicateA,
                duplicateB,
            ),
        )

        val vector = extractor.extract(snapshot)

        assertEquals(128.0, vector.sbp!!, 0.0)
        assertEquals(82.0, vector.dbp!!, 0.0)
        assertEquals(FeatureQualityStatus.VALID, vector.featureQuality.getValue(CvdFeatureFields.SBP).status)
    }

    @Test
    fun missingLabValuesRemainMissingAndAreNotInvented() {
        val snapshot = HealthMemorySnapshot(
            profile = BaselineHealthProfile(age = 50),
            labs = ClinicalLabValues(recordedAt = now),
        )

        val vector = extractor.extract(snapshot)

        assertNull(vector.fastingGlucose)
        assertNull(vector.totalCholesterol)
        assertNull(vector.ldl)
        assertNull(vector.hdl)
        assertNull(vector.triglycerides)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.FASTING_GLUCOSE).status)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.TOTAL_CHOLESTEROL).status)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.LDL).status)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.HDL).status)
        assertEquals(FeatureQualityStatus.MISSING, vector.featureQuality.getValue(CvdFeatureFields.TRIGLYCERIDES).status)
    }

    private fun bp(id: String, measuredAt: Long, sbp: Double, dbp: Double) =
        RingMeasurementEntity(
            id = id,
            metricType = RingMetricType.BLOOD_PRESSURE.name,
            measuredAt = measuredAt,
            primaryValue = sbp,
            secondaryValue = dbp,
            unit = "mmHg",
            source = "ring",
        )

    private fun steps(id: String, measuredAt: Long, value: Double) =
        RingMeasurementEntity(
            id = id,
            metricType = RingMetricType.STEPS.name,
            measuredAt = measuredAt,
            primaryValue = value,
            unit = "steps",
            source = "ring",
        )

    private fun activity(id: String, startedAt: Long, steps: Int, durationMinutes: Int) =
        RingActivityEntity(
            id = id,
            startedAt = startedAt,
            endedAt = startedAt + durationMinutes * 60_000L,
            activityType = "walking",
            steps = steps,
            distanceMeters = steps * 0.7,
            caloriesKcal = 100.0,
            durationMinutes = durationMinutes,
            averageHeartRate = 95.0,
            source = "ring",
        )
}
