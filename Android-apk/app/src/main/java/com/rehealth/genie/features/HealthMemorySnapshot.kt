package com.rehealth.genie.features

import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.ring.data.RingActivityEntity
import com.rehealth.genie.ring.data.RingMeasurementEntity
import com.rehealth.genie.ring.data.RingSleepSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

data class HealthMemorySnapshot(
    val profile: BaselineHealthProfile? = null,
    val labs: ClinicalLabValues? = null,
    val ringMeasurements: List<RingMeasurementEntity> = emptyList(),
    val ringActivities: List<RingActivityEntity> = emptyList(),
    val ringSleepSessions: List<RingSleepSessionEntity> = emptyList(),
    val interviewBaselineText: String? = null,
    val interviewUpdatedAt: Long? = null,
) {
    val sleepSummary: SleepSummary?
        get() = summarizeSleep(ringSleepSessions)

    companion object {
        fun fromPatientProfile(
            profile: PatientProfilePayload?,
            labs: ClinicalLabValues? = null,
            ringMeasurements: List<RingMeasurementEntity> = emptyList(),
            ringActivities: List<RingActivityEntity> = emptyList(),
            ringSleepSessions: List<RingSleepSessionEntity> = emptyList(),
            interviewBaselineText: String? = null,
            interviewUpdatedAt: Long? = null,
        ): HealthMemorySnapshot =
            HealthMemorySnapshot(
                profile = profile?.let {
                    BaselineHealthProfile(
                        age = it.age,
                        gender = it.gender,
                        heightCm = it.heightCm,
                        weightKg = it.weightKg,
                        bmi = it.bmi,
                        smoking = it.smoking,
                        drinking = it.drinking,
                        diabetesHistory = it.diabetesHistory,
                        hypertensionHistory = it.hypertensionHistory,
                        familyHistory = it.familyHistory,
                        updatedAt = it.updatedAt,
                    )
                },
                labs = labs,
                ringMeasurements = ringMeasurements,
                ringActivities = ringActivities,
                ringSleepSessions = ringSleepSessions,
                interviewBaselineText = interviewBaselineText,
                interviewUpdatedAt = interviewUpdatedAt,
            )
    }
}

data class BaselineHealthProfile(
    val age: Int? = null,
    val gender: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bmi: Double? = null,
    val smoking: Boolean? = null,
    val drinking: Boolean? = null,
    val diabetesHistory: Boolean? = null,
    val hypertensionHistory: Boolean? = null,
    val familyHistory: Boolean? = null,
    val updatedAt: Long? = null,
)

data class ClinicalLabValues(
    val fastingGlucose: Double? = null,
    val totalCholesterol: Double? = null,
    val ldl: Double? = null,
    val hdl: Double? = null,
    val triglycerides: Double? = null,
    val recordedAt: Long? = null,
)

data class SleepSummary(
    val latestTotalMinutes: Int,
    val latestEndedAt: Long,
    val sevenDayAverageMinutes: Double,
    val nights: Int,
)

private fun summarizeSleep(sessions: List<RingSleepSessionEntity>): SleepSummary? {
    val validSessions = sessions
        .filter { it.endedAt > it.startedAt }
        .mapNotNull { session ->
            val total = session.deepMinutes + session.lightMinutes + session.remMinutes
            if (total <= 0) null else session to total
        }
        .sortedByDescending { it.first.endedAt }
    val latest = validSessions.firstOrNull() ?: return null
    val latestDate = Instant.ofEpochMilli(latest.first.endedAt).atZone(ZoneOffset.UTC).toLocalDate()
    val recent = validSessions.filter { (session, _) ->
        val date = Instant.ofEpochMilli(session.endedAt).atZone(ZoneOffset.UTC).toLocalDate()
        !date.isBefore(latestDate.minusDays(6))
    }
    return SleepSummary(
        latestTotalMinutes = latest.second,
        latestEndedAt = latest.first.endedAt,
        sevenDayAverageMinutes = recent.map { it.second }.average(),
        nights = recent.map { it.first.endedAt.toUtcDate() }.distinct().size,
    )
}

private fun Long.toUtcDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
