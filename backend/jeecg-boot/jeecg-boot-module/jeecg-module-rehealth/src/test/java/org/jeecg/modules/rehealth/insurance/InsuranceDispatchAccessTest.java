package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceDispatchAccessTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceDispatchAccess access = new InsuranceDispatchAccess(jdbc);

    private static final String SUBJECT_REF = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void selfScopeAcceptsASubjectAssignedToTheCurrentEmployee() {
        InsuranceAssignmentScope scope = new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF);
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                eq(1000), eq(SUBJECT_REF), eq("emp-1"), eq("SELF"), eq("emp-1")))
                .thenReturn(1);

        assertDoesNotThrow(() -> access.requireDispatchable(1000, scope, SUBJECT_REF));
    }

    @Test
    void teamScopeAcceptsASubjectAssignedToAnyDepartmentColleague() {
        InsuranceAssignmentScope scope = new InsuranceAssignmentScope("mgr-1", InsuranceAssignmentScope.MODE_TEAM);
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                eq(1000), eq(SUBJECT_REF), eq("mgr-1"), eq("TEAM"), eq("mgr-1")))
                .thenReturn(1);

        assertDoesNotThrow(() -> access.requireDispatchable(1000, scope, SUBJECT_REF));
    }

    @Test
    void rejectsASubjectOutsideTheResponsibilityRange() {
        InsuranceAssignmentScope scope = new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF);
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                eq(1000), eq(SUBJECT_REF), eq("emp-1"), eq("SELF"), eq("emp-1")))
                .thenReturn(0);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> access.requireDispatchable(1000, scope, SUBJECT_REF)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void rejectsBlankSubjectReferences() {
        InsuranceAssignmentScope scope = new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> access.requireDispatchable(1000, scope, "  ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void unrestrictedScopeSkipsTheCheckEntirely() {
        assertDoesNotThrow(() -> access.requireDispatchable(1000, null, SUBJECT_REF));
        verify(jdbc, never()).queryForObject(anyString(), eq(Integer.class));
    }
}
