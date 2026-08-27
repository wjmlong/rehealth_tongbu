package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceEmployeeQrEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceScanSessionEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceEmployeeQrMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceScanSessionMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scan association (phase 2): employee QR codes (30-day validity, one per
 * employee per tenant, refreshable and disableable) and the one-shot scan
 * session flow (scan → confirm/cancel). Relationship creation is delegated
 * to {@link InsuranceAssignmentService#scanClaim} and reuses the phase-1
 * createAssignment mechanics.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码关联服务-----------
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceScanLinkService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int DEFAULT_EXPIRES_MINUTES = 30 * 24 * 60;
    private static final int SESSION_TTL_MINUTES = 5;
    private static final int SCAN_RATE_LIMIT_PER_MINUTE = 10;
    private static final int MAX_SCAN_ATTEMPTS = 3;

    private final JdbcTemplate jdbc;
    private final InsuranceEmployeeQrMapper qrMapper;
    private final InsuranceScanSessionMapper sessionMapper;
    private final InsuranceAssignmentService assignmentService;
    private final SecureRandom random = new SecureRandom();

    public InsuranceScanLinkService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            InsuranceEmployeeQrMapper qrMapper,
            InsuranceScanSessionMapper sessionMapper,
            InsuranceAssignmentService assignmentService
    ) {
        this.jdbc = jdbc;
        this.qrMapper = qrMapper;
        this.sessionMapper = sessionMapper;
        this.assignmentService = assignmentService;
    }

    /** Generates or refreshes the employee QR code (one active code per employee). */
    @Transactional
    public InsuranceScanLinkResponse.QrCode ensureQr(int tenantId, String employeeId, Integer expiresInMinutes) {
        int ttl = expiresInMinutes == null || expiresInMinutes <= 0 ? DEFAULT_EXPIRES_MINUTES : Math.min(expiresInMinutes, 365 * 24 * 60);
        LocalDateTime now = LocalDateTime.now();
        InsuranceEmployeeQrEntity existing = qrMapper.selectOne(new LambdaQueryWrapper<InsuranceEmployeeQrEntity>()
                .eq(InsuranceEmployeeQrEntity::getTenantId, tenantId)
                .eq(InsuranceEmployeeQrEntity::getEmployeeId, employeeId)
                .last("LIMIT 1"));
        if (existing != null && "active".equals(existing.getStatus())) {
            existing.setExpiresAt(now.plusMinutes(ttl));
            existing.setUpdatedAt(now);
            qrMapper.updateById(existing);
            return qrResponse(existing, tenantId);
        }
        InsuranceEmployeeQrEntity entity = existing == null ? new InsuranceEmployeeQrEntity() : existing;
        entity.setId(entity.getId() == null ? uuid() : entity.getId());
        entity.setTenantId(tenantId);
        entity.setEmployeeId(employeeId);
        entity.setCode(generateUniqueCode(tenantId));
        entity.setStatus("active");
        entity.setExpiresAt(now.plusMinutes(ttl));
        entity.setCreatedAt(entity.getCreatedAt() == null ? now : entity.getCreatedAt());
        entity.setUpdatedAt(now);
        if (existing == null) {
            qrMapper.insert(entity);
        } else {
            qrMapper.updateById(entity);
        }
        return qrResponse(entity, tenantId);
    }

    public InsuranceScanLinkResponse.QrCode currentQr(int tenantId, String employeeId) {
        InsuranceEmployeeQrEntity existing = qrMapper.selectOne(new LambdaQueryWrapper<InsuranceEmployeeQrEntity>()
                .eq(InsuranceEmployeeQrEntity::getTenantId, tenantId)
                .eq(InsuranceEmployeeQrEntity::getEmployeeId, employeeId)
                .last("LIMIT 1"));
        if (existing == null) {
            return null;
        }
        return qrResponse(existing, tenantId);
    }

    @Transactional
    public InsuranceScanLinkResponse.QrCode disableQr(int tenantId, String employeeId) {
        InsuranceEmployeeQrEntity existing = qrMapper.selectOne(new LambdaQueryWrapper<InsuranceEmployeeQrEntity>()
                .eq(InsuranceEmployeeQrEntity::getTenantId, tenantId)
                .eq(InsuranceEmployeeQrEntity::getEmployeeId, employeeId)
                .last("LIMIT 1"));
        if (existing == null) {
            throw InsuranceApiException.notFound("当前员工尚未生成二维码");
        }
        existing.setStatus("disabled");
        existing.setUpdatedAt(LocalDateTime.now());
        qrMapper.updateById(existing);
        return qrResponse(existing, tenantId);
    }

    /**
     * Validates the scanned code and creates a pending one-shot session.
     * Rate limits: at most {@link #SCAN_RATE_LIMIT_PER_MINUTE} scans per user
     * per minute.
     */
    @Transactional
    public InsuranceScanLinkResponse.ScanPreview scan(String code, String userId) {
        String normalized = required(code, "employeeCode", 16);
        enforceRateLimit(userId);
        InsuranceEmployeeQrEntity qr = qrMapper.selectOne(new LambdaQueryWrapper<InsuranceEmployeeQrEntity>()
                .eq(InsuranceEmployeeQrEntity::getCode, normalized)
                .last("LIMIT 1"));
        if (qr == null || !isUsable(qr)) {
            throw InsuranceApiException.notFound("码无效或已失效");
        }
        int tenantId = qr.getTenantId();
        if (!isActiveTenantMember(tenantId, qr.getEmployeeId())) {
            throw InsuranceApiException.forbidden("该员工已不在服务状态");
        }
        LocalDateTime now = LocalDateTime.now();
        InsuranceScanSessionEntity session = new InsuranceScanSessionEntity();
        session.setId(uuid());
        session.setTenantId(tenantId);
        session.setEmployeeId(qr.getEmployeeId());
        session.setQrId(qr.getId());
        session.setUserId(userId);
        session.setStatus("pending");
        session.setExpiresAt(now.plusMinutes(SESSION_TTL_MINUTES));
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);
        return new InsuranceScanLinkResponse.ScanPreview(
                session.getId(),
                session.getExpiresAt(),
                employeePreview(tenantId, qr.getEmployeeId()),
                existingPrimaryContact(tenantId, userId)
        );
    }

    /** One-shot confirm: consumes the session and creates/replaces the service relationship. */
    @Transactional
    public InsuranceScanLinkResponse.ConfirmResult confirm(
            String sessionId, String userId, boolean replaceExisting
    ) {
        InsuranceScanSessionEntity session = sessionMapper.selectById(required(sessionId, "sessionId", 64));
        if (session == null || !session.getUserId().equals(userId)) {
            throw InsuranceApiException.notFound("扫码会话不存在");
        }
        if (!"pending".equals(session.getStatus())) {
            throw InsuranceApiException.conflict("该扫码会话已被使用或已失效，请重新扫码");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus("expired");
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            throw InsuranceApiException.gone("扫码已超时，请重新扫码");
        }
        InsuranceScanLinkResponse.ConfirmResult result = assignmentService.scanClaim(
                session.getTenantId(), session.getEmployeeId(), userId, replaceExisting);
        LocalDateTime now = LocalDateTime.now();
        session.setStatus("confirmed");
        session.setConfirmedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.updateById(session);
        return result;
    }

    @Transactional
    public void cancel(String sessionId, String userId) {
        InsuranceScanSessionEntity session = sessionMapper.selectById(required(sessionId, "sessionId", 64));
        if (session == null || !session.getUserId().equals(userId)) {
            throw InsuranceApiException.notFound("扫码会话不存在");
        }
        if ("pending".equals(session.getStatus())) {
            session.setStatus("cancelled");
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private boolean isUsable(InsuranceEmployeeQrEntity qr) {
        return "active".equals(qr.getStatus())
                && (qr.getExpiresAt() == null || qr.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    private boolean isActiveTenantMember(int tenantId, String employeeId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_tenant membership
                INNER JOIN sys_user account
                    ON account.id = CONVERT(membership.user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE membership.user_id = ? AND membership.tenant_id = ?
                  AND membership.status = '1' AND account.status = 1 AND account.del_flag = 0
                """, Integer.class, employeeId, tenantId);
        return count != null && count > 0;
    }

    private InsuranceScanLinkResponse.ScanPreview.Employee employeePreview(int tenantId, String employeeId) {
        try {
            List<InsuranceScanLinkResponse.ScanPreview.Employee> rows = jdbc.query("""
                    SELECT account.realname, tenant.name, COALESCE(depart.depart_name, '')
                    FROM sys_user account
                    JOIN sys_tenant tenant ON tenant.id = CAST(? AS CHAR)
                    LEFT JOIN sys_user_depart membership ON membership.user_id = account.id
                    LEFT JOIN sys_depart depart ON depart.id = membership.dep_id AND depart.tenant_id = ?
                    WHERE account.id = CONVERT(? USING utf8mb3) COLLATE utf8mb3_general_ci
                    LIMIT 1
                    """, (rs, rowNum) -> {
                String name = rs.getString(1) == null ? "服务专员" : rs.getString(1);
                return new InsuranceScanLinkResponse.ScanPreview.Employee(
                        name,
                        rs.getString(2),
                        rs.getString(3),
                        name.substring(0, 1));
            }, tenantId, tenantId, employeeId);
            return rows.isEmpty()
                    ? new InsuranceScanLinkResponse.ScanPreview.Employee("服务专员", null, null, "服")
                    : rows.get(0);
        } catch (org.springframework.dao.DataAccessException ignored) {
            return new InsuranceScanLinkResponse.ScanPreview.Employee("服务专员", null, null, "服");
        }
    }

    private InsuranceScanLinkResponse.ScanPreview.Contact existingPrimaryContact(int tenantId, String userId) {
        try {
            List<InsuranceScanLinkResponse.ScanPreview.Contact> rows = jdbc.query("""
                    SELECT emp.realname, a.role_type
                    FROM rehealth_insurance_user_assignment a
                    JOIN rehealth_insurance_enrollment e ON e.id = a.enrollment_id
                    LEFT JOIN sys_user emp ON emp.id = CONVERT(a.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    WHERE e.tenant_id = ? AND e.rehealth_user_id = ? AND a.status = 'active'
                    ORDER BY CASE WHEN a.role_type = 'PRIMARY' THEN 0 ELSE 1 END, a.start_time DESC, a.id DESC
                    LIMIT 1
                    """, (rs, rowNum) -> new InsuranceScanLinkResponse.ScanPreview.Contact(
                            String.valueOf(tenantId),
                            rs.getString(1),
                            rs.getString(2)
                    ), tenantId, userId);
            return rows.isEmpty() ? null : rows.get(0);
        } catch (org.springframework.dao.DataAccessException ignored) {
            return null;
        }
    }

    private void enforceRateLimit(String userId) {
        Long recent = sessionMapper.selectCount(new LambdaQueryWrapper<InsuranceScanSessionEntity>()
                .eq(InsuranceScanSessionEntity::getUserId, userId)
                .ge(InsuranceScanSessionEntity::getCreatedAt, LocalDateTime.now().minusMinutes(1)));
        if (recent != null && recent >= SCAN_RATE_LIMIT_PER_MINUTE) {
            throw InsuranceApiException.tooManyRequests("操作过于频繁，请稍后再试");
        }
        if (recent != null && recent >= MAX_SCAN_ATTEMPTS) {
            // 轻量防枚举：短窗口内多次尝试即限流（与每分钟上限叠加）
            throw InsuranceApiException.tooManyRequests("操作过于频繁，请稍后再试");
        }
    }

    private String generateUniqueCode(int tenantId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder code = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            String value = code.toString();
            Long clash = qrMapper.selectCount(new LambdaQueryWrapper<InsuranceEmployeeQrEntity>()
                    .eq(InsuranceEmployeeQrEntity::getTenantId, tenantId)
                    .eq(InsuranceEmployeeQrEntity::getCode, value));
            if (clash == null || clash < 1) {
                return value;
            }
        }
        throw InsuranceApiException.serviceUnavailable("二维码生成失败，请重试");
    }

    private InsuranceScanLinkResponse.QrCode qrResponse(InsuranceEmployeeQrEntity entity, int tenantId) {
        long scanCount = 0;
        Long count = sessionMapper.selectCount(new LambdaQueryWrapper<InsuranceScanSessionEntity>()
                .eq(InsuranceScanSessionEntity::getQrId, entity.getId()));
        if (count != null) {
            scanCount = count;
        }
        return new InsuranceScanLinkResponse.QrCode(
                entity.getCode(),
                entity.getStatus(),
                entity.getExpiresAt(),
                scanCount,
                "rehealth://insurance/scan?c=" + entity.getCode() + "&t=" + tenantId
        );
    }

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw InsuranceApiException.badRequest(field + " is required");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw InsuranceApiException.badRequest(field + " must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码关联服务-----------
