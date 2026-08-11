package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceRiskServiceTest {
    private static final String SUBJECT_ONE = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SUBJECT_TWO = "2222222222222222222222222222222222222222222222222222222222222222";
    private static final String MISSING_SUBJECT = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void scopeIsUnavailableUnlessTheExplicitDevFlagIsEnabled() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, false, "development");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.dashboard(1000)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        verify(repository, never()).dashboard(1000);
    }

    @Test
    void dashboardExposesOnlyVerifiedRiskAndMarksUnsupportedMetricsNotConnected() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        Timestamp evaluatedAt = Timestamp.from(Instant.parse("2026-08-11T06:00:00Z"));
        when(repository.dashboard(1000)).thenReturn(new InsuranceRiskRepository.DashboardSnapshot(
                9, 5, 2, 2, 1, 3, 1, evaluatedAt
        ));
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceRiskResponse.Dashboard dashboard = service.dashboard(1000);

        assertEquals(InsuranceRiskService.DEV_SCOPE_MODE, dashboard.scopeMode());
        assertEquals(9, dashboard.totalInsured());
        assertEquals(5, dashboard.assessedInsured());
        assertEquals(2, dashboard.syntheticInsured());
        assertEquals(2, dashboard.unassessedInsured());
        assertEquals(1, dashboard.riskDistribution().high());
        assertEquals("2026-08-11T06:00:00Z", dashboard.latestEvaluatedAt());
        assertEquals("not_connected", dashboard.claims().status());
        assertNull(dashboard.claims().value());
        assertEquals("not_connected", dashboard.savings().status());
        assertEquals("not_connected", dashboard.psm().status());
        assertEquals("not_connected", dashboard.rwe().status());
    }

    @Test
    void developmentScopeFailsClosedOutsideDevelopmentEvenWhenFlagIsTrue() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "staging");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.dashboard(1000)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
        verify(repository, never()).dashboard(1000);
    }

    @Test
    void latestMockRiskIsSyntheticAndNeverLeaksItsScoreLevelOrFactors() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        Timestamp evaluatedAt = Timestamp.from(Instant.parse("2026-08-11T06:00:00Z"));
        InsuranceRiskRepository.SubjectSnapshot synthetic = snapshot(
                SUBJECT_ONE,
                "张三丰",
                true,
                0.91,
                "high",
                evaluatedAt,
                "{\"smoking\":0.8}",
                true
        );
        when(repository.subjects(1000, 1, 20, null, null))
                .thenReturn(new InsuranceRiskRepository.SubjectPage(1, List.of(synthetic)));
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceRiskResponse.Subject subject = service.insureds(1000, 1, 20, null, null).records().get(0);

        assertEquals("张*丰", subject.displayName());
        assertEquals("synthetic", subject.risk().status());
        assertNull(subject.risk().score());
        assertNull(subject.risk().level());
        assertNull(subject.risk().positiveFactors());
        assertEquals("not_available", subject.intervention().status());
        assertNull(subject.intervention().summary());
    }

    @Test
    void verifiedRiskReturnsOnlyPositiveFactorsInDescendingOrder() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        Timestamp evaluatedAt = Timestamp.from(Instant.parse("2026-08-11T06:00:00Z"));
        InsuranceRiskRepository.SubjectSnapshot assessed = snapshot(
                SUBJECT_TWO,
                null,
                false,
                0.61,
                "moderate",
                evaluatedAt,
                "{\"age\":0.2,\"smoking\":-0.5,\"bmi\":0.4}",
                false
        );
        when(repository.subject(1000, SUBJECT_TWO)).thenReturn(Optional.of(assessed));
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceRiskResponse.Subject subject = service.insured(1000, SUBJECT_TWO).subject();

        assertTrue(subject.displayName().startsWith("受保人-"));
        assertEquals("assessed", subject.risk().status());
        assertEquals(0.61, subject.risk().score());
        assertEquals("medium", subject.risk().level());
        assertEquals(List.of("bmi", "age"), subject.risk().positiveFactors().stream()
                .map(InsuranceRiskResponse.PositiveFactor::key)
                .toList());
        assertEquals("available", subject.intervention().status());
        assertNull(subject.intervention().summary());
    }

    @Test
    void missingTenantSubjectReturnsTrueNotFoundSemantics() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        when(repository.subject(1000, MISSING_SUBJECT)).thenReturn(Optional.empty());
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.insured(1000, MISSING_SUBJECT)
        );

        assertEquals(HttpStatus.NOT_FOUND, error.status());
    }

    @Test
    void rawInternalUserIdCannotBeUsedAsAnInsurerSubjectReference() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.insured(1000, "internal-user-id")
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        verify(repository, never()).subject(1000, "internal-user-id");
    }

    @Test
    void databaseFailureBecomesServiceUnavailableWithoutReturningEmptyData() {
        InsuranceRiskRepository repository = mock(InsuranceRiskRepository.class);
        when(repository.dashboard(1000)).thenThrow(new DataAccessResourceFailureException("down"));
        InsuranceRiskService service = new InsuranceRiskService(repository, objectMapper, true, "development");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.dashboard(1000)
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
    }

    private InsuranceRiskRepository.SubjectSnapshot snapshot(
            String subjectId,
            String name,
            Boolean riskMock,
            Double riskScore,
            String riskLevel,
            Timestamp evaluatedAt,
            String contributionJson,
            Boolean interventionMock
    ) {
        return new InsuranceRiskRepository.SubjectSnapshot(
                subjectId,
                name,
                42,
                "female",
                new BigDecimal("23.40"),
                riskMock,
                riskScore,
                riskLevel,
                "model-v1",
                evaluatedAt,
                contributionJson,
                interventionMock,
                "walk daily",
                evaluatedAt
        );
    }
}
