package org.jeecg.modules.rehealth.insurance;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class InsuranceSettingsServiceTenantIsolationTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void memberDirectoryExcludesPlatformAdministratorsInSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        service.members(1001);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class));
        assertTrue(sql.getValue().contains("platform_role.role_code IN ('admin', 'super_admin')"));
        assertTrue(sql.getValue().contains("NOT EXISTS"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void departmentCountsExcludePlatformAdministratorsInSql() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        service.departments(1001);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class));
        assertTrue(sql.getValue().contains("platform_role.role_code IN ('admin', 'super_admin')"));
        assertTrue(sql.getValue().contains("platform_user_role.user_id = department_user.id"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void changingDepartmentDeletesOnlyMembershipsOwnedByTheRequestedTenant() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("shared-user"), eq(1001)))
                .thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("tenant-1001-department"), eq(1001)))
                .thenReturn(1);
        when(jdbc.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        assertThrows(
                InsuranceApiException.class,
                () -> service.updateMemberDepartment(1001, "shared-user", "tenant-1001-department")
        );

        verify(jdbc).queryForObject(
                org.mockito.ArgumentMatchers.argThat(sql ->
                        sql.contains("platform_role.role_code IN ('admin', 'super_admin')")),
                eq(Integer.class), eq("shared-user"), eq(1001)
        );

        org.mockito.InOrder writes = inOrder(jdbc);
        writes.verify(jdbc).update(
                org.mockito.ArgumentMatchers.argThat(sql ->
                        sql.contains("JOIN sys_depart")
                                && sql.contains("department.tenant_id = ?")
                                && !sql.trim().equals("DELETE FROM sys_user_depart WHERE user_id = ?")
                ),
                eq("shared-user"),
                eq(1001)
        );
        writes.verify(jdbc).update(
                eq("INSERT INTO sys_user_depart (ID, user_id, dep_id) VALUES (?, ?, ?)"),
                anyString(),
                eq("shared-user"),
                eq("tenant-1001-department")
        );
    }

    @Test
    void invitationUsesOnlyTheRequestedTenantAndAnOwnedDepartment() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("13800000000")))
                .thenReturn("invited-user");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("invited-user"), eq(1001)))
                .thenReturn(0);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("tenant-1001-department"), eq(1001)))
                .thenReturn(1);

        InsuranceSettingsResponse.MemberInvitation invitation = service.inviteMember(
                1001, "operator-user", "13800000000", "tenant-1001-department"
        );

        verify(jdbc).update(
                org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("INSERT INTO sys_user_tenant")),
                anyString(), eq("invited-user"), eq(1001), eq("operator-user"), any()
        );
        verify(jdbc).update(
                eq("INSERT INTO sys_user_depart (id, user_id, dep_id) VALUES (?, ?, ?)"),
                anyString(), eq("invited-user"), eq("tenant-1001-department")
        );
        verify(jdbc).queryForObject(
                org.mockito.ArgumentMatchers.argThat(sql ->
                        sql.contains("platform_role.role_code IN ('admin', 'super_admin')")),
                eq(String.class), eq("13800000000")
        );
        org.junit.jupiter.api.Assertions.assertEquals("invited", invitation.status());
    }

    @Test
    void creatingMemberWritesOnlyCurrentTenantMembershipDepartmentAndRole() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("tenant-department"), eq(1001)))
                .thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("insurer_analyst"), eq(1001), eq(1001)))
                .thenReturn("analyst-role");

        InsuranceSettingsResponse.MemberCreation created = service.createMember(
                1001,
                "operator-user",
                new InsuranceSettingsRequest.MemberCreation(
                        null, "新成员", "13800000001", "new-member@example.com",
                        "tenant-department", "insurer_analyst"
                )
        );

        assertNotNull(created.userId());
        assertEquals("13800000001", created.username());
        assertEquals(16, created.temporaryPassword().length());
        assertTrue(created.mustChangePassword());
        verify(jdbc).update(
                org.mockito.ArgumentMatchers.argThat(sql -> sql.contains("INSERT INTO sys_user_tenant")),
                anyString(), anyString(), eq(1001), eq("operator-user"), any()
        );
        verify(jdbc).update(
                eq("INSERT INTO sys_user_role (id, user_id, role_id, tenant_id) VALUES (?, ?, ?, ?)"),
                anyString(), anyString(), eq("analyst-role"), eq(1001)
        );
    }

    @Test
    void currentOperatorCannotDisableTheirOwnTenantMembership() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("operator-user"), eq(1001)))
                .thenReturn(1);

        assertThrows(
                InsuranceApiException.class,
                () -> service.updateMemberStatus(1001, "operator-user", "operator-user", "disabled")
        );
    }

    @Test
    void pendingInvitationCannotBeActivatedByAnAdministrator() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InsuranceSettingsService service = new InsuranceSettingsService(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("invited-user"), eq(1001)))
                .thenReturn(1);
        when(jdbc.queryForObject(anyString(), eq(String.class), eq("invited-user"), eq(1001)))
                .thenReturn("5");

        assertThrows(
                InsuranceApiException.class,
                () -> service.updateMemberStatus(1001, "operator-user", "invited-user", "active")
        );
    }
}
