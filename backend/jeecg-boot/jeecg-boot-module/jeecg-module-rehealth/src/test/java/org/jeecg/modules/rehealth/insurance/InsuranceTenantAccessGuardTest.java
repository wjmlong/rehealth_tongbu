package org.jeecg.modules.rehealth.insurance;

import org.jeecg.common.system.vo.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InsuranceTenantAccessGuardTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceTenantAccessGuard guard = new InsuranceTenantAccessGuard(jdbc);

    @Test
    void acceptsOnlyAPositiveTenantAssignedToTheCaller() {
        LoginUser user = new LoginUser().setId("service-user").setRelTenantIds("1000, 1001");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("service-user"), eq(1001)))
                .thenReturn(1);

        assertEquals(1001, guard.requireTenant(user, "1001"));
    }

    @Test
    void rejectsMissingAndZeroTenantAsBadRequests() {
        LoginUser user = new LoginUser().setId("service-user").setRelTenantIds("1000");

        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class, () -> guard.requireTenant(user, null)).status()
        );
        assertEquals(
                HttpStatus.BAD_REQUEST,
                assertThrows(InsuranceApiException.class, () -> guard.requireTenant(user, "0")).status()
        );
    }

    @Test
    void rejectsTenantNotAssignedToTheCaller() {
        LoginUser user = new LoginUser().setId("service-user").setRelTenantIds("1000");

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> guard.requireTenant(user, "1001")
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void rejectsMembershipRevokedAfterTheTokenWasIssued() {
        LoginUser user = new LoginUser().setId("service-user").setRelTenantIds("1000");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("service-user"), eq(1000)))
                .thenReturn(0);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> guard.requireTenant(user, "1000")
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void authorizationDatabaseFailureIsServiceUnavailable() {
        LoginUser user = new LoginUser().setId("service-user").setRelTenantIds("1000");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("service-user"), eq(1000)))
                .thenThrow(new DataAccessResourceFailureException("down"));

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> guard.requireTenant(user, "1000")
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.status());
    }

    @Test
    void everyInsuranceWebRoleUsesItsOwnResponsibilityScope() {
        LoginUser user = new LoginUser().setId("insurance-staff");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("insurance-staff"), eq(9101)))
                .thenReturn(1);

        assertEquals("insurance-staff", guard.responsibilityScope(user, 9101));
    }

    @Test
    void accountWithoutInsuranceWebRoleCannotReadTenantWideRiskData() {
        LoginUser user = new LoginUser().setId("plain-member");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("plain-member"), eq(9101)))
                .thenReturn(0);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> guard.responsibilityScope(user, 9101)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void organizationAdminReceivesUnrestrictedAssignmentScope() {
        LoginUser user = new LoginUser().setId("org-admin");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("org-admin"), eq(9101)))
                .thenReturn(1);

        assertEquals(null, guard.assignmentScope(user, 9101));
    }

    @Test
    void departmentManagerReceivesTeamScope() {
        LoginUser user = new LoginUser().setId("dept-manager");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("dept-manager"), eq(9101)))
                .thenReturn(0, 1, 1);

        InsuranceAssignmentScope scope = guard.assignmentScope(user, 9101);

        assertEquals("dept-manager", scope.userId());
        assertEquals(InsuranceAssignmentScope.MODE_TEAM, scope.mode());
    }

    @Test
    void operatorReceivesSelfScope() {
        LoginUser user = new LoginUser().setId("operator");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("operator"), eq(9101)))
                .thenReturn(0, 0, 1);

        InsuranceAssignmentScope scope = guard.assignmentScope(user, 9101);

        assertEquals("operator", scope.userId());
        assertEquals(InsuranceAssignmentScope.MODE_SELF, scope.mode());
    }

    @Test
    void accountWithoutResponsibilityRoleCannotResolveAssignmentScope() {
        LoginUser user = new LoginUser().setId("plain-member");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("plain-member"), eq(9101)))
                .thenReturn(0, 0, 0);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> guard.assignmentScope(user, 9101)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void assignmentScopeOrNullStaysUnrestrictedForServiceAccountsWithoutResponsibilityRole() {
        LoginUser user = new LoginUser().setId("service-user");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("service-user"), eq(9101)))
                .thenReturn(0);

        assertEquals(null, guard.assignmentScopeOrNull(user, 9101));
    }

    @Test
    void assignmentScopeOrNullResolvesTeamScopeForDepartmentManager() {
        LoginUser user = new LoginUser().setId("dept-manager");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("dept-manager"), eq(9101)))
                .thenReturn(1, 0, 1, 1);

        InsuranceAssignmentScope scope = guard.assignmentScopeOrNull(user, 9101);

        assertEquals("dept-manager", scope.userId());
        assertEquals(InsuranceAssignmentScope.MODE_TEAM, scope.mode());
    }

    @Test
    void assignmentScopeOrNullIsNullForBlankCallers() {
        assertEquals(null, guard.assignmentScopeOrNull(null, 9101));
        assertEquals(null, guard.assignmentScopeOrNull(new LoginUser(), 9101));
    }
}
