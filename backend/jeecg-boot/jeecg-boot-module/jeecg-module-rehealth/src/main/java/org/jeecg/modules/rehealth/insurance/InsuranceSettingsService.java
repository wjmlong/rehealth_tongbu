package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.oConvertUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.Assignment;
import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.AssignmentRequest;
import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.Department;
import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.Member;
import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.MemberCreation;
import static org.jeecg.modules.rehealth.insurance.InsuranceSettingsResponse.MemberInvitation;

@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceSettingsService {
    private static final Pattern SUBJECT_REF = Pattern.compile("(?i)[0-9a-f]{64}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._@-]{2,99}");
    private static final List<String> INSURANCE_MEMBER_ROLES = List.of(
            "insurer_viewer", "insurer_analyst", "insurance_operator", "insurer_auditor",
            "insurance_department_manager"
    );
    private static final char[] TEMP_PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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

    public InsuranceSettingsService(@Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public InsuranceSettingsResponse.Organization organization(int tenantId) {
        try {
            return jdbc.queryForObject("""
                    SELECT id, organization_name, license_no, insurance_type, compliance_email,
                           regulatory_email, data_retention_years, mask_sensitive_data,
                           access_log_enabled, version
                    FROM rehealth_insurance_tenant_profile
                    WHERE tenant_id = ?
                    """, (rs, row) -> new InsuranceSettingsResponse.Organization(
                    rs.getString("id"), tenantId, rs.getString("organization_name"),
                    rs.getString("license_no"), rs.getString("insurance_type"),
                    rs.getString("compliance_email"), rs.getString("regulatory_email"),
                    rs.getInt("data_retention_years"), rs.getBoolean("mask_sensitive_data"),
                    rs.getBoolean("access_log_enabled"), rs.getInt("version")
            ), tenantId);
        } catch (EmptyResultDataAccessException ignored) {
            String tenantName = jdbc.queryForObject("SELECT name FROM sys_tenant WHERE id = ?", String.class, tenantId);
            return new InsuranceSettingsResponse.Organization("tenant-" + tenantId, tenantId, tenantName == null ? "" : tenantName,
                    null, "health_insurance", null, null, 7, true, true, 0);
        }
    }

    @Transactional
    public InsuranceSettingsResponse.Organization updateOrganization(int tenantId, String operatorId, InsuranceSettingsRequest.Organization request) {
        String name = required(request.name(), "organization name");
        int retention = request.dataRetentionYears() == null ? 7 : request.dataRetentionYears();
        if (retention < 1 || retention > 30) {
            throw InsuranceApiException.badRequest("dataRetentionYears must be between 1 and 30");
        }
        String id = "tenant-" + tenantId;
        jdbc.update("""
                INSERT INTO rehealth_insurance_tenant_profile
                    (id, tenant_id, organization_name, license_no, insurance_type, compliance_email,
                     regulatory_email, data_retention_years, mask_sensitive_data, access_log_enabled,
                     version, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?)
                ON DUPLICATE KEY UPDATE
                    organization_name = VALUES(organization_name), license_no = VALUES(license_no),
                    insurance_type = VALUES(insurance_type), compliance_email = VALUES(compliance_email),
                    regulatory_email = VALUES(regulatory_email), data_retention_years = VALUES(data_retention_years),
                    mask_sensitive_data = VALUES(mask_sensitive_data), access_log_enabled = VALUES(access_log_enabled),
                    version = version + 1, updated_at = VALUES(updated_at)
                """, id, tenantId, name, request.licenseNo(), request.insuranceType(), request.complianceEmail(),
                request.regulatoryEmail(), retention, Boolean.TRUE.equals(request.maskSensitiveData()),
                request.accessLogEnabled() == null || request.accessLogEnabled(), LocalDateTime.now(), LocalDateTime.now());
        return organization(tenantId);
    }

    public List<Department> departments(int tenantId) {
        return departments(tenantId, null);
    }

    /** Lists all departments for administrators, or assigned departments for a manager. */
    public List<Department> departments(int tenantId, String managerUserId) {
        return jdbc.query("""
                SELECT d.id, d.depart_name, d.parent_id,
                       (SELECT COUNT(*)
                        FROM sys_user_depart ud
                        INNER JOIN sys_user department_user ON department_user.id = ud.user_id
                        WHERE ud.dep_id = d.id
                          AND NOT EXISTS (
                              SELECT 1
                              FROM sys_user_role platform_user_role
                              INNER JOIN sys_role platform_role ON platform_role.id = platform_user_role.role_id
                              WHERE platform_user_role.user_id = department_user.id
                                AND platform_role.role_code IN ('admin', 'super_admin')
                          )) AS member_count
                FROM sys_depart d
                WHERE d.tenant_id = ? AND COALESCE(d.del_flag, '0') = '0' AND COALESCE(d.status, '1') = '1'
                  AND (? IS NULL OR EXISTS (
                      SELECT 1
                      FROM rehealth_insurance_subject_manager scope
                      WHERE scope.tenant_id = d.tenant_id
                        AND scope.department_id = d.id
                        AND scope.manager_user_id = ?
                        AND scope.status = 'active'
                  ))
                ORDER BY d.depart_order, d.depart_name
                """, (rs, row) -> new Department(rs.getString("id"), rs.getString("depart_name"),
                rs.getString("parent_id"), rs.getInt("member_count")), tenantId, managerUserId, managerUserId);
    }

    public List<Member> members(int tenantId) {
        return members(tenantId, null);
    }

    /**
     * Lists tenant business members for administrators, or only the insured
     * subjects assigned to a department manager. Platform administrators are
     * never exposed to tenant settings. The scope is enforced in SQL so direct
     * Java API calls cannot bypass the website permissions.
     */
    public List<Member> members(int tenantId, String managerUserId) {
        return jdbc.query("""
                SELECT u.id, u.username, u.realname, u.email, u.phone,
                       CASE
                           WHEN ut.status = '5' THEN 'invited'
                           WHEN ut.status = '3' THEN 'pending'
                           WHEN u.status = 1 AND u.del_flag = 0 AND ut.status = '1' THEN 'active'
                           ELSE 'disabled'
                       END AS member_status,
                       GROUP_CONCAT(DISTINCT d.id ORDER BY d.depart_name SEPARATOR ',') AS department_ids,
                       GROUP_CONCAT(DISTINCT d.depart_name ORDER BY d.depart_name SEPARATOR ', ') AS departments,
                       GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_name SEPARATOR ',') AS role_codes,
                       GROUP_CONCAT(DISTINCT r.role_name ORDER BY r.role_name SEPARATOR ', ') AS roles,
                       (SELECT COUNT(*) FROM rehealth_insurance_subject_manager sm
                        WHERE sm.tenant_id = ut.tenant_id AND sm.manager_user_id = u.id AND sm.status = 'active') AS assignment_count
                FROM sys_user_tenant ut
                JOIN sys_user u ON u.id = ut.user_id
                LEFT JOIN sys_user_depart ud ON ud.user_id = u.id
                LEFT JOIN sys_depart d ON d.id = ud.dep_id AND d.tenant_id = ut.tenant_id
                LEFT JOIN sys_user_role ur ON ur.user_id = u.id AND ur.tenant_id = ut.tenant_id
                LEFT JOIN sys_role r ON r.id = ur.role_id
                 WHERE ut.tenant_id = ?
                """ + EXCLUDE_PLATFORM_ADMINS + """
                   AND (? IS NULL OR EXISTS (
                       SELECT 1
                       FROM rehealth_insurance_subject_manager scope
                       WHERE scope.tenant_id = ut.tenant_id
                         AND scope.manager_user_id = ?
                         AND scope.subject_ref = LOWER(SHA2(CONCAT(ut.tenant_id, ':', ut.user_id), 256))
                         AND scope.status = 'active'
                   ))
                 GROUP BY u.id, u.username, u.realname, u.email, u.phone, u.status, u.del_flag, ut.status, ut.tenant_id
                 ORDER BY u.realname, u.username
                 """, (rs, row) -> new Member(rs.getString("id"), rs.getString("username"),
                 rs.getString("realname"), rs.getString("email"), rs.getString("phone"),
                 rs.getString("member_status"), rs.getString("department_ids"), rs.getString("departments"),
                 rs.getString("role_codes"), rs.getString("roles"),
                 rs.getInt("assignment_count")), tenantId, managerUserId, managerUserId);
    }

    public List<Assignment> assignments(int tenantId) {
        return assignments(tenantId, null);
    }

    /** Lists all assignments for administrators, or only the manager's own. */
    public List<Assignment> assignments(int tenantId, String managerUserId) {
        return jdbc.query("""
                SELECT sm.subject_ref, sm.manager_user_id, u.realname AS manager_name,
                       sm.department_id, d.depart_name, sm.status
                FROM rehealth_insurance_subject_manager sm
                JOIN sys_user u ON u.id = sm.manager_user_id
                LEFT JOIN sys_depart d ON d.id = sm.department_id
                 WHERE sm.tenant_id = ?
                   AND (? IS NULL OR sm.manager_user_id = ?)
                 ORDER BY d.depart_name, u.realname, sm.subject_ref
                 """, (rs, row) -> new Assignment(rs.getString("subject_ref"), rs.getString("manager_user_id"),
                 rs.getString("manager_name"), rs.getString("department_id"), rs.getString("depart_name"),
                 rs.getString("status")), tenantId, managerUserId, managerUserId);
    }

    @Transactional
    public Member updateMemberStatus(int tenantId, String operatorId, String userId, String status) {
        requireMember(tenantId, userId);
        if (!"active".equals(status) && !"disabled".equals(status)) {
            throw InsuranceApiException.badRequest("status must be active or disabled");
        }
        if (userId.equals(operatorId) && "disabled".equals(status)) {
            throw InsuranceApiException.badRequest("the current operator cannot disable their own tenant membership");
        }
        String currentStatus = jdbc.queryForObject(
                "SELECT status FROM sys_user_tenant WHERE user_id = ? AND tenant_id = ?",
                String.class, userId, tenantId);
        if (("5".equals(currentStatus) || "3".equals(currentStatus)) && "active".equals(status)) {
            throw InsuranceApiException.badRequest("a pending invitation must be accepted by the invited member");
        }
        // Membership status is tenant-scoped; do not disable the global user
        // account because the same Jeecg user may belong to another tenant.
        jdbc.update("UPDATE sys_user_tenant SET status = ?, update_time = ?, update_by = ? WHERE user_id = ? AND tenant_id = ?", "active".equals(status) ? "1" : "0", LocalDateTime.now(), operatorId, userId, tenantId);
        return members(tenantId).stream().filter(member -> member.id().equals(userId)).findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("member was not found"));
    }

    @Transactional
    public MemberInvitation inviteMember(int tenantId, String operatorId, String phone, String departmentId) {
        return inviteMember(tenantId, operatorId, phone, departmentId, null);
    }

    @Transactional
    public MemberInvitation inviteMember(int tenantId, String operatorId, String phone, String departmentId, String roleCode) {
        String normalizedPhone = required(phone, "phone");
        if (normalizedPhone.length() > 32) {
            throw InsuranceApiException.badRequest("phone must be at most 32 characters");
        }
        String userId;
        try {
            userId = jdbc.queryForObject("""
                    SELECT u.id
                    FROM sys_user u
                    WHERE u.phone = ? AND u.del_flag = 0
                    """ + EXCLUDE_PLATFORM_ADMINS, String.class, normalizedPhone);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("no registered Jeecg account matches the phone number");
        }
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user_tenant WHERE user_id = ? AND tenant_id = ?", Integer.class, userId, tenantId);
        if (existing != null && existing > 0) {
            throw InsuranceApiException.conflict("the account already belongs to or has a pending invitation for this tenant");
        }
        String normalizedDepartmentId = departmentId == null ? "" : departmentId.trim();
        if (!normalizedDepartmentId.isEmpty()) {
            Integer validDepartment = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM sys_depart WHERE id = ? AND tenant_id = ? AND COALESCE(del_flag, '0') = '0'",
                    Integer.class, normalizedDepartmentId, tenantId);
            if (validDepartment == null || validDepartment < 1) {
                throw InsuranceApiException.badRequest("departmentId is not in the requested tenant");
            }
        }
        jdbc.update("INSERT INTO sys_user_tenant (id, user_id, tenant_id, status, create_by, create_time) VALUES (?, ?, ?, '5', ?, ?)",
                UUID.randomUUID().toString().replace("-", ""), userId, tenantId, operatorId, LocalDateTime.now());
        if (!normalizedDepartmentId.isEmpty()) {
            jdbc.update("INSERT INTO sys_user_depart (id, user_id, dep_id) VALUES (?, ?, ?)",
                    UUID.randomUUID().toString().replace("-", ""), userId, normalizedDepartmentId);
        }
        if (roleCode != null && !roleCode.isBlank()) {
            String roleId = resolveInsuranceMemberRole(tenantId, roleCode);
            jdbc.update("INSERT INTO sys_user_role (id, user_id, role_id, tenant_id) VALUES (?, ?, ?, ?)",
                    UUID.randomUUID().toString().replace("-", ""), userId, roleId, tenantId);
        }
        return new MemberInvitation(userId, "invited", "邀请已发送，成员同意后方可进入保险机构工作台");
    }

    /**
     * Creates a new global Jeecg account and binds it to this tenant in one
     * transaction. The account is intentionally not added to any other
     * tenant, and the generated password is returned only in this response.
     */
    @Transactional
    public MemberCreation createMember(int tenantId, String operatorId, InsuranceSettingsRequest.MemberCreation request) {
        String realName = required(request.realName(), "realName");
        String phone = required(request.phone(), "phone");
        if (phone.length() > 45) {
            throw InsuranceApiException.badRequest("phone must be at most 45 characters");
        }
        String username = request.username() == null || request.username().isBlank()
                ? phone : required(request.username(), "username");
        if (!USERNAME.matcher(username).matches()) {
            throw InsuranceApiException.badRequest("username must be 3-100 characters using letters, numbers, '.', '_' '@' or '-'");
        }
        String email = request.email() == null || request.email().isBlank()
                ? null : required(request.email(), "email");
        if (email != null && email.length() > 45) {
            throw InsuranceApiException.badRequest("email must be at most 45 characters");
        }
        String departmentId = required(request.departmentId(), "departmentId");
        Integer validDepartment = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sys_depart WHERE id = ? AND tenant_id = ? AND COALESCE(del_flag, '0') = '0' AND COALESCE(status, '1') = '1'",
                Integer.class, departmentId, tenantId);
        if (validDepartment == null || validDepartment < 1) {
            throw InsuranceApiException.badRequest("departmentId is not in the requested tenant");
        }
        String roleId = resolveInsuranceMemberRole(tenantId, request.roleCode());
        ensureUniqueAccount("username", username);
        ensureUniqueAccount("phone", phone);
        if (email != null) {
            ensureUniqueAccount("email", email);
        }

        String userId = UUID.randomUUID().toString().replace("-", "");
        String salt = oConvertUtils.randomGen(8);
        String temporaryPassword = generateTemporaryPassword();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO sys_user
                    (id, username, realname, password, salt, email, phone, status, del_flag,
                     create_by, create_time, update_by, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, ?, ?, ?, ?)
                """, userId, username, realName, PasswordUtil.encrypt(username, temporaryPassword, salt), salt,
                email, phone, operatorId, now, operatorId, now);
        jdbc.update("INSERT INTO sys_user_tenant (id, user_id, tenant_id, status, create_by, create_time) VALUES (?, ?, ?, '1', ?, ?)",
                UUID.randomUUID().toString().replace("-", ""), userId, tenantId, operatorId, now);
        jdbc.update("INSERT INTO sys_user_depart (id, user_id, dep_id) VALUES (?, ?, ?)",
                UUID.randomUUID().toString().replace("-", ""), userId, departmentId);
        jdbc.update("INSERT INTO sys_user_role (id, user_id, role_id, tenant_id) VALUES (?, ?, ?, ?)",
                UUID.randomUUID().toString().replace("-", ""), userId, roleId, tenantId);
        return new MemberCreation(userId, username, temporaryPassword, true,
                "成员已创建，请将临时密码安全交给成员并要求首次登录后修改");
    }

    @Transactional
    public Member updateMemberDepartment(int tenantId, String userId, String departmentId) {
        requireMember(tenantId, userId);
        Integer valid = jdbc.queryForObject("SELECT COUNT(*) FROM sys_depart WHERE id = ? AND tenant_id = ? AND COALESCE(del_flag, '0') = '0'", Integer.class, departmentId, tenantId);
        if (valid == null || valid < 1) {
            throw InsuranceApiException.badRequest("departmentId is not in the requested tenant");
        }
        // A Jeecg account may belong to more than one insurer. Remove only
        // department memberships owned by the insurer being administered.
        jdbc.update("""
                DELETE membership
                FROM sys_user_depart membership
                JOIN sys_depart department ON department.id = membership.dep_id
                WHERE membership.user_id = ? AND department.tenant_id = ?
                """, userId, tenantId);
        jdbc.update("INSERT INTO sys_user_depart (ID, user_id, dep_id) VALUES (?, ?, ?)", UUID.randomUUID().toString().replace("-", ""), userId, departmentId);
        return members(tenantId).stream().filter(member -> member.id().equals(userId)).findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("member was not found"));
    }

    @Transactional
    public Member updateMemberRole(int tenantId, String userId, String roleCode) {
        requireMember(tenantId, userId);
        String roleId = resolveInsuranceMemberRole(tenantId, roleCode);
        jdbc.update("DELETE ur FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = ? AND ur.tenant_id = ? AND r.role_code IN ('insurer_viewer','insurer_analyst','insurance_operator','insurer_auditor','insurance_department_manager')", userId, tenantId);
        jdbc.update("INSERT INTO sys_user_role (id, user_id, role_id, tenant_id) VALUES (?, ?, ?, ?)", UUID.randomUUID().toString().replace("-", ""), userId, roleId, tenantId);
        return members(tenantId).stream().filter(member -> member.id().equals(userId)).findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound("member was not found"));
    }

    private void requireMember(int tenantId, String userId) {
        Integer valid = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user_tenant membership
                INNER JOIN sys_user u ON u.id = membership.user_id
                WHERE membership.user_id = ? AND membership.tenant_id = ?
                """ + EXCLUDE_PLATFORM_ADMINS, Integer.class, userId, tenantId);
        if (valid == null || valid < 1) {
            throw InsuranceApiException.notFound("member was not found in the requested tenant");
        }
    }

    private String resolveInsuranceMemberRole(int tenantId, String roleCode) {
        if (roleCode == null || !INSURANCE_MEMBER_ROLES.contains(roleCode.trim())) {
            throw InsuranceApiException.badRequest("roleCode is not an insurer role");
        }
        try {
            String roleId = jdbc.queryForObject("""
                    SELECT id
                    FROM sys_role
                    WHERE role_code = ? AND (tenant_id = 0 OR tenant_id = ?)
                    ORDER BY CASE WHEN tenant_id = ? THEN 0 ELSE 1 END
                    LIMIT 1
                    """, String.class, roleCode.trim(), tenantId, tenantId);
            if (roleId == null) {
                throw InsuranceApiException.badRequest("roleCode is not configured");
            }
            return roleId;
        } catch (EmptyResultDataAccessException e) {
            throw InsuranceApiException.badRequest("roleCode is not configured");
        }
    }

    private void ensureUniqueAccount(String field, String value) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE " + field + " = ?", Integer.class, value);
        if (count != null && count > 0) {
            throw InsuranceApiException.conflict("the account " + field + " is already registered; use the existing-account invitation flow");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            password.append(TEMP_PASSWORD_ALPHABET[SECURE_RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length)]);
        }
        return password.toString();
    }

    @Transactional
    public Assignment upsertAssignment(int tenantId, String operatorId, String subjectRef, AssignmentRequest request) {
        if (subjectRef == null || !SUBJECT_REF.matcher(subjectRef.trim()).matches()) {
            throw InsuranceApiException.badRequest("subjectRef must be a 64-character pseudonymous reference");
        }
        String managerId = required(request.managerUserId(), "managerUserId");
        String departmentId = required(request.departmentId(), "departmentId");
        Integer valid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sys_user_tenant ut
                JOIN sys_user_depart ud ON ud.user_id = ut.user_id AND ud.dep_id = ?
                JOIN sys_depart d ON d.id = ud.dep_id AND d.tenant_id = ut.tenant_id
                JOIN sys_user u ON u.id = ut.user_id
                WHERE ut.tenant_id = ? AND ut.user_id = ? AND ut.status = '1' AND u.status = 1 AND u.del_flag = 0
                """ + EXCLUDE_PLATFORM_ADMINS, Integer.class, departmentId, tenantId, managerId);
        if (valid == null || valid < 1) {
            throw InsuranceApiException.badRequest("managerUserId is not an active member of departmentId");
        }
        String status = request.status() == null || request.status().isBlank() ? "active" : request.status().trim();
        jdbc.update("""
                INSERT INTO rehealth_insurance_subject_manager
                    (id, tenant_id, manager_user_id, department_id, subject_ref, status, source_system, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'rehealth_settings', ?, ?)
                ON DUPLICATE KEY UPDATE department_id = VALUES(department_id), status = VALUES(status), updated_at = VALUES(updated_at)
                """, UUID.randomUUID().toString().replace("-", ""), tenantId, managerId, departmentId,
                subjectRef.trim().toLowerCase(), status, LocalDateTime.now(), LocalDateTime.now());
        return assignments(tenantId).stream().filter(item -> item.subjectRef().equalsIgnoreCase(subjectRef.trim())).findFirst()
                .orElseThrow(() -> InsuranceApiException.serviceUnavailable("assignment was not persisted"));
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank() || value.trim().length() > 200) {
            throw InsuranceApiException.badRequest(field + " is required and must be at most 200 characters");
        }
        return value.trim();
    }
}
