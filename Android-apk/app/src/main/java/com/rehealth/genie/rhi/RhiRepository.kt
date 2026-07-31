package com.rehealth.genie.rhi

import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.data.sync.InterventionFeedbackEntity
import com.rehealth.genie.network.PatientProfilePayload
import com.rehealth.genie.ring.data.RingDataDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RhiRepository(
    private val ringDataDao: RingDataDao,
    private val manualInputDao: RhiManualHealthInputDao? = null,
    private val interventionFeedbackDao: InterventionFeedbackDao? = null,
    private val userIdProvider: () -> String? = { null },
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun refreshPeriod(
        periodDays: Int,
        scoredOn: LocalDate = LocalDate.now(zoneId),
        profile: PatientProfilePayload? = null,
    ): RhiPeriodSummary {
        require(periodDays in setOf(7, 30, 90)) { "RHI period must be 7, 30, or 90 days" }
        val historyWarmupDays = 42
        val since = scoredOn.minusDays((periodDays + historyWarmupDays).toLong())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val activities = ringDataDao.getActivitiesSince(since)
        val sleepSessions = ringDataDao.getSleepSessionsSince(since)
        val measurements = ringDataDao.getMeasurementsSince(since)
        val manual = userIdProvider()?.let { manualInputDao?.get(it) }
        val feedback = interventionFeedbackDao?.completedFeedbackSince(since).orEmpty()
        return withContext(Dispatchers.Default) {
            val firstDate = scoredOn.minusDays((periodDays + 27).toLong())
            var previousDisplay: Double? = null
            var previousConfidence: Double? = null
            val daily = generateSequence(firstDate) { date ->
                date.plusDays(1).takeIf { it <= scoredOn }
            }.map { date ->
                val adherence = feedback.adherenceFor(date, zoneId)
                val result = RhiLiteEngine.calculate(
                    RhiLiteCalculationInput(
                        scoredOn = date,
                        zoneId = zoneId,
                        activities = activities,
                        sleepSessions = sleepSessions,
                        measurements = measurements,
                        previousDisplayScore = previousDisplay,
                        previousConfidence = previousConfidence,
                        context = RhiContextInput(
                            manual = manual,
                            profileBmi = profile.bmiValue(),
                            profileObservedAt = profile?.updatedAt,
                            age = profile?.age,
                            biologicalSex = profile?.gender,
                            nicotineExposure = profile?.smoking?.let { if (it) 1 else 0 },
                            diabetesStatus = profile?.diabetesHistory?.let { if (it) 1 else 0 },
                            antihypertensiveMedication = profile.medicationFlag(
                                "降压", "氨氯地平", "硝苯地平", "缬沙坦", "厄贝沙坦",
                                "替米沙坦", "贝那普利", "依那普利", "美托洛尔",
                            ),
                            lipidLoweringMedication = profile.medicationFlag(
                                "降脂", "他汀", "依折麦布", "非诺贝特",
                            ),
                            prematureCvdFamilyHistory = profile?.familyHistory?.let { if (it) 1 else 0 },
                            adherencePercent = adherence?.first,
                            adherenceConfidence = adherence?.second ?: 0.0,
                        ),
                    ),
                )
                val isValid = result.confidence >= RhiPeriodAggregator.MIN_VALID_CONFIDENCE &&
                    result.availableFeatureCount > 0 &&
                    result.availableDays > 0
                if (isValid) {
                    previousDisplay = result.displayScore
                    previousConfidence = result.confidence
                }
                date to result
            }.toList()
            val periodStart = scoredOn.minusDays((periodDays - 1).toLong())
            val valid = daily.mapNotNull { (date, result) ->
                result.takeIf {
                    date >= periodStart &&
                        it.confidence >= RhiPeriodAggregator.MIN_VALID_CONFIDENCE &&
                        it.availableFeatureCount > 0 &&
                        it.availableDays > 0
                }?.let {
                    RhiDailyScore(
                        date = date,
                        score = it.displayScore,
                        confidence = it.confidence,
                    )
                }
            }
            RhiPeriodAggregator.summarize(
                periodDays = periodDays,
                current = valid.lastOrNull { it.date == scoredOn },
                dailyScores = valid,
            )
        }
    }
}

private fun PatientProfilePayload?.medicationFlag(vararg keywords: String): Int? {
    val medications = this?.medications ?: return null
    return if (medications.any { medication -> keywords.any(medication::contains) }) 1 else 0
}

private fun PatientProfilePayload?.bmiValue(): Double? {
    this ?: return null
    bmi?.takeIf { it.isFinite() && it in 10.0..80.0 }?.let { return it }
    val heightM = heightCm?.takeIf { it.isFinite() && it in 80.0..250.0 }?.div(100.0)
    val weight = weightKg?.takeIf { it.isFinite() && it in 20.0..350.0 }
    return if (heightM != null && weight != null) {
        (weight / (heightM * heightM)).takeIf { it in 10.0..80.0 }
    } else {
        null
    }
}

private fun List<InterventionFeedbackEntity>.adherenceFor(
    scoredOn: LocalDate,
    zoneId: ZoneId,
): Pair<Double, Double>? {
    val end = scoredOn.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val start = scoredOn.minusDays(27).atStartOfDay(zoneId).toInstant().toEpochMilli()
    val applicable = filter { it.checkedAt in start until end && it.status != "not_applicable" }
    if (applicable.isEmpty()) return null
    val percent = applicable.map {
        when (it.status) {
            "completed" -> 100.0
            "partially_completed" -> 50.0
            "skipped" -> 0.0
            else -> 0.0
        }
    }.average()
    val confidence = (applicable.size / 7.0 * 0.80).coerceIn(0.0, 0.80)
    return percent to confidence
}
