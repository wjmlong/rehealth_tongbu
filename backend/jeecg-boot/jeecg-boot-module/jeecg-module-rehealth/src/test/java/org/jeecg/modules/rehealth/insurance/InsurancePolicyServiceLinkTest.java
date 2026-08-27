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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Policy-to-user linking: staff adds a basic policy to an APP user. One
 * policy may link to many users; repeating the same link is idempotent;
 * the user must be enrolled and inside the caller's responsibility scope.
 */
class InsurancePolicyServiceLinkTest {
    private static final String SUBJECT_REF = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceDispatchAccess dispatchAccess = mock(InsuranceDispatchAccess.class);
    private final InsurancePolicyService service = new InsurancePolicyService(jdbc, dispatchAccess);

    private final InsuranceAssignmentScope selfScope =
            new InsuranceAssignmentScope("emp-1", InsuranceAssignmentScope.MODE_SELF);

    @BeforeEach
    void stubLookups() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("POL-1")))
                .thenReturn(new InsurancePolicyService.PolicyRow("active"));
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000")))
                .thenReturn("user-1");
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("user-1")))
                .thenReturn(new InsurancePolicyService.SubjectRef(SUBJECT_REF, "王老五"));
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("enr-1")))
                .thenReturn(new InsurancePolicyService.SubjectRef(SUBJECT_REF, "王老五"));
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("POL-1"), eq(SUBJECT_REF)))
                .thenReturn(null);
    }

    private InsurancePolicyResponse.LinkRequest byPhone(String policyNo, String phone) {
        return new InsurancePolicyResponse.LinkRequest(policyNo, phone, null);
    }

    private InsurancePolicyResponse.LinkRequest byEnrollment(String policyNo, String enrollmentId) {
        return new InsurancePolicyResponse.LinkRequest(policyNo, null, enrollmentId);
    }

    @Test
    void linksAPolicyToAResponsibleEnrolledUserByPhone() {
        InsurancePolicyResponse.LinkResult result = service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13800000000"));

        assertEquals("POL-1", result.policyNo());
        assertEquals(SUBJECT_REF, result.subjectRef());
        assertEquals("王老五", result.userName());
        verify(dispatchAccess).requireDispatchable(1000, selfScope, SUBJECT_REF);
        verify(jdbc).update(anyString(), any(), eq(1000), eq("POL-1"), eq(SUBJECT_REF), eq("emp-1"), any(), any());
    }

    @Test
    void linksAPolicyByEnrollmentId() {
        InsurancePolicyResponse.LinkResult result = service.link(1000, selfScope, "emp-1", byEnrollment("POL-1", "enr-1"));

        assertEquals(SUBJECT_REF, result.subjectRef());
        verify(dispatchAccess).requireDispatchable(1000, selfScope, SUBJECT_REF);
        verify(jdbc).update(anyString(), any(), eq(1000), eq("POL-1"), eq(SUBJECT_REF), eq("emp-1"), any(), any());
    }

    @Test
    void repeatedLinkForTheSameUserIsIdempotent() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("POL-1"), eq(SUBJECT_REF)))
                .thenReturn(new InsurancePolicyService.InsurancePolicyLinkRow("assigned"));

        InsurancePolicyResponse.LinkResult result = service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13800000000"));

        assertEquals(SUBJECT_REF, result.subjectRef());
        verify(jdbc, never()).update(anyString(), any(), eq(1000), eq("POL-1"), eq(SUBJECT_REF), eq("emp-1"), any(), any());
    }

    @Test
    void requiresExactlyOneOfPhoneAndEnrollmentId() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class,
                        () -> service.link(1000, selfScope, "emp-1", new InsurancePolicyResponse.LinkRequest("POL-1", null, null))).status()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class,
                        () -> service.link(1000, selfScope, "emp-1", new InsurancePolicyResponse.LinkRequest("POL-1", "13800000000", "enr-1"))).status()
        );
    }

    @Test
    void rejectsAnUnknownPhone() {
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13900000000")))
                .thenThrow(EmptyResultDataAccessException.class);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13900000000"))
        );

        assertEquals(HttpStatus.NOT_FOUND, error.status());
    }

    @Test
    void rejectsAUserNotEnrolledInTheTenant() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("user-1")))
                .thenThrow(EmptyResultDataAccessException.class);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }

    @Test
    void rejectsASubjectOutsideTheResponsibilityRange() {
        doThrow(InsuranceApiException.forbidden("该被保人不在您的负责范围内，请先认领该用户或选择您负责的用户"))
                .when(dispatchAccess).requireDispatchable(1000, selfScope, SUBJECT_REF);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void rejectsAnInactivePolicy() {
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), eq(1000), eq("POL-1")))
                .thenReturn(new InsurancePolicyService.PolicyRow("ended"));

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.link(1000, selfScope, "emp-1", byPhone("POL-1", "13800000000"))
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
    }
}
