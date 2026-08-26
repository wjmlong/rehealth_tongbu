package org.jeecg.modules.rehealth.insurance;

import org.jeecg.common.system.vo.LoginUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceTenantAccessGuard {
    private final JdbcTemplate jdbc;

    public InsuranceTenantAccessGuard(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    public int requireTenant(LoginUser user, String requestedTenant) {
        int tenantId = parseTenant(requestedTenant);
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw InsuranceApiException.forbidden("authenticated service account is required");
        }
        boolean assigned = user.getRelTenantIds() != null
                && Arrays.stream(user.getRelTenantIds().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .anyMatch(Integer.toString(tenantId)::equals);
        if (!assigned) {
            throw InsuranceApiException.forbidden("requested tenant is not assigned to the authenticated service account");
        }
        try {
            Integer activeMembership = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM sys_user_tenant membership
                    INNER JOIN sys_user account
                        ON account.id = CONVERT(membership.user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    INNER JOIN sys_tenant tenant ON tenant.id = membership.tenant_id
                    WHERE membership.user_id = ?
                      AND membership.tenant_id = ?
                      AND membership.status = '1'
                      AND account.status = 1
                      AND account.del_flag = 0
                      AND tenant.status = 1
                      AND tenant.del_flag = 0
                    """, Integer.class, user.getId(), tenantId);
            if (activeMembership == null || activeMembership < 1) {
                throw InsuranceApiException.forbidden(
                        "requested tenant membership, account, or tenant is no longer active"
                );
            }
        } catch (DataAccessException e) {
            throw InsuranceApiException.serviceUnavailable("tenant authorization data is temporarily unavailable");
        }
        return tenantId;
    }

    /** Returns the logged-in user id when the user is a department manager. */
    public String managerScope(LoginUser user, int tenantId) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            return null;
        }
        Integer manager = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND ur.tenant_id = ?
                  AND r.role_code = 'insurance_department_manager'
                """, Integer.class, user.getId(), tenantId);
        return manager != null && manager > 0 ? user.getId() : null;
    }

    /** Returns the current staff id for assigned-user risk reads across all insurer WEB roles. */
    public String responsibilityScope(LoginUser user, int tenantId) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw InsuranceApiException.forbidden("authenticated insurance staff account is required");
        }
        Integer responsibleRole = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND ur.tenant_id = ?
                  AND r.role_code IN (
                      'insurance_org_admin',
                      'insurance_department_manager',
                      'insurer_analyst',
                      'insurance_operator',
                      'insurer_viewer',
                      'insurer_auditor'
                  )
                """, Integer.class, user.getId(), tenantId);
        if (responsibleRole == null || responsibleRole <= 0) {
            throw InsuranceApiException.forbidden("current account has no insurance responsibility role in the requested tenant");
        }
        return user.getId();
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增三级数据范围解析-----------
    /**
     * Resolves the three-level assignment data scope for the current staff.
     *
     * <p>Organization administrators and auditors receive an unrestricted
     * (null) scope; department managers receive a TEAM scope covering every
     * employee in their departments; every other responsibility role receives
     * a SELF scope. Accounts without an insurance responsibility role are
     * rejected.
     */
    public InsuranceAssignmentScope assignmentScope(LoginUser user, int tenantId) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            throw InsuranceApiException.forbidden("authenticated insurance staff account is required");
        }
        Integer orgWide = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND ur.tenant_id = ?
                  AND r.role_code IN ('insurance_org_admin', 'insurer_auditor')
                """, Integer.class, user.getId(), tenantId);
        if (orgWide != null && orgWide > 0) {
            return null;
        }
        Integer manager = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND ur.tenant_id = ?
                  AND r.role_code = 'insurance_department_manager'
                """, Integer.class, user.getId(), tenantId);
        Integer responsible = responsibleRoleCount(user.getId(), tenantId);
        if (responsible == null || responsible <= 0) {
            throw InsuranceApiException.forbidden("current account has no insurance responsibility role in the requested tenant");
        }
        return new InsuranceAssignmentScope(
                user.getId(),
                manager != null && manager > 0 ? InsuranceAssignmentScope.MODE_TEAM : InsuranceAssignmentScope.MODE_SELF
        );
    }

    //update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】导入侧数据范围：无保险责任角色的服务账号不受限-----------
    /**
     * Resolves the assignment data scope for policy import/dispatch callers.
     *
     * <p>Accounts holding an insurance responsibility role receive the same
     * three-level scope as {@link #assignmentScope(LoginUser, int)}; accounts
     * without one (core-system service accounts and platform admins) receive
     * {@code null}, i.e. unrestricted tenant-level imports, so existing
     * server-to-server integrations keep working.
     */
    public InsuranceAssignmentScope assignmentScopeOrNull(LoginUser user, int tenantId) {
        if (user == null || user.getId() == null || user.getId().isBlank()) {
            return null;
        }
        Integer responsible = responsibleRoleCount(user.getId(), tenantId);
        if (responsible == null || responsible <= 0) {
            return null;
        }
        return assignmentScope(user, tenantId);
    }

    private Integer responsibleRoleCount(String userId, int tenantId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_role ur
                JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? AND ur.tenant_id = ?
                  AND r.role_code IN (
                      'insurance_org_admin',
                      'insurance_department_manager',
                      'insurer_analyst',
                      'insurance_operator',
                      'insurer_viewer',
                      'insurer_auditor'
                  )
                """, Integer.class, userId, tenantId);
    }
    //update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】导入侧数据范围：无保险责任角色的服务账号不受限-----------
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】新增三级数据范围解析-----------

    private int parseTenant(String requestedTenant) {
        if (requestedTenant == null || requestedTenant.isBlank()) {
            throw InsuranceApiException.badRequest("X-Tenant-Id is required");
        }
        try {
            int tenantId = Integer.parseInt(requestedTenant.trim());
            if (tenantId <= 0) {
                throw InsuranceApiException.badRequest("X-Tenant-Id must be a positive integer");
            }
            return tenantId;
        } catch (NumberFormatException e) {
            throw InsuranceApiException.badRequest("X-Tenant-Id must be a positive integer");
        }
    }
}
