package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePolicyEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceClaimMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceImportBatchMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSubjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Policy import dispatches only to subjects inside the current staff's
 * responsibility range; service accounts without an insurance responsibility
 * role keep the unrestricted server-to-server path.
 */
class InsuranceImportServicePolicyScopeTest {
    private static final String SUBJECT_REF = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final InsuranceSubjectMapper subjectMapper = mock(InsuranceSubjectMapper.class);
    private final InsurancePolicyMapper policyMapper = mock(InsurancePolicyMapper.class);
    private final InsuranceClaimMapper claimMapper = mock(InsuranceClaimMapper.class);
    private final InsuranceImportBatchMapper importBatchMapper = mock(InsuranceImportBatchMapper.class);
    private final InsuranceTenantAccessGuard tenantAccessGuard = mock(InsuranceTenantAccessGuard.class);
    private final InsuranceDispatchAccess dispatchAccess = mock(InsuranceDispatchAccess.class);
    private final InsuranceImportService service = new InsuranceImportService(
            subjectMapper, policyMapper, claimMapper, importBatchMapper,
            new ObjectMapper().registerModule(new JavaTimeModule()), tenantAccessGuard, dispatchAccess);

    private static final InsuranceAssignmentScope TEAM_SCOPE =
            new InsuranceAssignmentScope("mgr-1", InsuranceAssignmentScope.MODE_TEAM);

    @BeforeEach
    void stubTenantSubjectLookup() {
        when(subjectMapper.selectCount(any())).thenReturn(1L);
    }

    private InsuranceImportRequest.PolicyBatch batch(String policyNo) {
        return new InsuranceImportRequest.PolicyBatch(
                "rehealth_website",
                "idem-1",
                List.of(new InsuranceImportRequest.PolicyRow(
                        policyNo, "PROD-1", "安心医疗", "医疗险", null, SUBJECT_REF,
                        null, null, null, null, LocalDate.of(2026, 8, 1), null,
                        "active", null, null, null))
        );
    }

    @Test
    void departmentManagerImportsOnlyInsideTheTeamScope() {
        LoginUser manager = new LoginUser().setId("mgr-1");
        when(tenantAccessGuard.assignmentScopeOrNull(manager, 1000)).thenReturn(TEAM_SCOPE);

        InsuranceImportResponse.BatchResult result = service.importPolicies(1000, manager, batch("POL-T-001"));

        verify(dispatchAccess).requireDispatchable(1000, TEAM_SCOPE, SUBJECT_REF);
        assertEquals("completed", result.status());
        assertEquals(1, result.records().size());
        assertEquals("created", result.records().get(0).status());
    }

    @Test
    void importAbortsWhenTheSubjectIsOutsideTheResponsibilityRange() {
        LoginUser manager = new LoginUser().setId("mgr-1");
        when(tenantAccessGuard.assignmentScopeOrNull(manager, 1000)).thenReturn(TEAM_SCOPE);
        doThrow(InsuranceApiException.forbidden("该被保人不在您的负责范围内，请先认领该用户或选择您负责的用户"))
                .when(dispatchAccess).requireDispatchable(1000, TEAM_SCOPE, SUBJECT_REF);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.importPolicies(1000, manager, batch("POL-T-002"))
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(policyMapper, never()).insert(any(InsurancePolicyEntity.class));
    }

    @Test
    void serviceAccountsWithoutResponsibilityRoleStayUnrestricted() {
        LoginUser serviceAccount = new LoginUser().setId("core-system");
        when(tenantAccessGuard.assignmentScopeOrNull(serviceAccount, 1000)).thenReturn(null);

        InsuranceImportResponse.BatchResult result = service.importPolicies(1000, serviceAccount, batch("POL-S-001"));

        verify(dispatchAccess).requireDispatchable(1000, null, SUBJECT_REF);
        assertEquals("completed", result.status());
        assertEquals(1, result.records().size());
    }
}
