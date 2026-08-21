package org.jeecg.modules.rehealth.account;

import org.jeecg.common.constant.PasswordConstant;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.rehealth.insurance.InsuranceApiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.jeecg.modules.rehealth.account.AccountPasswordResponse.Change;
import static org.jeecg.modules.rehealth.account.AccountPasswordResponse.Reset;
import static org.jeecg.modules.rehealth.account.AccountPasswordResponse.Status;

/**
 * Password operations shared by the ReHealth website and tenant settings.
 * The credential remains the global Jeecg sys_user credential; tenant scope is
 * enforced only for administrator reset operations.
 */
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class AccountPasswordService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final String DEFAULT_PASSWORD = PasswordConstant.DEFAULT_PASSWORD;
    private static final String EXCLUDE_PLATFORM_ADMINS = """
            AND NOT EXISTS (
                SELECT 1
                FROM sys_user_role platform_user_role
                INNER JOIN sys_role platform_role ON platform_role.id = platform_user_role.role_id
                WHERE platform_user_role.user_id = u.id
                  AND platform_role.role_code IN ('admin', 'super_admin')
            )
            """;

    private final JdbcTemplate jdbc;

    public AccountPasswordService(@Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Status status(String userId) {
        Credential credential = credential(userId);
        Integer forced = stateFlag(userId);
        boolean defaultPassword = PasswordUtil.encrypt(credential.username(), DEFAULT_PASSWORD, credential.salt())
                .equals(credential.password());
        return new Status((forced != null && forced == 1) || defaultPassword);
    }

    @Transactional
    public Change changeOwnPassword(String userId, AccountPasswordRequest.Change request) {
        Credential credential = credential(userId);
        String oldPassword = requiredPassword(request == null ? null : request.oldPassword(), "oldPassword");
        String newPassword = requiredPassword(request == null ? null : request.newPassword(), "newPassword");
        String confirmPassword = requiredPassword(request == null ? null : request.confirmPassword(), "confirmPassword");

        String oldEncoded = PasswordUtil.encrypt(credential.username(), oldPassword, credential.salt());
        if (!oldEncoded.equals(credential.password())) {
            throw InsuranceApiException.badRequest("旧密码不正确");
        }
        validateNewPassword(credential.username(), newPassword, confirmPassword);

        String salt = oConvertUtils.randomGen(8);
        String encoded = PasswordUtil.encrypt(credential.username(), newPassword, salt);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE sys_user
                SET password = ?, salt = ?, last_pwd_update_time = ?, update_time = ?, update_by = ?
                WHERE id = ? AND del_flag = 0
                """, encoded, salt, now, now, userId, userId);
        markChanged(userId, now);
        return new Change(false, "密码修改成功，请重新登录");
    }

    @Transactional
    public Reset resetTenantMemberPassword(int tenantId, String operatorId, String userId) {
        if (operatorId == null || operatorId.isBlank()) {
            throw InsuranceApiException.forbidden("操作人身份无效");
        }
        if (operatorId.equals(userId)) {
            throw InsuranceApiException.badRequest("不能通过管理员重置接口重置自己的密码，请使用修改密码功能");
        }
        requireActiveTenantMember(tenantId, userId);
        Credential credential = credential(userId);
        String salt = oConvertUtils.randomGen(8);
        String encoded = PasswordUtil.encrypt(credential.username(), DEFAULT_PASSWORD, salt);
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE sys_user
                SET password = ?, salt = ?, last_pwd_update_time = ?, update_time = ?, update_by = ?
                WHERE id = ? AND del_flag = 0
                """, encoded, salt, now, now, operatorId, userId);
        markReset(userId, tenantId, operatorId, now);
        return new Reset(true, "密码已重置为默认密码，请通知成员首次登录后修改");
    }

    /** Marks a newly created account as requiring its first password change. */
    public void markNewMember(String userId, LocalDateTime now) {
        upsertState(userId, true, "new_member", null, null, null, null, now);
    }

    private Credential credential(String userId) {
        if (userId == null || userId.isBlank()) {
            throw InsuranceApiException.badRequest("userId is required");
        }
        try {
            return jdbc.queryForObject("""
                    SELECT username, password, salt
                    FROM sys_user
                    WHERE id = ? AND del_flag = 0
                    """, (rs, rowNum) -> new Credential(
                    rs.getString("username"), rs.getString("password"), rs.getString("salt")
            ), userId);
        } catch (EmptyResultDataAccessException e) {
            throw InsuranceApiException.notFound("用户不存在");
        }
    }

    private Integer stateFlag(String userId) {
        try {
            return jdbc.queryForObject(
                    "SELECT must_change_password FROM rehealth_user_password_state WHERE user_id = ?",
                    Integer.class, userId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private void requireActiveTenantMember(int tenantId, String userId) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_tenant membership
                INNER JOIN sys_user u ON u.id = membership.user_id
                WHERE membership.user_id = ?
                  AND membership.tenant_id = ?
                  AND membership.status = '1'
                  AND u.status = 1
                  AND u.del_flag = 0
                """ + EXCLUDE_PLATFORM_ADMINS, Integer.class, userId, tenantId);
        if (count == null || count < 1) {
            throw InsuranceApiException.notFound("成员不属于当前机构或已停用");
        }
    }

    private void validateNewPassword(String username, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw InsuranceApiException.badRequest("两次输入的新密码不一致");
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH || newPassword.length() > MAX_PASSWORD_LENGTH) {
            throw InsuranceApiException.badRequest("新密码长度必须为 8-72 位");
        }
        if (DEFAULT_PASSWORD.equals(newPassword)) {
            throw InsuranceApiException.badRequest("新密码不能使用系统默认密码");
        }
        if (username != null && username.equalsIgnoreCase(newPassword)) {
            throw InsuranceApiException.badRequest("新密码不能与账号相同");
        }
    }

    private String requiredPassword(String value, String field) {
        if (value == null || value.isBlank() || value.length() > MAX_PASSWORD_LENGTH) {
            throw InsuranceApiException.badRequest(field + "不能为空且长度不能超过 72 位");
        }
        return value;
    }

    private void markChanged(String userId, LocalDateTime now) {
        upsertState(userId, false, "self_change", null, null, now, null, now);
    }

    private void markReset(String userId, int tenantId, String operatorId, LocalDateTime now) {
        upsertState(userId, true, "admin_reset", tenantId, operatorId, null, now, now);
    }

    private void upsertState(String userId, boolean mustChange, String reason, Integer resetTenantId,
                             String resetBy, LocalDateTime changedAt, LocalDateTime resetAt, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO rehealth_user_password_state
                    (user_id, must_change_password, reason, reset_tenant_id, reset_by,
                     reset_at, changed_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    must_change_password = VALUES(must_change_password),
                    reason = VALUES(reason), reset_tenant_id = VALUES(reset_tenant_id),
                    reset_by = VALUES(reset_by), reset_at = VALUES(reset_at),
                    changed_at = VALUES(changed_at), updated_at = VALUES(updated_at)
                """, userId, mustChange ? 1 : 0, reason, resetTenantId, resetBy,
                resetAt, changedAt, now, now);
    }

    static record Credential(String username, String password, String salt) {
    }
}
