package org.jeecg.modules.rehealth.account;

import org.jeecg.common.util.PasswordUtil;
import org.jeecg.modules.rehealth.insurance.InsuranceApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountPasswordServiceTest {
    private static final String USER_ID = "user-1";
    private static final String USERNAME = "employee";
    private static final String SALT = "salt1234";

    @Test
    void statusUsesExplicitForcedChangeState() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AccountPasswordService.Credential>>any(), eq(USER_ID)))
                .thenReturn(new AccountPasswordService.Credential(USERNAME, PasswordUtil.encrypt(USERNAME, "Existing1!", SALT), SALT));
        when(jdbc.queryForObject(
                eq("SELECT must_change_password FROM rehealth_user_password_state WHERE user_id = ?"),
                eq(Integer.class), eq(USER_ID))).thenReturn(1);

        assertTrue(new AccountPasswordService(jdbc).status(USER_ID).mustChangePassword());
    }

    @Test
    void selfChangeRejectsDefaultPasswordAndWritesStateOnSuccess() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AccountPasswordService.Credential>>any(), eq(USER_ID)))
                .thenReturn(new AccountPasswordService.Credential(USERNAME, PasswordUtil.encrypt(USERNAME, "Existing1!", SALT), SALT));
        AccountPasswordService service = new AccountPasswordService(jdbc);

        assertThrows(InsuranceApiException.class, () -> service.changeOwnPassword(
                USER_ID, new AccountPasswordRequest.Change("Existing1!", "123456", "123456")));

        service.changeOwnPassword(USER_ID,
                new AccountPasswordRequest.Change("Existing1!", "NewPassword1!", "NewPassword1!"));

        verify(jdbc).update(org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("UPDATE sys_user")), any(), any(), any(), any(), eq(USER_ID), eq(USER_ID));
        verify(jdbc).update(org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("INSERT INTO rehealth_user_password_state")), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void resetRequiresAnActiveMemberInTheCurrentTenantAndCannotResetSelf() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        AccountPasswordService service = new AccountPasswordService(jdbc);

        assertThrows(InsuranceApiException.class, () -> service.resetTenantMemberPassword(1001, USER_ID, USER_ID));

        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("target"), eq(1001))).thenReturn(0);
        assertThrows(InsuranceApiException.class, () -> service.resetTenantMemberPassword(1001, USER_ID, "target"));
    }

    @Test
    void resetWritesDefaultPasswordAndForcedStateForCurrentTenantMember() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("target"), eq(1001))).thenReturn(1);
        when(jdbc.queryForObject(anyString(), org.mockito.ArgumentMatchers.<RowMapper<AccountPasswordService.Credential>>any(), eq("target")))
                .thenReturn(new AccountPasswordService.Credential(USERNAME, PasswordUtil.encrypt(USERNAME, "Existing1!", SALT), SALT));

        AccountPasswordResponse.Reset result = new AccountPasswordService(jdbc)
                .resetTenantMemberPassword(1001, USER_ID, "target");

        assertTrue(result.mustChangePassword());
        assertFalse(result.message().isBlank());
        verify(jdbc).update(org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("UPDATE sys_user")), any(), any(), any(), any(), eq(USER_ID), eq("target"));
        verify(jdbc).update(org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("INSERT INTO rehealth_user_password_state")), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
