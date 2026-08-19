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
            ownerUserId = "user-1",
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

    @Test
    fun `stores confirmed historical evaluations on their requested days`() = runTest {
        val riskDao = FakeRiskHistoryDao()
        val repository = RiskHistoryRepository(
            riskHistoryDao = riskDao,
            feedbackDao = FakeFeedbackDao(),
            userIdProvider = { "user-1" },
        )

        repository.recordConfirmedRemoteRisk(
            RiskResultDto(risk_score = 0.31, is_mock = false),
            evaluatedAt = 1_699_827_200_000L,
        )
        repository.recordConfirmedRemoteRisk(
            RiskResultDto(risk_score = 0.29, is_mock = false),
            evaluatedAt = 1_699_913_600_000L,
        )

        val history = repository.attributionHistory()
        assertEquals(2, history.size)
        assertEquals(listOf(0.31, 0.29), history.map { it.riskScore })
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

    override suspend fun sinceForUser(userId: String, since: Long): List<RiskHistoryEntity> =
        rows.filter { it.userId == userId && it.evaluatedAt >= since }.sortedBy { it.evaluatedOn }
}

private class FakeFeedbackDao : InterventionFeedbackDao {
    val rows = mutableListOf<InterventionFeedbackEntity>()

    override suspend fun insert(feedback: InterventionFeedbackEntity) { rows += feedback }
    override suspend fun update(feedback: InterventionFeedbackEntity) = Unit
    override suspend fun pendingUploads(ownerUserId: String, now: Long): List<InterventionFeedbackEntity> = emptyList()
    override fun observeFeedback(ownerUserId: String, feedbackId: String): Flow<InterventionFeedbackEntity?> =
        flowOf(rows.find { it.ownerUserId == ownerUserId && it.id == feedbackId })
    override suspend fun getLatestForIntervention(ownerUserId: String, interventionId: String): InterventionFeedbackEntity? = null
    override fun observePendingFeedback(ownerUserId: String): Flow<List<InterventionFeedbackEntity>> = flowOf(emptyList())
    override suspend fun supersedeDeadLetters(ownerUserId: String, occurrenceId: String) = Unit
    override suspend fun completedFeedbackSince(ownerUserId: String, since: Long): List<InterventionFeedbackEntity> =
        rows.filter { it.ownerUserId == ownerUserId && it.checkedAt >= since }
    override suspend fun pruneDone(cutoffTimestamp: Long) = Unit
    override suspend fun countPending(ownerUserId: String): Int = 0
}
