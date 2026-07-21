package com.rehealth.genie.data

import com.rehealth.genie.data.sync.InterventionFeedbackDao
import com.rehealth.genie.data.sync.InterventionFeedbackEntity
import com.rehealth.genie.network.dto.RiskResultDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RiskHistoryRepositoryTest {
    @Test
    fun `stores only confirmed remote result and maps completed feedback`() = runTest {
        val riskDao = FakeRiskHistoryDao()
        val feedbackDao = FakeFeedbackDao()
        val repository = RiskHistoryRepository(
            riskHistoryDao = riskDao,
            feedbackDao = feedbackDao,
            userIdProvider = { "user-1" },
            nowProvider = { 1_700_000_000_000L },
        )

        repository.recordConfirmedRemoteRisk(RiskResultDto(risk_score = 0.8, is_mock = true))
        assertEquals(emptyList(), riskDao.rows)

        repository.recordConfirmedRemoteRisk(
            RiskResultDto(risk_score = 0.42, risk_level = "moderate", is_mock = false),
        )
        feedbackDao.rows += InterventionFeedbackEntity(
            id = "feedback-1",
            interventionId = "plan-1",
            status = "completed",
            checkedAt = 1_700_000_000_000L,
            createdAt = 1_700_000_000_000L,
        )

        val history = repository.attributionHistory()
        assertEquals(1, history.size)
        assertEquals(0.42, history.single().riskScore)
        assertEquals(true, history.single().isInterventionDay)
    }
}

private class FakeRiskHistoryDao : RiskHistoryDao {
    val rows = mutableListOf<RiskHistoryEntity>()

    override suspend fun upsert(entity: RiskHistoryEntity) {
        rows.removeAll { it.userId == entity.userId && it.evaluatedOn == entity.evaluatedOn }
        rows += entity
    }

    override suspend fun latestForUser(userId: String, limit: Int): List<RiskHistoryEntity> =
        rows.filter { it.userId == userId }.sortedByDescending { it.evaluatedOn }.take(limit)
}

private class FakeFeedbackDao : InterventionFeedbackDao {
    val rows = mutableListOf<InterventionFeedbackEntity>()

    override suspend fun insert(feedback: InterventionFeedbackEntity) { rows += feedback }
    override suspend fun update(feedback: InterventionFeedbackEntity) = Unit
    override suspend fun pendingUploads(now: Long): List<InterventionFeedbackEntity> = emptyList()
    override suspend fun getLatestForIntervention(interventionId: String): InterventionFeedbackEntity? = null
    override fun observePendingFeedback(): Flow<List<InterventionFeedbackEntity>> = flowOf(emptyList())
    override suspend fun completedFeedbackSince(since: Long): List<InterventionFeedbackEntity> =
        rows.filter { it.checkedAt >= since }
    override suspend fun pruneDone(cutoffTimestamp: Long) = Unit
    override suspend fun countPending(): Int = 0
}
