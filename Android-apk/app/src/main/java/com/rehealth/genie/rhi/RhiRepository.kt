package com.rehealth.genie.rhi

import com.rehealth.genie.ring.data.RingDataDao
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RhiRepository(
    private val ringDataDao: RingDataDao,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    suspend fun refreshPeriod(
        periodDays: Int,
        scoredOn: LocalDate = LocalDate.now(zoneId),
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
        return withContext(Dispatchers.Default) {
            val firstDate = scoredOn.minusDays((periodDays + 27).toLong())
            var previousDisplay: Double? = null
            var previousConfidence: Double? = null
            val daily = generateSequence(firstDate) { date ->
                date.plusDays(1).takeIf { it <= scoredOn }
            }.map { date ->
                val result = RhiLiteEngine.calculate(
                    RhiLiteCalculationInput(
                        scoredOn = date,
                        zoneId = zoneId,
                        activities = activities,
                        sleepSessions = sleepSessions,
                        measurements = measurements,
                        previousDisplayScore = previousDisplay,
                        previousConfidence = previousConfidence,
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
