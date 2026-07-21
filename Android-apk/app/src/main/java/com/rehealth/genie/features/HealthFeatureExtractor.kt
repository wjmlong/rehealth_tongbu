package com.rehealth.genie.features

import com.rehealth.genie.ring.RingMetricType
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.pow

class HealthFeatureExtractor(
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    fun extract(snapshot: HealthMemorySnapshot): CvdFeatureVector {
        val quality = mutableMapOf<String, FeatureQuality>()
        val profile = snapshot.profile
        val labs = snapshot.labs
        val bloodPressure = extractBloodPressure(snapshot.ringMeasurements)
        val exercise = extractExerciseDays(snapshot.ringActivities, snapshot.ringMeasurements)

        val age = profile?.age?.takeIf { it in 18..120 }.also {
            quality[CvdFeatureFields.AGE] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported age available.",
                lowConfidence = profile?.age != null,
                lowConfidenceReason = "User-reported age is outside the supported adult CVD range.",
            )
        }
        val gender = normalizeGender(profile?.gender).also {
            quality[CvdFeatureFields.GENDER] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported gender available.",
                lowConfidence = profile?.gender != null,
                lowConfidenceReason = "User-reported gender could not be mapped to the model contract.",
            )
        }
        val bmi = extractBmi(profile).also {
            quality[CvdFeatureFields.BMI] = qualityForBmi(it, profile)
        }

        quality[CvdFeatureFields.SBP] = bloodPressure.sbpQuality
        quality[CvdFeatureFields.DBP] = bloodPressure.dbpQuality
        quality[CvdFeatureFields.EXERCISE_DAYS] = exercise.quality

        val fastingGlucose = labs?.fastingGlucose.validLabValue(CvdFeatureFields.FASTING_GLUCOSE, labs, quality)
        val totalCholesterol = labs?.totalCholesterol.validLabValue(CvdFeatureFields.TOTAL_CHOLESTEROL, labs, quality)
        val ldl = labs?.ldl.validLabValue(CvdFeatureFields.LDL, labs, quality)
        val hdl = labs?.hdl.validLabValue(CvdFeatureFields.HDL, labs, quality)
        val triglycerides = labs?.triglycerides.validLabValue(CvdFeatureFields.TRIGLYCERIDES, labs, quality)

        val smoking = profile?.smoking?.toIntFlag().also {
            quality[CvdFeatureFields.SMOKING] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported smoking status available.",
            )
        }
        val drinking = profile?.drinking?.toIntFlag().also {
            quality[CvdFeatureFields.DRINKING] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported drinking status available.",
            )
        }
        val diabetesHistory = profile?.diabetesHistory?.toIntFlag().also {
            quality[CvdFeatureFields.DIABETES_HISTORY] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported diabetes history available.",
            )
        }
        val hypertensionHistory = profile?.hypertensionHistory?.toIntFlag().also {
            quality[CvdFeatureFields.HYPERTENSION_HISTORY] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported hypertension history available.",
            )
        }
        val familyHistory = profile?.familyHistory?.toIntFlag().also {
            quality[CvdFeatureFields.FAMILY_HISTORY] = qualityForProfileValue(
                value = it,
                observedAt = profile?.updatedAt,
                missingReason = "No user-reported family history available.",
            )
        }

        CvdFeatureFields.ALL.forEach { field ->
            quality.putIfAbsent(field, FeatureQuality.missing("Field was not produced by the extractor."))
        }

        return CvdFeatureVector(
            age = age,
            gender = gender,
            bmi = bmi,
            sbp = bloodPressure.sbp,
            dbp = bloodPressure.dbp,
            fastingGlucose = fastingGlucose,
            totalCholesterol = totalCholesterol,
            ldl = ldl,
            hdl = hdl,
            triglycerides = triglycerides,
            exerciseDays = exercise.days,
            smoking = smoking,
            drinking = drinking,
            diabetesHistory = diabetesHistory,
            hypertensionHistory = hypertensionHistory,
            familyHistory = familyHistory,
            featureQuality = quality.toMap(),
        )
    }

    private fun extractBloodPressure(measurements: List<RingMeasurementEntity>): BloodPressureFeature {
        val bloodPressureRows = measurements
            .filter { it.metricType == RingMetricType.BLOOD_PRESSURE.name }
            .distinctBy { "${it.measuredAt}:${it.primaryValue}:${it.secondaryValue}" }
            .sortedByDescending { it.measuredAt }

        val latestValid = bloodPressureRows.firstOrNull { it.isValidBloodPressure() }
        if (latestValid != null) {
            val quality = FeatureQuality.valid(
                source = FeatureSource.REAL_DEVICE,
                observedAt = latestValid.measuredAt,
                reason = "Most recent plausible ring blood pressure measurement.",
            )
            return BloodPressureFeature(
                sbp = latestValid.primaryValue,
                dbp = latestValid.secondaryValue,
                sbpQuality = quality,
                dbpQuality = quality,
            )
        }

        if (bloodPressureRows.isNotEmpty()) {
            val latest = bloodPressureRows.first()
            val quality = FeatureQuality.lowConfidence(
                source = FeatureSource.REAL_DEVICE,
                observedAt = latest.measuredAt,
                reason = "Ring blood pressure rows were present but outside plausible physiological bounds.",
            )
            return BloodPressureFeature(sbpQuality = quality, dbpQuality = quality)
        }

        val missing = FeatureQuality.missing("No ring blood pressure measurement available.")
        return BloodPressureFeature(sbpQuality = missing, dbpQuality = missing)
    }

    private fun extractExerciseDays(
        activities: List<RingActivityEntity>,
        measurements: List<RingMeasurementEntity>,
    ): ExerciseFeature {
        val now = nowProvider()
        val today = now.toUtcDate()
        val startDate = today.minusDays(EXERCISE_LOOKBACK_DAYS - 1)
        val validActivityDates = activities
            .filter { it.startedAt <= now && it.isPlausibleActivity() }
            .mapNotNull { activity ->
                val date = activity.startedAt.toUtcDate()
                if (date in startDate..today && activity.qualifiesAsExerciseDay()) date else null
            }
        val validStepDates = measurements
            .filter { it.metricType == RingMetricType.STEPS.name && it.measuredAt <= now && it.isPlausibleStepMeasurement() }
            .mapNotNull { measurement ->
                val date = measurement.measuredAt.toUtcDate()
                if (date in startDate..today && measurement.primaryValue >= DAILY_STEP_EXERCISE_THRESHOLD) date else null
            }
        val validDataPresent = activities.any { it.startedAt <= now && it.isPlausibleActivity() } ||
            measurements.any { it.metricType == RingMetricType.STEPS.name && it.measuredAt <= now && it.isPlausibleStepMeasurement() }
        val invalidDataPresent = activities.any { it.startedAt <= now && !it.isPlausibleActivity() } ||
            measurements.any { it.metricType == RingMetricType.STEPS.name && it.measuredAt <= now && !it.isPlausibleStepMeasurement() }

        if (validDataPresent) {
            val latestAt = listOf(
                activities.filter { it.isPlausibleActivity() }.maxOfOrNull { it.startedAt },
                measurements.filter { it.metricType == RingMetricType.STEPS.name && it.isPlausibleStepMeasurement() }
                    .maxOfOrNull { it.measuredAt },
            ).filterNotNull().maxOrNull()
            return ExerciseFeature(
                days = (validActivityDates + validStepDates).distinct().size.coerceIn(0, EXERCISE_LOOKBACK_DAYS.toInt()),
                quality = FeatureQuality.valid(
                    source = FeatureSource.REAL_DEVICE,
                    observedAt = latestAt,
                    reason = "Derived from ring activity rows and/or daily step measurements over the last 7 days.",
                ),
            )
        }

        if (invalidDataPresent) {
            return ExerciseFeature(
                quality = FeatureQuality.lowConfidence(
                    source = FeatureSource.REAL_DEVICE,
                    reason = "Ring activity or step rows were present but outside plausible bounds.",
                ),
            )
        }

        return ExerciseFeature(quality = FeatureQuality.missing("No ring activity or step data available."))
    }

    private fun Double?.validLabValue(
        field: String,
        labs: ClinicalLabValues?,
        quality: MutableMap<String, FeatureQuality>,
    ): Double? {
        val value = this
        return when {
            value == null -> {
                quality[field] = FeatureQuality.missing("No clinical lab value available for $field.")
                null
            }
            value.isFinite() && value > 0.0 && value <= MAX_REASONABLE_LAB_VALUE -> {
                quality[field] = FeatureQuality.valid(
                    source = FeatureSource.CLINICAL_REPORT,
                    observedAt = labs?.recordedAt,
                    reason = "Provided by clinical lab/report input.",
                )
                value
            }
            else -> {
                quality[field] = FeatureQuality.lowConfidence(
                    source = FeatureSource.CLINICAL_REPORT,
                    observedAt = labs?.recordedAt,
                    reason = "Clinical lab value for $field is outside broad plausible bounds.",
                )
                null
            }
        }
    }

    private fun qualityForProfileValue(
        value: Any?,
        observedAt: Long?,
        missingReason: String,
        lowConfidence: Boolean = false,
        lowConfidenceReason: String = missingReason,
    ): FeatureQuality =
        when {
            value != null -> FeatureQuality.valid(
                source = FeatureSource.USER_REPORTED,
                observedAt = observedAt,
                reason = "Provided by baseline profile or interview.",
            )
            lowConfidence -> FeatureQuality.lowConfidence(
                source = FeatureSource.USER_REPORTED,
                observedAt = observedAt,
                reason = lowConfidenceReason,
            )
            else -> FeatureQuality.missing(missingReason)
        }

    private fun qualityForBmi(value: Double?, profile: BaselineHealthProfile?): FeatureQuality {
        if (value == null) {
            val lowConfidence = profile?.bmi != null || profile?.heightCm != null || profile?.weightKg != null
            return if (lowConfidence) {
                FeatureQuality.lowConfidence(
                    source = FeatureSource.USER_REPORTED,
                    observedAt = profile?.updatedAt,
                    reason = "BMI or height/weight inputs were present but outside plausible bounds.",
                )
            } else {
                FeatureQuality.missing("No BMI or height/weight profile data available.")
            }
        }
        return FeatureQuality.valid(
            source = if (profile?.bmi != null) FeatureSource.USER_REPORTED else FeatureSource.DERIVED,
            observedAt = profile?.updatedAt,
            reason = if (profile?.bmi != null) {
                "Provided by baseline profile or interview."
            } else {
                "Derived from user-reported height and weight."
            },
        )
    }

    private companion object {
        const val EXERCISE_LOOKBACK_DAYS = 7L
        const val DAILY_STEP_EXERCISE_THRESHOLD = 7_000.0
        const val MAX_REASONABLE_LAB_VALUE = 1_000.0
    }
}

