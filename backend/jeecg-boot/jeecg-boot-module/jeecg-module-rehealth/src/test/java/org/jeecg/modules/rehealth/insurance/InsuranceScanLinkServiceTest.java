package org.jeecg.modules.rehealth.insurance;

import org.jeecg.modules.rehealth.insurance.entity.InsuranceEmployeeQrEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceScanSessionEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceEmployeeQrMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceScanSessionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InsuranceScanLinkServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final InsuranceEmployeeQrMapper qrMapper = mock(InsuranceEmployeeQrMapper.class);
    private final InsuranceScanSessionMapper sessionMapper = mock(InsuranceScanSessionMapper.class);
    private final InsuranceAssignmentService assignmentService = mock(InsuranceAssignmentService.class);
    private final InsuranceScanLinkService service = new InsuranceScanLinkService(
            jdbc, qrMapper, sessionMapper, assignmentService);

    private InsuranceEmployeeQrEntity qr(String code, String status, LocalDateTime expiresAt) {
        InsuranceEmployeeQrEntity entity = new InsuranceEmployeeQrEntity();
        entity.setId("qr-1");
        entity.setTenantId(1001);
        entity.setEmployeeId("emp-1");
        entity.setCode(code);
        entity.setStatus(status);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }

    private void stubEmployeePreview(String name) {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1001), eq(1001), eq("emp-1")))
                .thenReturn(java.util.List.of(new InsuranceScanLinkResponse.ScanPreview.Employee(
                        name, "康泰人寿", "健康险运营部", name.substring(0, 1))));
    }

    private void stubNoExistingContact() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1001), eq("app-user-1")))
                .thenReturn(java.util.List.of());
    }

    @Test
    void ensureQrCreatesAnEightCharacterCodeWithTheScanPayload() {
        when(qrMapper.selectOne(any())).thenReturn(null);
        when(qrMapper.selectCount(any())).thenReturn(0L);

        InsuranceScanLinkResponse.QrCode result = service.ensureQr(1001, "emp-1", null);

        assertEquals(8, result.code().length());
        assertEquals("active", result.status());
        assertEquals("rehealth://insurance/scan?c=" + result.code() + "&t=1001", result.payload());
        ArgumentCaptor<InsuranceEmployeeQrEntity> captor = ArgumentCaptor.forClass(InsuranceEmployeeQrEntity.class);
        verify(qrMapper).insert(captor.capture());
        assertEquals("emp-1", captor.getValue().getEmployeeId());
    }

    @Test
    void ensureQrRefreshesAnExistingActiveCodeInPlace() {
        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "active", LocalDateTime.now().plusDays(1)));

        InsuranceScanLinkResponse.QrCode result = service.ensureQr(1001, "emp-1", 60);

        assertEquals("ABCD2345", result.code());
        verify(qrMapper).updateById(any(InsuranceEmployeeQrEntity.class));
    }

    @Test
    void disableQrMarksTheCodeDisabled() {
        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "active", LocalDateTime.now().plusDays(1)));

        InsuranceScanLinkResponse.QrCode result = service.disableQr(1001, "emp-1");

        assertEquals("disabled", result.status());
    }

    @Test
    void scanCreatesAPendingSessionWithTheMaskedEmployeePreview() {
        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "active", LocalDateTime.now().plusDays(1)));
        when(sessionMapper.selectCount(any())).thenReturn(0L);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("emp-1"), eq(1001))).thenReturn(1);
        stubEmployeePreview("陈立峰");
        stubNoExistingContact();

        InsuranceScanLinkResponse.ScanPreview result = service.scan("ABCD2345", "app-user-1");

        assertEquals("陈立峰", result.employee().name());
        assertNotNull(result.sessionId());
        ArgumentCaptor<InsuranceScanSessionEntity> captor = ArgumentCaptor.forClass(InsuranceScanSessionEntity.class);
        verify(sessionMapper).insert(captor.capture());
        assertEquals("pending", captor.getValue().getStatus());
        assertEquals("app-user-1", captor.getValue().getUserId());
        assertEquals(1001, captor.getValue().getTenantId());
    }

    @Test
    void scanRejectsUnknownDisabledOrExpiredCodes() {
        when(sessionMapper.selectCount(any())).thenReturn(0L);
        when(qrMapper.selectOne(any())).thenReturn(null);
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(InsuranceApiException.class, () -> service.scan("UNKNOWN8", "app-user-1")).status());

        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "disabled", LocalDateTime.now().plusDays(1)));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(InsuranceApiException.class, () -> service.scan("ABCD2345", "app-user-1")).status());

        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "active", LocalDateTime.now().minusDays(1)));
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(InsuranceApiException.class, () -> service.scan("ABCD2345", "app-user-1")).status());
    }

    @Test
    void scanRejectsAnEmployeeWhoIsNoLongerAnActiveTenantMember() {
        when(qrMapper.selectOne(any())).thenReturn(qr("ABCD2345", "active", LocalDateTime.now().plusDays(1)));
        when(sessionMapper.selectCount(any())).thenReturn(0L);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("emp-1"), eq(1001))).thenReturn(0);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.scan("ABCD2345", "app-user-1")
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
    }

    @Test
    void scanRateLimitsFrequentAttemptsPerUser() {
        when(sessionMapper.selectCount(any())).thenReturn(10L);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.scan("ABCD2345", "app-user-1")
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.status());
    }

    @Test
    void confirmConsumesTheSessionAndCreatesTheRelationship() {
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId("session-1");
        session.setTenantId(1001);
        session.setEmployeeId("emp-1");
        session.setUserId("app-user-1");
        session.setStatus("pending");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(sessionMapper.selectById("session-1")).thenReturn(session);
        when(assignmentService.scanClaim(1001, "emp-1", "app-user-1", false))
                .thenReturn(new InsuranceScanLinkResponse.ConfirmResult(true, false, "陈立峰"));

        InsuranceScanLinkResponse.ConfirmResult result = service.confirm("session-1", "app-user-1", false);

        assertTrue(result.created());
        assertEquals("confirmed", session.getStatus());
        verify(sessionMapper).updateById(session);
    }

    @Test
    void confirmRejectsExpiredSessions() {
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId("session-1");
        session.setTenantId(1001);
        session.setEmployeeId("emp-1");
        session.setUserId("app-user-1");
        session.setStatus("pending");
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(sessionMapper.selectById("session-1")).thenReturn(session);

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.confirm("session-1", "app-user-1", false)
        );

        assertEquals(HttpStatus.GONE, error.status());
        assertEquals("expired", session.getStatus());
    }

    @Test
    void confirmRejectsSessionsOwnedByAnotherUser() {
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId("session-1");
        session.setUserId("app-user-other");
        session.setStatus("pending");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(sessionMapper.selectById("session-1")).thenReturn(session);

        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(InsuranceApiException.class,
                        () -> service.confirm("session-1", "app-user-1", false)).status());
    }

    @Test
    void confirmRejectsReusedSessions() {
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId("session-1");
        session.setUserId("app-user-1");
        session.setStatus("confirmed");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(sessionMapper.selectById("session-1")).thenReturn(session);

        assertEquals(HttpStatus.CONFLICT,
                assertThrows(InsuranceApiException.class,
                        () -> service.confirm("session-1", "app-user-1", false)).status());
    }

    @Test
    void cancelMarksAPendingSessionCancelled() {
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId("session-1");
        session.setUserId("app-user-1");
        session.setStatus("pending");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(sessionMapper.selectById("session-1")).thenReturn(session);

        service.cancel("session-1", "app-user-1");

        assertEquals("cancelled", session.getStatus());
        verify(sessionMapper).updateById(session);
    }
}
