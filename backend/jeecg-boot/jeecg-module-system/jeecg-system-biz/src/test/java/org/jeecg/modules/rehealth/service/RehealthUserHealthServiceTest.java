package org.jeecg.modules.rehealth.service;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.modules.system.entity.SysUserTenant;
import org.jeecg.modules.system.service.ISysUserTenantService;
import org.jeecg.modules.rehealth.vo.RehealthUserHealthVO;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void latestRiskWindowUsesMySqlSafeAlias() {
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains(") AS risk_row_num"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("WHERE risk_row_num = 1"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("COALESCE(is_mock, 1) ASC"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("factor_contribution_json"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("JOIN rehealth_patient_profile p"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("priority_intervention"));
        assertTrue(RehealthUserHealthService.BASE_SELECT.contains("intervention_row_num = 1"));
        assertFalse(RehealthUserHealthService.BASE_SELECT.contains(") AS row_number"));
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

        RehealthUserHealthVO patient = new RehealthUserHealthVO();
        assertEquals("unknown", patient.getProvenanceStatus());
        patient.setLatestRisk(new RehealthUserHealthVO.RiskSummary());
        RehealthUserHealthService.attachTelemetry(patient, telemetry);
        assertNull(patient.getLatestRisk());
        assertEquals("synthetic", patient.getProvenanceStatus());

        RehealthUserHealthVO syntheticPreview = new RehealthUserHealthVO();
        RehealthUserHealthVO.RiskSummary mockRisk = new RehealthUserHealthVO.RiskSummary();
        mockRisk.setIsMock(true);
        syntheticPreview.setLatestRisk(mockRisk);
        RehealthUserHealthVO.InterventionSummary mockIntervention =
                new RehealthUserHealthVO.InterventionSummary();
        mockIntervention.setIsMock(true);
        syntheticPreview.setLatestIntervention(mockIntervention);
        RehealthUserHealthService.attachTelemetry(syntheticPreview, telemetry);
        assertNotNull(syntheticPreview.getLatestRisk());
        assertTrue(syntheticPreview.getLatestRisk().getIsMock());
        assertNotNull(syntheticPreview.getLatestIntervention());
        assertTrue(syntheticPreview.getLatestIntervention().getIsMock());

        RehealthUserHealthVO markerPatient = new RehealthUserHealthVO();
        markerPatient.setLatestRisk(new RehealthUserHealthVO.RiskSummary());
        RehealthUserHealthService.attachTelemetry(
                markerPatient, telemetry(false, List.of("ring_sim")));
        assertEquals("synthetic", markerPatient.getProvenanceStatus());
        assertNull(markerPatient.getLatestRisk());

        JSONObject realTelemetry = new JSONObject();
        realTelemetry.put("provenance", List.of("hband_wearable"));
        realTelemetry.put("isSynthetic", false);
        RehealthUserHealthVO realPatient = new RehealthUserHealthVO();
        RehealthUserHealthVO.RiskSummary realRisk = new RehealthUserHealthVO.RiskSummary();
        realRisk.setIsMock(false);
        realPatient.setLatestRisk(realRisk);
        RehealthUserHealthVO.InterventionSummary realIntervention =
                new RehealthUserHealthVO.InterventionSummary();
        realIntervention.setIsMock(false);
        realPatient.setLatestIntervention(realIntervention);
        RehealthUserHealthService.attachTelemetry(realPatient, realTelemetry);
        assertEquals("verified_real", realPatient.getProvenanceStatus());
        assertNotNull(realPatient.getLatestRisk());
        assertNotNull(realPatient.getLatestIntervention());

        RehealthUserHealthVO mismatchedPlan = new RehealthUserHealthVO();
        RehealthUserHealthVO.InterventionSummary unmarkedPlan =
                new RehealthUserHealthVO.InterventionSummary();
        unmarkedPlan.setIsMock(false);
        mismatchedPlan.setLatestIntervention(unmarkedPlan);
        RehealthUserHealthService.attachTelemetry(mismatchedPlan, telemetry);
        assertNull(mismatchedPlan.getLatestIntervention());

        for (JSONObject unverified : List.of(
                new JSONObject(),
                telemetry(false, List.of()),
                telemetry(false, List.of("unregistered_vendor")),
                telemetry(false, List.of("hband_wearable", "unregistered_vendor")))) {
            RehealthUserHealthVO unknownPatient = new RehealthUserHealthVO();
            unknownPatient.setLatestRisk(new RehealthUserHealthVO.RiskSummary());
            RehealthUserHealthService.attachTelemetry(unknownPatient, unverified);
            assertEquals("unknown", unknownPatient.getProvenanceStatus());
            assertNull(unknownPatient.getLatestRisk());
        }
    }

    @Test
    void dailyPreviewMustMatchVerifiedOrSyntheticProvenance() {
        assertTrue(RehealthUserHealthService.previewMatchesProvenance("synthetic", true));
        assertTrue(RehealthUserHealthService.previewMatchesProvenance("verified_real", false));
        assertFalse(RehealthUserHealthService.previewMatchesProvenance("synthetic", false));
        assertFalse(RehealthUserHealthService.previewMatchesProvenance("verified_real", true));
        assertFalse(RehealthUserHealthService.previewMatchesProvenance("unknown", true));
        assertTrue(RehealthUserHealthService.isSyntheticSource("LOCAL_MEDICAL_TEST_SEED"));
        assertFalse(RehealthUserHealthService.isSyntheticSource("android_rhi_v2"));
    }

    private static JSONObject telemetry(boolean synthetic, List<String> provenance) {
        JSONObject telemetry = new JSONObject();
        telemetry.put("isSynthetic", synthetic);
        telemetry.put("provenance", provenance);
        return telemetry;
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
        assertTrue(jdbc.sql.contains("NOT EXISTS"));
        assertTrue(jdbc.sql.contains("other_membership.status = '1'"));
        assertTrue(jdbc.sql.contains("other_membership.tenant_id <> sut.tenant_id"));
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
