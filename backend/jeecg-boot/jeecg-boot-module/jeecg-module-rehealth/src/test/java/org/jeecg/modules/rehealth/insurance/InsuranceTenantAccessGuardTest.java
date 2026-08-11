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
}
