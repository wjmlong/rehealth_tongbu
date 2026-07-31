package com.rehealth.genie.rhi

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhiLiteEngineTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 7, 31)

    @Test
    fun `more recent steps produce a higher RHI than fewer steps`() {
        val improved = calculate(currentSteps = 8_000)
        val worsened = calculate(currentSteps = 1_000)

        assertTrue(improved.rawScore > worsened.rawScore)
        assertTrue(improved.domains.getValue("activity_fitness")!! > 50.0)
        assertTrue(worsened.domains.getValue("activity_fitness")!! < 50.0)
    }

    @Test
    fun `missing wearable data stays neutral with zero confidence`() {
        val result = RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = emptyList(),
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = null,
            ),
        )

        assertEquals(50.0, result.rawScore)
        assertEquals(0.0, result.confidence)
        assertEquals(0, result.availableFeatureCount)
    }

    @Test
    fun `display score uses RHI quarter smoothing`() {
        val raw = calculate(currentSteps = 8_000, previousDisplay = null)
        val smoothed = calculate(currentSteps = 8_000, previousDisplay = 50.0)

        assertEquals(
            kotlin.math.round((0.25 * raw.rawScore + 0.75 * 50.0) * 10.0) / 10.0,
            smoothed.displayScore,
        )
    }

    @Test
    fun `falling confidence cannot improve the displayed RHI`() {
        val result = RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = emptyList(),
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = 40.0,
                previousConfidence = 0.8,
            ),
        )

        assertEquals(40.0, result.displayScore)
    }

    @Test
    fun `higher confirmed cuff blood pressure lowers hemodynamic RHI`() {
        val healthy = calculateWithMeasurements(
            measurements = emptyList(),
            context = cuffContext(118.0, 76.0),
        )
        val elevated = calculateWithMeasurements(
            measurements = emptyList(),
            context = cuffContext(165.0, 105.0),
        )

        assertTrue(
            healthy.domains.getValue("hemodynamic")!! >
                elevated.domains.getValue("hemodynamic")!!,
        )
    }

    @Test
    fun `cuffless wearable blood pressure does not enter RHI`() {
        val cuffless = calculateWithMeasurements(bloodPressureMeasurements(165.0, 105.0))
        val missing = calculateWithMeasurements(emptyList())

        assertEquals(
            missing.domains.getValue("hemodynamic"),
            cuffless.domains.getValue("hemodynamic"),
        )
        assertEquals(missing.confidence, cuffless.confidence)
    }

    @Test
    fun `Room metabolic measurements affect metabolic domain in healthy direction`() {
        val controlled = calculateWithMeasurements(
            listOf(
                metric(RingMetricType.BMI, 22.0, "kg/m²"),
                metric(RingMetricType.LDL_CHOLESTEROL, 2.4, "mmol/L"),
                metric(RingMetricType.HDL_CHOLESTEROL, 1.5, "mmol/L"),
                metric(RingMetricType.TRIGLYCERIDES, 1.3, "mmol/L"),
                metric(RingMetricType.BLOOD_GLUCOSE, 5.4, "mmol/L"),
            ),
        )
        val uncontrolled = calculateWithMeasurements(
            listOf(
                metric(RingMetricType.BMI, 34.0, "kg/m²"),
                metric(RingMetricType.LDL_CHOLESTEROL, 5.0, "mmol/L"),
                metric(RingMetricType.HDL_CHOLESTEROL, 0.6, "mmol/L"),
                metric(RingMetricType.TRIGLYCERIDES, 7.0, "mmol/L"),
                metric(RingMetricType.BLOOD_GLUCOSE, 10.0, "mmol/L"),
            ),
        )

        assertTrue(
            controlled.domains.getValue("metabolic_control")!! >
                uncontrolled.domains.getValue("metabolic_control")!!,
        )
    }

    @Test
    fun `metric specific mg per dL conversions match mmol per L inputs`() {
        val mmol = calculateWithMeasurements(
            listOf(
                metric(RingMetricType.LDL_CHOLESTEROL, 2.4, "mmol/L"),
                metric(RingMetricType.HDL_CHOLESTEROL, 1.5, "mmol/L"),
                metric(RingMetricType.TRIGLYCERIDES, 1.3, "mmol/L"),
                metric(RingMetricType.BLOOD_GLUCOSE, 5.4, "mmol/L"),
            ),
        )
        val mgDl = calculateWithMeasurements(
            listOf(
                metric(RingMetricType.LDL_CHOLESTEROL, 2.4 * 38.67, "mg/dL"),
                metric(RingMetricType.HDL_CHOLESTEROL, 1.5 * 38.67, "mg/dL"),
                metric(RingMetricType.TRIGLYCERIDES, 1.3 * 88.57, "mg/dL"),
                metric(RingMetricType.BLOOD_GLUCOSE, 5.4 * 18.0, "mg/dL"),
            ),
        )

        assertEquals(
            mmol.domains.getValue("metabolic_control")!!,
            mgDl.domains.getValue("metabolic_control")!!,
            0.1,
        )
    }

    @Test
    fun `confirmed manual health inputs affect RHI without fabricating missing fields`() {
        val updatedAt = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val healthier = calculateWithMeasurements(
            emptyList(),
            RhiContextInput(
                manual = RhiManualHealthInputEntity(
                    userId = "user",
                    sedentaryHoursPerDay = 5.0,
                    waistCircumferenceCm = 76.0,
                    vo2MaxMlKgMin = 45.0,
                    hba1cPercent = 5.4,
                    egfrMlMin173m2 = 100.0,
                    updatedAt = updatedAt,
                ),
            ),
        )
        val lessHealthy = calculateWithMeasurements(
            emptyList(),
            RhiContextInput(
                manual = RhiManualHealthInputEntity(
                    userId = "user",
                    sedentaryHoursPerDay = 12.0,
                    waistCircumferenceCm = 110.0,
                    vo2MaxMlKgMin = 18.0,
                    hba1cPercent = 9.0,
                    egfrMlMin173m2 = 45.0,
                    updatedAt = updatedAt,
                ),
            ),
        )

        assertTrue(healthier.rawScore > lessHealthy.rawScore)
        assertTrue(healthier.availableFeatureCount > 0)
    }

    @Test
    fun `all 32 core fields have an explicit source path`() {
        val updatedAt = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val sleep = (0L..41L).map { daysAgo ->
            val wakeDate = today.minusDays(daysAgo)
            RingSleepSessionEntity(
                id = "sleep-$daysAgo",
                startedAt = wakeDate.minusDays(1).atTime(23, 0).atZone(zoneId).toInstant().toEpochMilli(),
                endedAt = wakeDate.atTime(7, 0).atZone(zoneId).toInstant().toEpochMilli(),
                deepMinutes = 120,
                lightMinutes = 240,
                awakeMinutes = 30,
                remMinutes = 90,
                interruptionMinutes = 10,
                source = "TEST_DEVICE",
            )
        }
        val activities = (0L..34L).flatMap { daysAgo ->
            val date = today.minusDays(daysAgo)
            listOf(
                RingActivityEntity(
                    id = "activity-summary-$daysAgo",
                    startedAt = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                    endedAt = null,
                    activityType = "daily_summary",
                    steps = 7_000,
                    distanceMeters = 5_000.0,
                    caloriesKcal = 300.0,
                    durationMinutes = 0,
                    averageHeartRate = null,
                    source = "TEST_DEVICE",
                ),
                RingActivityEntity(
                    id = "activity-workout-$daysAgo",
                    startedAt = date.atTime(18, 0).atZone(zoneId).toInstant().toEpochMilli(),
                    endedAt = date.atTime(18, 30).atZone(zoneId).toInstant().toEpochMilli(),
                    activityType = "workout",
                    steps = 0,
                    distanceMeters = 0.0,
                    caloriesKcal = 150.0,
                    durationMinutes = 30,
                    averageHeartRate = 110.0,
                    source = "TEST_DEVICE",
                ),
            )
        }
        val measurements = buildList {
            (0L..41L).forEach { daysAgo ->
                val measuredAt = today.minusDays(daysAgo).atTime(3, 0)
                    .atZone(zoneId).toInstant().toEpochMilli()
                add(
                    RingMeasurementEntity(
                        id = "hr-$daysAgo",
                        metricType = RingMetricType.HEART_RATE.name,
                        measuredAt = measuredAt,
                        primaryValue = if (daysAgo <= 13) 62.0 else 67.0,
                        unit = "bpm",
                        quality = 95,
                        source = "TEST_DEVICE",
                    ),
                )
                add(
                    RingMeasurementEntity(
                        id = "hrv-$daysAgo",
                        metricType = RingMetricType.HRV.name,
                        measuredAt = measuredAt,
                        primaryValue = if (daysAgo <= 13) 52.0 else 47.0,
                        unit = "ms",
                        quality = 95,
                        source = "TEST_DEVICE",
                    ),
                )
                if (daysAgo <= 13) {
                    add(
                        RingMeasurementEntity(
                            id = "spo2-$daysAgo",
                            metricType = RingMetricType.BLOOD_OXYGEN.name,
                            measuredAt = measuredAt,
                            primaryValue = 96.0,
                            unit = "%",
                            quality = 90,
                            source = "TEST_DEVICE",
                        ),
                    )
                }
            }
            addAll(weightPair(daysAgo = 0, fatKg = 18.0, leanKg = 52.0))
            addAll(weightPair(daysAgo = 28, fatKg = 19.0, leanKg = 52.0))
        }
        val manual = RhiManualHealthInputEntity(
            userId = "user",
            sedentaryHoursPerDay = 7.0,
            waistCircumferenceCm = 82.0,
            vo2MaxMlKgMin = 38.0,
            hba1cPercent = 5.6,
            egfrMlMin173m2 = 96.0,
            cuffSbp7dMean = 120.0,
            cuffDbp7dMean = 78.0,
            cuffValidDays = 7,
            cuffConfirmed = true,
            totalCholesterolMmolL = 4.5,
            ldlMmolL = 2.6,
            hdlMmolL = 1.4,
            triglyceridesMmolL = 1.3,
            labConfirmed = true,
            labRecordedAt = today.minusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
            updatedAt = updatedAt,
        )

        val result = RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = activities,
                sleepSessions = sleep,
                measurements = measurements,
                previousDisplayScore = null,
                context = RhiContextInput(
                    manual = manual,
                    profileBmi = 22.5,
                    profileObservedAt = updatedAt,
                    age = 40,
                    biologicalSex = "male",
                    nicotineExposure = 0,
                    diabetesStatus = 0,
                    antihypertensiveMedication = 0,
                    lipidLoweringMedication = 0,
                    prematureCvdFamilyHistory = 0,
                    adherencePercent = 90.0,
                    adherenceConfidence = 0.8,
                ),
            ),
        )

        assertEquals(32, result.availableFeatureCount)
        assertEquals(7, result.availableDays)
    }

    private fun calculate(
        currentSteps: Int,
        previousDisplay: Double? = null,
    ): RhiLiteCalculation {
        val activities = (0L..34L).map { daysAgo ->
            val date = today.minusDays(daysAgo)
            RingActivityEntity(
                id = "steps-$daysAgo-$currentSteps",
                startedAt = date.atStartOfDay(zoneId).toInstant().toEpochMilli(),
                endedAt = null,
                activityType = "daily_summary",
                steps = if (daysAgo <= 6) currentSteps else 3_000,
                distanceMeters = 0.0,
                caloriesKcal = 0.0,
                durationMinutes = 0,
                averageHeartRate = null,
                source = "TEST_DEVICE",
            )
        }
        return RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = activities,
                sleepSessions = emptyList(),
                measurements = emptyList(),
                previousDisplayScore = previousDisplay,
            ),
        )
    }

    private fun calculateWithMeasurements(
        measurements: List<RingMeasurementEntity>,
        context: RhiContextInput = RhiContextInput(),
    ): RhiLiteCalculation =
        RhiLiteEngine.calculate(
            RhiLiteCalculationInput(
                scoredOn = today,
                zoneId = zoneId,
                activities = emptyList(),
                sleepSessions = emptyList(),
                measurements = measurements,
                previousDisplayScore = null,
                context = context,
            ),
        )

    private fun cuffContext(sbp: Double, dbp: Double): RhiContextInput =
        RhiContextInput(
            manual = RhiManualHealthInputEntity(
                userId = "user",
                cuffSbp7dMean = sbp,
                cuffDbp7dMean = dbp,
                cuffValidDays = 7,
                cuffConfirmed = true,
                updatedAt = today.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            ),
        )

    private fun bloodPressureMeasurements(sbp: Double, dbp: Double): List<RingMeasurementEntity> =
        (0L..6L).map { daysAgo ->
            metric(
                RingMetricType.BLOOD_PRESSURE,
                sbp,
                "mmHg",
                secondaryValue = dbp,
                daysAgo = daysAgo,
            )
        }

    private fun weightPair(
        daysAgo: Long,
        fatKg: Double,
        leanKg: Double,
    ): List<RingMeasurementEntity> {
        val measuredAt = today.minusDays(daysAgo).atTime(12, 0)
            .atZone(zoneId).toInstant().toEpochMilli()
        return listOf(
            RingMeasurementEntity(
                id = "fat-$daysAgo",
                metricType = RingMetricType.FAT_MASS.name,
                measuredAt = measuredAt,
                primaryValue = fatKg,
                unit = "kg",
                quality = 90,
                source = "TEST_SCALE",
            ),
            RingMeasurementEntity(
                id = "lean-$daysAgo",
                metricType = RingMetricType.FAT_FREE_MASS.name,
                measuredAt = measuredAt,
                primaryValue = leanKg,
                unit = "kg",
                quality = 90,
                source = "TEST_SCALE",
            ),
        )
    }

    private fun metric(
        type: RingMetricType,
        value: Double,
        unit: String,
        secondaryValue: Double? = null,
        daysAgo: Long = 0,
    ): RingMeasurementEntity =
        RingMeasurementEntity(
            id = "${type.name}-$value-$daysAgo",
            metricType = type.name,
            measuredAt = today.minusDays(daysAgo).atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli(),
            primaryValue = value,
            secondaryValue = secondaryValue,
            unit = unit,
            quality = 90,
            source = "TEST_DEVICE",
        )
}
