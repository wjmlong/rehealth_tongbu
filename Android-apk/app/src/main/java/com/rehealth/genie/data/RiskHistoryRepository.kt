package com.rehealth.genie.data

import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.phm.AttributionHistoryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import kotlin.math.roundToInt

data class RiskPeriodSummary(
    val averageRiskScore: Double,
    val averageHealthIndex: Int,
    val daysWithScore: Int,
)

/** Keeps the real-score history used by PIAS separate from mock and UI-only values. */
class RiskHistoryRepository(
    private val riskHistoryDao: RiskHistoryDao,
    private val feedbackDao: InterventionFeedbackDao,
    private val userIdProvider: () -> String?,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun recordConfirmedRemoteRisk(result: RiskResultDto) {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return
        val score = result.normalizedRiskScore ?: return
        if (result.normalizedIsMock != false) return

        val evaluatedAt = nowProvider()
        riskHistoryDao.upsert(
            RiskHistoryEntity(
                userId = userId,
                evaluatedOn = dayFor(evaluatedAt),
                riskScore = score.coerceIn(0.0, 1.0),
                riskLevel = result.normalizedRiskLevel,
                evaluatedAt = evaluatedAt,
            ),
        )
    }

    suspend fun attributionHistory(limit: Int = MAX_HISTORY_DAYS): List<AttributionHistoryPoint> {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return emptyList()
        val history = riskHistoryDao.latestForUser(userId, limit).asReversed()
        if (history.isEmpty()) return emptyList()

        val completedDays = feedbackDao.completedFeedbackSince(history.first().evaluatedAt)
            .asSequence()
            .filter { it.status == "completed" || it.status == "partially_completed" }
            .map { dayFor(it.checkedAt) }
            .toSet()
        return history.map {
            AttributionHistoryPoint(
                date = it.evaluatedOn,
                riskScore = it.riskScore,
                isInterventionDay = it.evaluatedOn in completedDays,
            )
        }
    }

    suspend fun periodSummary(windowDays: Int): RiskPeriodSummary? {
        val userId = userIdProvider()?.takeIf { it.isNotBlank() } ?: return null
        val now = nowProvider()
        val since = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (windowDays > 1) add(Calendar.DAY_OF_YEAR, -(windowDays - 1))
        }.timeInMillis
        val scores = riskHistoryDao.sinceForUser(userId, since).map(RiskHistoryEntity::riskScore)
        if (scores.isEmpty()) return null
        val average = scores.average().coerceIn(0.0, 1.0)
        return RiskPeriodSummary(
            averageRiskScore = average,
            averageHealthIndex = ((1.0 - average) * 100.0).roundToInt().coerceIn(0, 100),
            daysWithScore = scores.size,
        )
    }

    private fun dayFor(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    private companion object {
        const val MAX_HISTORY_DAYS = 30
    }
}
