package com.rehealth.genie.ui

import com.rehealth.genie.phm.AttributionHistoryPoint
import com.rehealth.genie.phm.IndividualAttributionResult
import com.rehealth.genie.rhi.RhiCalculationSource
import com.rehealth.genie.rhi.RhiDailyScore
import com.rehealth.genie.rhi.RhiPeriodAggregation
import com.rehealth.genie.rhi.RhiPeriodSummary
import com.rehealth.genie.rdi.RdiDailyScore
import com.rehealth.genie.rdi.RdiPeriodAggregation
import com.rehealth.genie.rdi.RdiPeriodSummary
import com.rehealth.genie.rdi.RdiScenarioForecast
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
    fun `labels local risk impact baseline states conservatively`() {
        assertEquals("已建立个人基线", rdiImpactStatusLabel("confirmed"))
        assertEquals("初步结果", rdiImpactStatusLabel("provisional"))
        assertEquals("基线建立中", rdiImpactStatusLabel("accumulating"))
        assertEquals("计算中", rdiImpactStatusLabel(null))
        assertEquals("状态待确认", rdiImpactStatusLabel("unexpected"))
    }

    @Test
    fun `maps confirmed RDI sixteen risk as a score out of one hundred`() {
        val history = listOf(
            rdiPoint("2026-04-25", 52.0),
            rdiPoint("2026-06-25", 50.5),
            rdiPoint("2026-07-17", 48.0),
            rdiPoint("2026-07-22", 46.9),
        )

        val sevenDays = map(
            period = AttributionPeriod.DAYS_7,
            rdiSummary = rdiSummary(7, 46.9, history.takeLast(2)),
        )
        val thirtyDays = map(
            period = AttributionPeriod.DAYS_30,
            rdiSummary = rdiSummary(30, 48.0, history.takeLast(3)),
        )
        val ninetyDays = map(
            period = AttributionPeriod.DAYS_90,
            rdiSummary = rdiSummary(90, 49.3, history),
        )

        assertEquals("46.9/100", sevenDays.currentRiskText)
        assertEquals("48.0/100", thirtyDays.currentRiskText)
        assertEquals("49.3/100", ninetyDays.currentRiskText)
        assertEquals(2, sevenDays.selectedHistory.size)
        assertEquals(3, thirtyDays.selectedHistory.size)
        assertEquals(4, ninetyDays.selectedHistory.size)
        assertFalse(sevenDays.rdiScenario.forecastAvailable)
        assertFalse(sevenDays.rdiScenario.intervalAvailable)
    }

    @Test
    fun `period RDI sixteen history is not replaced by evaluation or PIAS values`() {
        val state = map(
            rdiSummary = rdiSummary(
                7,
                46.9,
                listOf(rdiPoint("2026-07-20", 47.3), rdiPoint("2026-07-22", 46.9)),
            ),
            evaluation = AttributionRiskEvaluation(
                riskScore = 0.99,
                riskLevel = "very_high",
                contributions = emptyMap(),
                confirmed = true,
            ),
            pias = IndividualAttributionResult(
                status = "ready",
                d30NoAction = 0.88,
                d30WithPlan = 0.77,
                riskReduction = 0.11,
            ),
        )

        assertEquals("46.9/100", state.currentRiskText)
        assertFalse(state.rdiScenario.forecastAvailable)
    }

    @Test
    fun `maps native RDI scenario scores and interval without PIAS conversion`() {
        val scenario = RdiScenarioForecast(
            noAction = listOf(46.9, 47.1, 47.3),
            withPlan = listOf(46.9, 46.2, 44.8),
            ciLower = listOf(46.9, 45.7, 43.9),
            ciUpper = listOf(46.9, 46.8, 45.7),
            d30NoAction = 47.3,
            d30WithPlan = 44.8,
            expectedReduction = 2.5,
        )
        val state = map(
            rdiSummary = rdiSummary(
                periodDays = 7,
                score = 46.9,
                history = listOf(rdiPoint("2026-07-21", 47.2), rdiPoint("2026-07-22", 46.9)),
                scenario = scenario,
            ),
            pias = IndividualAttributionResult(
                status = "ready",
                d30NoAction = 0.88,
                d30WithPlan = 0.77,
                riskReduction = 0.11,
            ),
        )

        assertTrue(state.rdiScenario.forecastAvailable)
        assertTrue(state.rdiScenario.intervalAvailable)
        assertEquals(listOf(46.9, 47.1, 47.3), state.rdiScenario.noAction)
        assertEquals(listOf(46.9, 46.2, 44.8), state.rdiScenario.withPlan)
        assertEquals(47.3, state.rdiScenario.noActionScore)
        assertEquals(44.8, state.rdiScenario.withPlanScore)
        assertEquals(2.5, state.rdiScenario.expectedReduction)
    }

    @Test
    fun `maps RHI improvement against the ninety day baseline`() {
        val improvement = AttributionUiMapper.mapRhiImprovement(
            summary = rhiSummary(
                RhiDailyScore(LocalDate.of(2026, 4, 24), 55.0, 0.8),
                RhiDailyScore(LocalDate.of(2026, 7, 18), 69.0, 0.8),
                RhiDailyScore(today, 71.0, 0.8),
                periodDays = 7,
            ),
            period = AttributionPeriod.DAYS_7,
            today = today,
        )

        assertEquals(16.0, improvement.improvementPoints)
        assertEquals("+16.0 分", improvement.improvementText)
        assertEquals("RHI-100 · 较个人基线改善 · 最近 7 天", improvement.comparisonText)
        assertEquals(2, improvement.selectedHistory.size)
    }

    @Test
    fun `RHI improvement changes with selected seven thirty and ninety day windows`() {
        val history = arrayOf(
            RhiDailyScore(LocalDate.of(2026, 4, 24), 55.0, 0.8),
            RhiDailyScore(LocalDate.of(2026, 6, 25), 62.0, 0.8),
            RhiDailyScore(LocalDate.of(2026, 7, 18), 69.0, 0.8),
            RhiDailyScore(today, 71.0, 0.8),
        )

        val seven = AttributionUiMapper.mapRhiImprovement(
            rhiSummary(*history, periodDays = 7),
            AttributionPeriod.DAYS_7,
            today,
        )
        val thirty = AttributionUiMapper.mapRhiImprovement(
            rhiSummary(*history, periodDays = 30).copy(score = 70.0),
            AttributionPeriod.DAYS_30,
            today,
        )
        val ninety = AttributionUiMapper.mapRhiImprovement(
            rhiSummary(*history, periodDays = 90).copy(score = 67.3),
            AttributionPeriod.DAYS_90,
            today,
        )

        assertEquals(16.0, seven.improvementPoints)
        assertEquals(15.0, thirty.improvementPoints)
        assertEquals(12.3, ninety.improvementPoints)
    }

    @Test
    fun `reports negative RHI improvement without hiding the sign`() {
        val improvement = AttributionUiMapper.mapRhiImprovement(
            summary = rhiSummary(
                RhiDailyScore(LocalDate.of(2026, 7, 20), 71.0, 0.8),
                RhiDailyScore(today, 65.0, 0.8),
                baseline = RhiDailyScore(LocalDate.of(2026, 4, 24), 71.0, 0.8),
            ),
            period = AttributionPeriod.DAYS_7,
            today = today,
        )

        assertEquals("-6.0 分", improvement.improvementText)
        assertEquals("RHI-100 · 较个人基线改善 · 最近 7 天", improvement.comparisonText)
    }

    @Test
    fun `requires two valid RHI days for improvement`() {
        val improvement = AttributionUiMapper.mapRhiImprovement(
            summary = rhiSummary(RhiDailyScore(today, 71.0, 0.8)),
            period = AttributionPeriod.DAYS_7,
            today = today,
        )

        assertEquals("-- 分", improvement.improvementText)
        assertTrue(improvement.comparisonText.contains("至少 2 个有效日"))
    }

    @Test
    fun `RDI history only displays points inside the selected period`() {
        val state = map(
            rdiSummary = rdiSummary(
                7,
                46.9,
                listOf(rdiPoint("2026-01-01", 80.0), rdiPoint("2026-07-22", 46.9)),
            ),
        )

        assertEquals(listOf("2026-07-22"), state.selectedHistory.map { it.date })
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
    fun `keeps confirmed history separate from returned PIAS scenarios`() {
        val state = map(
            rdiSummary = rdiSummary(
                7,
                46.2,
                listOf(rdiPoint("2026-07-20", 46.9), rdiPoint("2026-07-22", 46.2)),
            ),
            pias = IndividualAttributionResult(
                status = "ready",
                forecastNoAction = listOf(0.28, 0.29, 0.30),
                forecastWithPlan = listOf(0.28, 0.26, 0.24),
                forecastCiLower = listOf(0.24, 0.23, 0.21),
                forecastCiUpper = listOf(0.32, 0.33, 0.34),
            ),
        )
        val pias = assertIs<AttributionPiasUiState.Ready>(state.pias)

        assertEquals(listOf(0.469, 0.462), state.selectedHistory.map { it.riskScore })
        assertEquals(listOf(0.28, 0.29, 0.30), pias.forecast.noAction)
        assertEquals(listOf(0.28, 0.26, 0.24), pias.forecast.withPlan)
        assertEquals(listOf(0.24, 0.23, 0.21), pias.forecast.ciLower)
        assertEquals(listOf(0.32, 0.33, 0.34), pias.forecast.ciUpper)
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
        assertEquals(9, accumulating.remainingDays)
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
    fun `shows independently confirmed factor rules when cvd scorer is mock`() {
        val state = map(
            evaluation = AttributionRiskEvaluation(
                riskScore = 0.30,
                riskLevel = "moderate",
                contributions = mapOf("ldl" to 0.96),
                confirmed = false,
                factorConfirmed = true,
                contributionRuleVersion = "factor16-rule-v1.0.0",
                measuredComponents = mapOf("ldl" to 0.96),
                controlSupportComponents = mapOf("ldl" to 0.0),
            ),
        )

        assertEquals("--/100", state.currentRiskText)
        assertEquals(0.96, state.factors.single { it.key == "ldl" }.contribution)
        assertEquals(0.0, state.factors.single { it.key == "ldl" }.controlSupportComponent)
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
        rdiSummary: RdiPeriodSummary? = null,
    ): AttributionUiState = AttributionUiMapper.map(
        AttributionUiInput(
            period = period,
            today = today,
            evaluation = evaluation,
            remote = AttributionRemoteData(history = history, pias = pias),
            rdiSummary = rdiSummary,
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

    private fun rhiSummary(
        vararg history: RhiDailyScore,
        periodDays: Int = 7,
        baseline: RhiDailyScore? = history.firstOrNull(),
    ) = RhiPeriodSummary(
        periodDays = periodDays,
        score = history.lastOrNull()?.score,
        confidence = history.lastOrNull()?.confidence,
        validDays = history.size,
        requiredValidDays = 14,
        aggregation = RhiPeriodAggregation.ROBUST_MEDIAN,
        history = history.toList(),
        calculationSource = RhiCalculationSource.LOCAL,
        baseline90d = baseline,
    )

    private fun rdiPoint(date: String, score: Double) = RdiDailyScore(
        date = LocalDate.parse(date),
        score = score,
        confidence = 0.8,
    )

    private fun rdiSummary(
        periodDays: Int,
        score: Double,
        history: List<RdiDailyScore>,
        scenario: RdiScenarioForecast? = null,
    ) = RdiPeriodSummary(
        periodDays = periodDays,
        score = score,
        confidence = 0.8,
        validDays = history.size,
        requiredValidDays = if (periodDays == 7) 1 else if (periodDays == 30) 7 else 14,
        aggregation = if (periodDays == 7) {
            RdiPeriodAggregation.CURRENT_7_DAY
        } else {
            RdiPeriodAggregation.ROBUST_MEDIAN
        },
        history = history,
        scenario = scenario,
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
