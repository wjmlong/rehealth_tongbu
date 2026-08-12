package org.jeecg.modules.rehealth.insurance;

import org.jeecg.modules.rehealth.insurance.mapper.InsuranceClaimMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceConsentMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceCoverageMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceInterventionMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MybatisPlusInsuranceBusinessRepositoryTest {
    @Test
    void tenantSummaryUsesMybatisPlusMappersAndMapsAggregates() {
        InsurancePolicyMapper policies = mock(InsurancePolicyMapper.class);
        InsuranceCoverageMapper coverages = mock(InsuranceCoverageMapper.class);
        InsuranceClaimMapper claims = mock(InsuranceClaimMapper.class);
        InsuranceInterventionMapper interventions = mock(InsuranceInterventionMapper.class);
        InsuranceConsentMapper consents = mock(InsuranceConsentMapper.class);

        when(policies.selectCount(any())).thenReturn(3L);
        when(coverages.selectCount(any())).thenReturn(4L);
        when(interventions.selectCount(any())).thenReturn(1L);
        when(claims.selectMaps(any())).thenReturn(List.of(Map.of(
                "claim_count", 2L,
                "billed_amount", new BigDecimal("100.00"),
                "paid_amount", new BigDecimal("25.00"),
                "latest_updated_at", Timestamp.valueOf("2026-08-11 14:00:00")
        )));
        when(policies.selectMaps(any())).thenReturn(List.of(Map.of(
                "latest_updated_at", Timestamp.valueOf("2026-08-11 10:00:00")
        )));
        when(coverages.selectMaps(any())).thenReturn(List.of(Map.of(
                "latest_updated_at", Timestamp.valueOf("2026-08-11 11:00:00")
        )));
        when(interventions.selectMaps(any())).thenReturn(List.of(Map.of(
                "latest_updated_at", Timestamp.valueOf("2026-08-11 12:00:00")
        )));

        MybatisPlusInsuranceBusinessRepository repository = new MybatisPlusInsuranceBusinessRepository(
                policies, coverages, claims, interventions, consents
        );

        InsuranceRiskRepository.BusinessSnapshot summary = repository.tenant(1000);

        assertEquals(3, summary.activePolicies());
        assertEquals(4, summary.activeCoverages());
        assertEquals(2, summary.claimCount());
        assertEquals(new BigDecimal("25.00"), summary.paidAmount());
        assertEquals(Timestamp.valueOf("2026-08-11 14:00:00"), summary.latestUpdatedAt());
    }
}
