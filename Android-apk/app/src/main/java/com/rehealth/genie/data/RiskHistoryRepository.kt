package com.rehealth.genie.data

import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.network.dto.RiskResultDto
import com.rehealth.genie.phm.AttributionHistoryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private fun dayFor(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))

    private companion object {
        const val MAX_HISTORY_DAYS = 30
    }
}
