package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceInterventionWorkbenchEvidenceTest {
    @Test
    void concludesStageImprovementWhenAllEvidenceGatesPass() {
        InsuranceInterventionWorkbenchService.ImprovementEvidenceDecision result = decide(
                true, false, 30, 14, 14, -0.035, -0.058);

        assertTrue(result.conclusive());
        assertEquals("improved", result.conclusion());
        assertEquals("individual_att", result.effectMetric());
        assertEquals(-0.035, result.effectValue());
    }

    @Test
    void concludesNotImprovedInsteadOfCallingSufficientEvidenceInsufficient() {
        InsuranceInterventionWorkbenchService.ImprovementEvidenceDecision result = decide(
                true, false, 30, 14, 14, 0.01, 0.02);

        assertTrue(result.conclusive());
        assertEquals("not_improved", result.conclusion());
    }

    @Test
    void requiresRealDataBaselineInterventionWindowAndEffectSignal() {
        assertInsufficient(decide(true, true, 30, 14, 14, -0.03, null));
        assertInsufficient(decide(true, false, 13, 14, 14, -0.03, null));
        assertInsufficient(decide(true, false, 30, 14, 6, -0.03, null));
        assertInsufficient(decide(true, false, 30, 14, 14, null, null));
        assertInsufficient(decide(false, false, 30, 14, 14, -0.03, null));
    }

    @Test
    void fallsBackToTrendDeltaWhenIndividualEffectIsUnavailable() {
        InsuranceInterventionWorkbenchService.ImprovementEvidenceDecision result = decide(
                true, false, 28, 14, 8, null, -0.04);

        assertTrue(result.conclusive());
        assertEquals("improved", result.conclusion());
        assertEquals("trend_delta", result.effectMetric());
        assertEquals(-0.04, result.effectValue());
    }

    private InsuranceInterventionWorkbenchService.ImprovementEvidenceDecision decide(
            Boolean sufficient,
            Boolean mock,
            Integer historyDays,
            Integer minHistoryDays,
            Integer interventionDays,
            Double individualAtt,
            Double trendDelta
    ) {
        return InsuranceInterventionWorkbenchService.evaluateImprovementEvidence(
                new InsuranceInterventionWorkbenchService.AttributionSnapshot(
                        sufficient, mock, historyDays, minHistoryDays, interventionDays,
                        0.82, individualAtt, trendDelta, "ready", "测试", new Timestamp(0)));
    }

    private void assertInsufficient(
            InsuranceInterventionWorkbenchService.ImprovementEvidenceDecision result
    ) {
        assertFalse(result.conclusive());
        assertEquals("insufficient", result.conclusion());
    }
}
