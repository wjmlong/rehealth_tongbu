package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcInsuranceRiskRepositoryContractTest {
    @Test
    void detailResolvesTenantBoundPseudonymAndNeverSelectsRawUserIdAsSubjectId() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        String subjectRef = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        when(jdbc.query(any(String.class),
                ArgumentMatchers.<RowMapper<InsuranceRiskRepository.SubjectSnapshot>>any(),
                eq(1000), isNull(), isNull(), isNull(), isNull(), eq(subjectRef))).thenReturn(List.of());
        JdbcInsuranceRiskRepository repository = new JdbcInsuranceRiskRepository(jdbc);

        repository.subject(1000, subjectRef);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(),
                ArgumentMatchers.<RowMapper<InsuranceRiskRepository.SubjectSnapshot>>any(),
                eq(1000), isNull(), isNull(), isNull(), isNull(), eq(subjectRef));
        String query = sql.getValue();
        assertTrue(query.contains("insurance_subject.subject_ref AS subject_id"));
        assertTrue(query.contains("insurance_subject.rehealth_user_id AS internal_user_id"));
        assertTrue(query.contains("insurance_subject.enrollment_status = 'active'"));
        assertTrue(query.contains("WHERE ts.subject_id = ?"));
        assertTrue(query.contains("rehealth_insurance_user_assignment"));
        assertTrue(query.contains("rehealth_insurance_enrollment"));
        assertFalse(query.contains("rehealth_insurance_subject_manager"));
        assertFalse(query.contains("FROM sys_user_tenant"));
        assertFalse(query.contains("SELECT ts.internal_user_id"));
        assertFalse(query.contains("ROW_NUMBER()"));
        assertFalse(query.contains("profile.user_id COLLATE"));
        assertTrue(query.contains("profile.user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci"));
    }

    @Test
    void teamScopeExtendsOwnershipToSharedDepartmentsInsideTheSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        String subjectRef = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
        when(jdbc.query(any(String.class),
                ArgumentMatchers.<RowMapper<InsuranceRiskRepository.SubjectSnapshot>>any(),
                eq(1000), eq("manager-1"), eq("manager-1"), eq("TEAM"), eq("manager-1"),
                eq(subjectRef))).thenReturn(List.of());
        JdbcInsuranceRiskRepository repository = new JdbcInsuranceRiskRepository(jdbc);

        repository.subject(1000, new InsuranceAssignmentScope("manager-1", InsuranceAssignmentScope.MODE_TEAM), subjectRef);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(),
                ArgumentMatchers.<RowMapper<InsuranceRiskRepository.SubjectSnapshot>>any(),
                eq(1000), eq("manager-1"), eq("manager-1"), eq("TEAM"), eq("manager-1"), eq(subjectRef));
        String query = sql.getValue();
        assertTrue(query.contains("? = 'TEAM'"));
        assertTrue(query.contains("sys_user_depart manager_depart"));
        assertTrue(query.contains("CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci"));
    }
}
