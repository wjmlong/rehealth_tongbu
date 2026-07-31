package com.rehealth.genie.rhi

import com.rehealth.genie.network.dto.RhiV2FeatureFields
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RhiRemoteRequestMappingTest {
    private val zoneId = ZoneId.of("Asia/Shanghai")
    private val scoredOn = LocalDate.of(2026, 7, 31)

    @Test
    fun `remote request maps all 32 fields and keeps provenance timestamps`() {
        val profileObservedAt = scoredOn.minusDays(90).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val manualObservedAt = scoredOn.minusDays(2).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val labObservedAt = scoredOn.minusDays(10).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val manual = RhiManualHealthInputEntity(
            userId = "user",
            sedentaryHoursPerDay = 6.0,
            waistCircumferenceCm = 84.0,
            vo2MaxMlKgMin = 38.0,
            hba1cPercent = 5.3,
            egfrMlMin173m2 = 96.0,
            cuffSbp7dMean = 118.0,
            cuffDbp7dMean = 76.0,
            cuffValidDays = 7,
            cuffConfirmed = true,
            totalCholesterolMmolL = 4.5,
            ldlMmolL = 2.5,
            hdlMmolL = 1.35,
            triglyceridesMmolL = 1.1,
            labConfirmed = true,
            labRecordedAt = labObservedAt,
            updatedAt = manualObservedAt,
        )
        val input = RhiLiteCalculationInput(
            scoredOn = scoredOn,
            zoneId = zoneId,
            activities = emptyList(),
            sleepSessions = emptyList(),
            measurements = emptyList(),
            previousDisplayScore = 70.0,
            context = RhiContextInput(
                manual = manual,
                profileObservedAt = profileObservedAt,
                age = 50,
                biologicalSex = "male",
                nicotineExposure = 0,
                diabetesStatus = 0,
                antihypertensiveMedication = 0,
                lipidLoweringMedication = 0,
                prematureCvdFamilyHistory = 0,
            ),
        )
        val featureValues = RhiV2FeatureFields.ALL
            .filterNot { it == "biological_sex" }
            .associateWith { field ->
                RhiExtractedFeature(value = validValue(field), confidence = 0.9)
            }
        val calculation = RhiLiteCalculation(
            rawScore = 72.0,
            displayScore = 70.5,
            confidence = 0.9,
            availableDays = 90,
            availableFeatureCount = 32,
            domains = emptyMap(),
            features = featureValues,
        )

        val request = input.toRemoteRequest(calculation)

        assertEquals(32, request.featureQuality.size)
        assertTrue(request.featureQuality.values.none { it.status == "MISSING" })
        assertEquals("male", request.featureVector.biologicalSex)
        assertEquals(50, request.featureVector.age)
        assertEquals(118.0, request.featureVector.sbp7dMean)
        assertEquals("USER_REPORTED", request.featureQuality.getValue("sbp_7d_mean").source)
        assertEquals(manualObservedAt, request.featureQuality.getValue("sbp_7d_mean").observedAt)
        assertEquals("CLINICAL_REPORT", request.featureQuality.getValue("ldl_c").source)
        assertEquals(labObservedAt, request.featureQuality.getValue("ldl_c").observedAt)
        assertEquals("USER_REPORTED", request.featureQuality.getValue("age").source)
        assertEquals(profileObservedAt, request.featureQuality.getValue("age").observedAt)
        assertEquals("hba1c_percent", request.glycemiaMetric)
    }

    private fun validValue(field: String): Double = when (field) {
        "age" -> 50.0
        "waist_circumference_cm" -> 84.0
        "bmi" -> 22.4
        "sbp_7d_mean" -> 118.0
        "dbp_7d_mean" -> 76.0
        "total_cholesterol" -> 4.5
        "hdl_c" -> 1.35
        "ldl_c" -> 2.5
        "triglycerides" -> 1.1
        "glycemia_value" -> 5.3
        "egfr" -> 96.0
        "resting_hr_14d_median" -> 68.0
        "resting_hr_change_28d_pct" -> -5.0
        "nocturnal_hrv_14d_median" -> 52.0
        "hrv_change_28d_pct" -> 10.0
        "cardiorespiratory_fitness_score" -> 76.0
        "sleep_duration_7d_mean_hours" -> 7.8
        "sleep_regularity_14d_pct" -> 90.0
        "sleep_efficiency_14d_pct" -> 90.0
        "nocturnal_spo2_drop_burden_14d_pct" -> 0.0
        "steps_7d_mean" -> 8_000.0
        "mvpa_minutes_7d" -> 180.0
        "sedentary_hours_7d_mean" -> 6.0
        "active_day_regularity_14d_pct" -> 90.0
        "weight_change_28d_pct" -> -1.0
        "adherence_composite_28d_pct" -> 90.0
        else -> 0.0
    }
}
