package com.rehealth.genie.ui

import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.phm.IndividualAttributionResult
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AttributionUiStateTest {
    private val today = LocalDate.of(2026, 7, 22)

    @Test
    fun `maps confirmed risk and signed percentage point improvement for each period`() {
        val history = listOf(
            point("2026-04-25", 0.40),
            point("2026-06-25", 0.35),
            point("2026-07-17", 0.25),
            point("2026-07-22", 0.0219),
        )

        val sevenDays = map(history = history, period = AttributionPeriod.DAYS_7)
        val thirtyDays = map(history = history, period = AttributionPeriod.DAYS_30)
        val ninetyDays = map(history = history, period = AttributionPeriod.DAYS_90)

        assertEquals("2.19%", sevenDays.currentRiskText)
        assertEquals("+22.8", sevenDays.improvementText)
        assertEquals("+32.8", thirtyDays.improvementText)
        assertEquals("+37.8", ninetyDays.improvementText)
        assertEquals(2, sevenDays.selectedHistory.size)
        assertEquals(3, thirtyDays.selectedHistory.size)
        assertEquals(4, ninetyDays.selectedHistory.size)
    }

    @Test
    fun `reports negative improvement without hiding the sign`() {
        val state = map(
            history = listOf(point("2026-07-20", 0.20), point("2026-07-22", 0.30)),
        )

        assertEquals("-10.0", state.improvementText)
    }

    @Test
    fun `requires two selected confirmed risks for improvement`() {
        val state = map(history = listOf(point("2026-07-22", 0.30)))

        assertEquals("--", state.improvementText)
        assertTrue(state.improvementMessage.contains("2"))
    }

    @Test
    fun `ignores malformed and out of window risk dates`() {
        val state = map(
            history = listOf(
                point("not-a-date", 0.90),
                point("2026-01-01", 0.80),
                point("2026-07-22", 0.25),
            ),
        )

        assertEquals(listOf("2026-07-22"), state.selectedHistory.map { it.date })
        assertEquals("--", state.improvementText)
    }

    @Test
    fun `truncates unequal PIAS arrays and drops malformed confidence data`() {
        val result = IndividualAttributionResult(
            status = "ready",
            forecastNoAction = listOf(0.4, 0.39, 0.38, Double.NaN),
            forecastWithPlan = listOf(0.4, 0.35),
            forecastCiLower = listOf(0.3),
            forecastCiUpper = listOf(0.5, 0.49, 0.48),
        )

        val pias = assertIs<AttributionPiasUiState.Ready>(map(pias = result).pias)

        assertEquals(listOf(0.4, 0.39), pias.forecast.noAction)
        assertEquals(listOf(0.4, 0.35), pias.forecast.withPlan)
        assertEquals(emptyList(), pias.forecast.ciLower)
        assertEquals(emptyList(), pias.forecast.ciUpper)
        assertTrue(pias.forecast.chartAvailable)
    }

    @Test
    fun `keeps ready PIAS state compact when forecast arrays are empty`() {
        val result = IndividualAttributionResult(status = "ready")

        val pias = assertIs<AttributionPiasUiState.Ready>(map(pias = result).pias)

        assertFalse(pias.forecast.chartAvailable)
        assertIs<AttributionAttUiState.Unavailable>(pias.att)
    }

    @Test
    fun `distinguishes accumulating ready and unavailable ATT`() {
        val accumulating = assertIs<AttributionPiasUiState.Accumulating>(
            map(
                pias = IndividualAttributionResult(
                    status = "accumulating",
                    historyDays = 5,
                    minHistoryDays = 14,
                ),
            ).pias,
        )
        val ready = assertIs<AttributionPiasUiState.Ready>(
            map(
                pias = IndividualAttributionResult(
                    status = "ready",
                    historyDays = 30,
                    minHistoryDays = 14,
                    attAvailable = false,
                    interventionDataSufficient = false,
                    individualAtt = -0.04,
                ),
            ).pias,
        )

        assertEquals(5, accumulating.historyDays)
        assertEquals(14, accumulating.minHistoryDays)
        assertIs<AttributionAttUiState.Unavailable>(ready.att)
    }

    @Test
    fun `normalizes aliases and always emits exact canonical factor order`() {
        val state = map(
            evaluation = AttributionRiskEvaluation(
                riskScore = 0.0219,
                riskLevel = "low",
                contributions = mapOf(
                    "family_history" to 0.03,
                    "fastingGlucose" to -0.02,
                    "age" to 0.01,
                    "unknownField" to 0.99,
                ),
                confirmed = true,
            ),
        )

        assertEquals(AttributionUiMapper.CANONICAL_FACTOR_KEYS, state.factors.map { it.key })
        assertEquals(4, state.factorGroups.size)
        assertEquals(-0.02, state.factors.single { it.key == "fasting_glucose" }.contribution)
        assertNull(state.factors.single { it.key == "bmi" }.contribution)
        assertTrue(state.factors.single { it.key == "bmi" }.contributionMissing)
    }

    @Test
    fun `maps real Room activity and labels debug replay honestly`() {
        val real = map(
            activity = AttributionActivityInput(
                startedAt = 1_753_200_000_000L,
                activityType = "daily",
                steps = 4321,
                durationMinutes = 38,
                caloriesKcal = 156.0,
                distanceMeters = 3120.0,
                source = "MRD_SDK",
                replay = false,
            ),
        ).activity
        val hiddenReplay = map(
            activity = replayActivity(),
            allowDebugReplay = false,
        ).activity
        val visibleReplay = map(
            activity = replayActivity(),
            allowDebugReplay = true,
        ).activity

        assertEquals(4321, real?.steps)
        assertEquals("MR11 戒指 · Room", real?.provenanceLabel)
        assertNull(hiddenReplay)
        assertTrue(visibleReplay?.provenanceLabel?.contains("调试回放") == true)
    }

    @Test
    fun `excludes heuristic and idless interventions`() {
        val fallback = map(
            interventions = listOf(intervention(id = "walking_zone2")),
            interventionSourceMode = "local_heuristic",
        )
        val idless = map(interventions = listOf(intervention(id = null)))
        val server = map(interventions = listOf(intervention(id = "server-plan-42")))

        assertTrue(fallback.interventions.isEmpty())
        assertTrue(idless.interventions.isEmpty())
        assertEquals("server-plan-42", server.interventions.single().id)
        assertTrue(server.interventions.single().feedbackEnabled)
    }

    @Test
    fun `ignores stale completion and exposes retry transitions`() {
        val firstData = AttributionRemoteData(listOf(point("2026-07-21", 0.4)), null)
        val secondData = AttributionRemoteData(listOf(point("2026-07-22", 0.3)), null)

        val loadingOne = AttributionRefreshState().reduce(AttributionRefreshEvent.Started(1))
        val loadingTwo = loadingOne.reduce(AttributionRefreshEvent.Started(2))
        val staleIgnored = loadingTwo.reduce(AttributionRefreshEvent.Succeeded(1, firstData))
        val failed = staleIgnored.reduce(AttributionRefreshEvent.Failed(2, "offline"))
        val retrying = failed.reduce(AttributionRefreshEvent.Started(3))
        val succeeded = retrying.reduce(AttributionRefreshEvent.Succeeded(3, secondData))

        assertEquals(AttributionRefreshPhase.LOADING, staleIgnored.phase)
        assertNull(staleIgnored.data)
        assertEquals(AttributionRefreshPhase.ERROR, failed.phase)
        assertTrue(failed.canRetry)
        assertEquals(AttributionRefreshPhase.LOADING, retrying.phase)
        assertNull(retrying.errorMessage)
        assertEquals(AttributionRefreshPhase.READY, succeeded.phase)
        assertEquals(secondData, succeeded.data)
    }

    @Test
    fun `retains loaded history when PIAS refresh fails`() {
        val historyOnly = AttributionRemoteData(listOf(point("2026-07-22", 0.3)), null)
        val failed = AttributionRefreshState()
            .reduce(AttributionRefreshEvent.Started(7))
            .reduce(AttributionRefreshEvent.Failed(7, "offline", historyOnly))

        assertEquals(AttributionRefreshPhase.ERROR, failed.phase)
        assertEquals(historyOnly, failed.data)
        assertEquals("offline", failed.errorMessage)
    }

    private fun map(
        history: List<AttributionHistoryPoint> = emptyList(),
        period: AttributionPeriod = AttributionPeriod.DAYS_7,
        pias: IndividualAttributionResult? = null,
        evaluation: AttributionRiskEvaluation? = null,
        activity: AttributionActivityInput? = null,
        allowDebugReplay: Boolean = false,
        interventions: List<AttributionInterventionInput> = emptyList(),
        interventionSourceMode: String? = null,
    ): AttributionUiState = AttributionUiMapper.map(
        AttributionUiInput(
            period = period,
            today = today,
            evaluation = evaluation,
            remote = AttributionRemoteData(history = history, pias = pias),
            refreshPhase = AttributionRefreshPhase.READY,
            activity = activity,
            allowDebugReplay = allowDebugReplay,
            interventions = interventions,
            interventionSourceMode = interventionSourceMode,
        ),
    )

    private fun point(date: String, score: Double) = AttributionHistoryPoint(
        date = date,
        riskScore = score,
        isInterventionDay = false,
    )

    private fun replayActivity() = AttributionActivityInput(
        startedAt = 1_753_200_000_000L,
        activityType = "walking",
        steps = 1000,
        durationMinutes = 20,
        caloriesKcal = 50.0,
        distanceMeters = 800.0,
        source = "MockRingRepository",
        replay = true,
    )

    private fun intervention(id: String?) = AttributionInterventionInput(
        id = id,
        title = "步行计划",
        action = "晚餐后步行",
        duration = "14 天",
        reason = "服务端建议",
        status = "active",
    )
}
