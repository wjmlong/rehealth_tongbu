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
