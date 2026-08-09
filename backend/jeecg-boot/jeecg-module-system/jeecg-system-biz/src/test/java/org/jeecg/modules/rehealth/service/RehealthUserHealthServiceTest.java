package org.jeecg.modules.rehealth.service;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.system.entity.SysUserTenant;
import org.jeecg.modules.system.service.ISysUserTenantService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RehealthUserHealthServiceTest {
    @Test
    void pageSizeIsBoundedToOneThroughOneHundred() {
        assertEquals(20, RehealthUserHealthService.boundPageSize(null));
        assertEquals(1, RehealthUserHealthService.boundPageSize(0));
        assertEquals(100, RehealthUserHealthService.boundPageSize(101));
    }

    @Test
    void rejectsUnsupportedRiskLevelBeforeQuery() {
        ISysUserTenantService memberships = mock(ISysUserTenantService.class);
        SysUserTenant active = new SysUserTenant();
        active.setStatus("1");
        when(memberships.getUserTenantByTenantId("admin-a", 1000)).thenReturn(active);
        RehealthUserHealthService service = new RehealthUserHealthService(
                memberships, mock(JdbcTemplate.class), mock(RehealthDeviceHealthClient.class));

        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> service.listPatients("1000", "admin-a", 1, 20, null, "critical")
        );

        assertEquals(400, failure.getStatusCode().value());
    }

    @Test
    void syntheticTelemetryIsRecognizedForRiskSuppression() {
        JSONObject telemetry = new JSONObject();
        telemetry.put("provenance", List.of("LOCAL_TEST_SEED"));
        telemetry.put("isSynthetic", true);

        assertTrue(RehealthUserHealthService.isSyntheticTelemetry(telemetry));
    }

    @Test
    void rejectsTenantOutsideCurrentUserMembership() {
        ISysUserTenantService memberships = mock(ISysUserTenantService.class);
        when(memberships.getUserTenantByTenantId("admin-a", 2000)).thenReturn(null);
        RehealthUserHealthService service = new RehealthUserHealthService(
                memberships, mock(JdbcTemplate.class), mock(RehealthDeviceHealthClient.class));

        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> service.resolveTenant("2000", "admin-a")
        );

        assertEquals(403, failure.getStatusCode().value());
    }

    @Test
    void verifiesTargetMembershipBeforeDeviceRead() {
        ISysUserTenantService memberships = mock(ISysUserTenantService.class);
        SysUserTenant active = new SysUserTenant();
        active.setStatus("1");
        when(memberships.getUserTenantByTenantId("admin-a", 1000)).thenReturn(active);
        RehealthDeviceHealthClient deviceClient = mock(RehealthDeviceHealthClient.class);
        EmptyPatientJdbcTemplate jdbc = new EmptyPatientJdbcTemplate();
        RehealthUserHealthService service = new RehealthUserHealthService(
                memberships, jdbc, deviceClient);

        ResponseStatusException failure = assertThrows(
                ResponseStatusException.class,
                () -> service.patientDetail("1000", "admin-a", "outside-user")
        );

        assertEquals(404, failure.getStatusCode().value());
        assertTrue(jdbc.sql.contains("sut.tenant_id = ?"));
        verify(deviceClient, never()).fetch("1000", "outside-user");
    }

    private static final class EmptyPatientJdbcTemplate extends JdbcTemplate {
        private String sql = "";

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql = sql;
            return List.of();
        }
    }
}
