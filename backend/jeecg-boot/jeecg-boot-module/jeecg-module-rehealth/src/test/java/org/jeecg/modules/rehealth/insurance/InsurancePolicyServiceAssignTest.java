package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Two-step policy assignment: an unassigned policy is assigned to an APP
 * user by phone only when the user is registered, enrolled, inside the
 * caller's responsibility scope, and the policy is not already assigned
 * to someone else.
 */
class InsurancePolicyServiceAssignTest {
    private static final String SUBJECT_REF = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceDispatchAccess dispatchAccess = mock(InsuranceDispatchAccess.class);
    private final InsurancePolicyService service = new InsurancePolicyService(jdbc, dispatchAccess);

    private final InsuranceAssignmentScope selfScope =
            new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF);

    private InsurancePolicyResponse.AssignRequest request(String policyNo, String phone) {
        return new InsurancePolicyResponse.AssignRequest(policyNo, phone);
    }

    private void stubPolicy(String insuredSubjectRef, String status) {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("POL-1")))
                .thenReturn(new InsurancePolicyService.PolicyRow(insuredSubjectRef, status));
    }

    private void stubSubject(String subjectRef, String userName) {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("user-1")))
                .thenReturn(new InsurancePolicyService.SubjectRef(subjectRef, userName));
    }

    @BeforeEach
    void stubLookups() {
        stubPolicy(null, "active");
        stubSubject(SUBJECT_REF, "王老五");
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000")))
                .thenReturn("user-1");
    }

    @Test
    void assignsAnUnassignedPolicyToAResponsibleEnrolledUser() {
        InsurancePolicyResponse.AssignResult result = service.assign(1000, selfScope, request("POL-1", "13800000000"));

        assertEquals("POL-1", result.policyNo());
        assertEquals(SUBJECT_REF, result.insuredSubjectRef());
        assertEquals("王老五", result.insuredUserName());
        verify(dispatchAccess).requireDispatchable(1000, selfScope, SUBJECT_REF);
        verify(jdbc).update(anyString(), eq(SUBJECT_REF), any(), any(), eq(1000), eq("POL-1"));
    }

    @Test
    void reassigningToTheSameSubjectIsIdempotentAndSkipsTheUpdate() {
        stubPolicy(SUBJECT_REF, "active");

        InsurancePolicyResponse.AssignResult result = service.assign(1000, selfScope, request("POL-1", "13800000000"));

        assertEquals(SUBJECT_REF, result.insuredSubjectRef());
        verify(jdbc, org.mockito.Mockito.never()).update(anyString(), any(), any(), any(), eq(1000), eq("POL-1"));
    }

    @Test
    void rejectsAnUnknownPhone() {
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13900000000")))
                .thenThrow(EmptyResultDataAccessException.class);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.assign(1000, selfScope, request("POL-1", "13900000000"))
        );

        assertEquals(HttpStatus.NOT_FOUND, error.status());
    }

    @Test
    void rejectsAUserNotEnrolledInTheTenant() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("user-1")))
                .thenThrow(EmptyResultDataAccessException.class);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.assign(1000, selfScope, request("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void rejectsASubjectOutsideTheResponsibilityRange() {
        doThrow(InsuranceApiException.forbidden("该被保人不在您的负责范围内，请先认领该用户或选择您负责的用户"))
                .when(dispatchAccess).requireDispatchable(1000, selfScope, SUBJECT_REF);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.assign(1000, selfScope, request("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void rejectsAPolicyAlreadyAssignedToAnotherSubject() {
        stubPolicy("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb", "active");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.assign(1000, selfScope, request("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.CONFLICT, error.status());
    }

    @Test
    void rejectsAnInactivePolicy() {
        stubPolicy(null, "ended");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.assign(1000, selfScope, request("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void rejectsMissingFields() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class,
                        () -> service.assign(1000, selfScope, request(" ", "13800000000"))).status()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class,
                        () -> service.assign(1000, selfScope, request("POL-1", " "))).status()
        );
    }
}