private data class BloodPressureFeature(
    val sbp: Double? = null,
    val dbp: Double? = null,
    val sbpQuality: FeatureQuality,
    val dbpQuality: FeatureQuality,
)

private data class ExerciseFeature(
    val days: Int? = null,
    val quality: FeatureQuality,
)

private fun RingMeasurementEntity.isValidBloodPressure(): Boolean {
    val dbp = secondaryValue ?: return false
    return primaryValue.isFinite() &&
        dbp.isFinite() &&
        primaryValue in 70.0..250.0 &&
        dbp in 40.0..150.0 &&
        primaryValue > dbp
}

private fun RingMeasurementEntity.isPlausibleStepMeasurement(): Boolean =
    primaryValue.isFinite() && primaryValue in 0.0..100_000.0

private fun RingActivityEntity.isPlausibleActivity(): Boolean =
    steps in 0..100_000 &&
        distanceMeters.isFinite() &&
        distanceMeters >= 0.0 &&
        caloriesKcal.isFinite() &&
        caloriesKcal >= 0.0 &&
        durationMinutes in 0..1_440

private fun RingActivityEntity.qualifiesAsExerciseDay(): Boolean =
    durationMinutes >= 20 || steps >= 7_000

private fun extractBmi(profile: BaselineHealthProfile?): Double? {
    val reported = profile?.bmi
    if (reported != null && reported.isFinite() && reported in 10.0..80.0) {
        return reported
    }
    val heightMeters = profile?.heightCm?.takeIf { it.isFinite() && it in 80.0..250.0 }?.div(100.0)
    val weightKg = profile?.weightKg?.takeIf { it.isFinite() && it in 20.0..350.0 }
    val derived = if (heightMeters != null && weightKg != null) weightKg / heightMeters.pow(2) else null
    return derived?.takeIf { it.isFinite() && it in 10.0..80.0 }
}

private fun normalizeGender(value: String?): Int? =
    when (value?.trim()?.lowercase()) {
        "male", "m", "man", "男", "男性" -> 1
        "female", "f", "woman", "女", "女性" -> 0
        else -> null
    }

private fun Boolean.toIntFlag(): Int = if (this) 1 else 0

private fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
