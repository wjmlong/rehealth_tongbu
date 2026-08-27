package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Insurance-side basic policy library: the tenant policy list (pure policy
 * information plus the number of linked users) and linking policies to APP
 * users ("add a policy to a user"). One policy may link to many users; the
 * APP binding and consent steps stay on the APP side.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】保单列表与添加保单关联-----------
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsurancePolicyService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CANDIDATES = 200;

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
    private final InsuranceDispatchAccess dispatchAccess;

    public InsurancePolicyService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc,
            InsuranceDispatchAccess dispatchAccess
    ) {
        this.jdbc = jdbc;
        this.dispatchAccess = dispatchAccess;
    }

    /**
     * Basic policy library list: pure policy information (no insured-person
     * fields) plus the number of linked users. Every importer sees the whole
     * tenant library; user-level visibility lives in the link table.
     */
    public InsurancePolicyResponse.PolicyPage list(
            int tenantId, int pageNo, int pageSize, String keyword
    ) {
        int normalizedPageNo = Math.max(1, pageNo);
        int normalizedPageSize = Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 100) {
            throw InsuranceApiException.badRequest("keyword must not exceed 100 characters");
        }
        boolean hasKeyword = !normalizedKeyword.isEmpty();

        StringBuilder where = new StringBuilder("p.tenant_id=?");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (hasKeyword) {
            where.append(" AND (LOWER(COALESCE(p.policy_no,'')) LIKE ?"
                    + " OR LOWER(COALESCE(p.product_name,'')) LIKE ?)");
            String like = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
        }

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rehealth_insurance_policy p WHERE " + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(normalizedPageSize);
        pageArgs.add((normalizedPageNo - 1) * normalizedPageSize);
        List<InsurancePolicyResponse.Item> items = jdbc.query(
                "SELECT p.policy_no, p.product_name, p.policy_type, p.default_plan_id,"
                        + " cat.name AS plan_name, p.status, p.effective_on, p.expires_on, p.coverage_amount, p.premium_amount,"
                        + " (SELECT COUNT(*) FROM rehealth_insurance_policy_link link"
                        + "   WHERE link.tenant_id=p.tenant_id AND link.policy_no=p.policy_no AND link.status='assigned') AS link_count"
                        + " FROM rehealth_insurance_policy p"
                        + " LEFT JOIN rehealth_insurance_plan_catalog cat"
                        + "   ON cat.tenant_id=p.tenant_id AND cat.plan_id=p.default_plan_id AND cat.status='active'"
                        + " WHERE " + where
                        + " ORDER BY p.effective_on DESC, p.created_at DESC"
                        + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new InsurancePolicyResponse.Item(
                        rs.getString("policy_no"),
                        rs.getString("product_name"),
                        rs.getString("policy_type"),
                        rs.getString("default_plan_id"),
                        rs.getString("plan_name"),
                        rs.getString("status"),
                        toLocalDate(rs.getDate("effective_on")),
                        toLocalDate(rs.getDate("expires_on")),
                        rs.getBigDecimal("coverage_amount"),
                        rs.getBigDecimal("premium_amount"),
                        rs.getLong("link_count")
                ), pageArgs.toArray());
        return new InsurancePolicyResponse.PolicyPage(total == null ? 0 : total, items);
    }

    public List<InsurancePolicyResponse.DispatchableSubject> dispatchableSubjects(
            int tenantId, InsuranceAssignmentScope scope, String keyword
    ) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 100) {
            throw InsuranceApiException.badRequest("keyword must not exceed 100 characters");
        }
        boolean hasKeyword = !normalizedKeyword.isEmpty();
        StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.subject_ref,
                       COALESCE(profile.name, u.realname, '未命名用户') AS user_name,
                       a.employee_id, emp.realname AS employee_name
                FROM rehealth_insurance_enrollment e
                LEFT JOIN sys_user u ON u.id = CONVERT(e.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                LEFT JOIN rehealth_patient_profile profile ON profile.user_id = e.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                LEFT JOIN rehealth_insurance_user_assignment a
                  ON a.enrollment_id = e.id AND a.status = 'active' AND a.role_type = 'PRIMARY'
                LEFT JOIN sys_user emp ON emp.id = CONVERT(a.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE e.tenant_id=? AND e.enrollment_status='active'
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (scope != null) {
            sql.append("""
                      AND a.id IS NOT NULL
                      AND (a.employee_id=?
                           OR (?='TEAM' AND EXISTS (
                               SELECT 1 FROM sys_user_depart my_dept
                               JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id=my_dept.dep_id
                               WHERE my_dept.user_id=?
                                 AND assignee_dept.user_id = CONVERT(a.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                           )))
                    """);
            args.add(scope.userId());
            args.add(scope.mode());
            args.add(scope.userId());
        }
        if (hasKeyword) {
            sql.append(" AND LOWER(COALESCE(profile.name,'')) LIKE ?");
            args.add("%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%");
        }
        sql.append(" ORDER BY user_name ASC LIMIT ").append(MAX_CANDIDATES);
        return jdbc.query(sql.toString(), (rs, rowNum) -> new InsurancePolicyResponse.DispatchableSubject(
                rs.getString("id"),
                rs.getString("subject_ref"),
                rs.getString("user_name"),
                rs.getString("employee_name")
        ), args.toArray());
    }

    /**
     * Links a basic policy to an APP user ("add a policy to a user"). The
     * user is resolved by phone or enrollment id, must be enrolled in the
     * tenant and inside the caller's responsibility scope. One policy may be
     * linked to many users; linking the same policy to the same user again is
     * idempotent.
     */
    public InsurancePolicyResponse.LinkResult link(
            int tenantId, InsuranceAssignmentScope scope, String actorUserId,
            InsurancePolicyResponse.LinkRequest request
    ) {
        String policyNo = required(request.policyNo(), "policyNo", 128);
        String phone = trim(request.phone(), 45);
        String enrollmentId = trim(request.enrollmentId(), 64);
        if ((phone == null) == (enrollmentId == null)) {
            throw InsuranceApiException.badRequest("phone 与 enrollmentId 必须且只能提供一个");
        }

        PolicyRow policy = policyRow(tenantId, policyNo);
        if (policy == null) {
            throw InsuranceApiException.notFound("该保单号不存在于当前机构");
        }
        if (!"active".equalsIgnoreCase(policy.status())) {
            throw InsuranceApiException.badRequest("该保单非生效状态，无法添加给用户");
        }
        SubjectRef subject = enrollmentId != null
                ? subjectByEnrollment(tenantId, enrollmentId)
                : subjectByPhone(tenantId, userIdByPhone(phone));
        dispatchAccess.requireDispatchable(tenantId, scope, subject.subjectRef());

        LocalDateTime now = LocalDateTime.now();
        InsurancePolicyLinkRow existing = linkRow(tenantId, policyNo, subject.subjectRef());
        if (existing == null) {
            jdbc.update("""
                    INSERT INTO rehealth_insurance_policy_link
                        (id, tenant_id, policy_no, subject_ref, status, source_system, created_by, created_at, updated_at)
                    VALUES (?, ?, ?, ?, 'assigned', 'rehealth_website', ?, ?, ?)
                    """, uuid(), tenantId, policyNo, subject.subjectRef(), actorUserId, now, now);
        } else if (!"assigned".equals(existing.status())) {
            jdbc.update("""
                    UPDATE rehealth_insurance_policy_link
                    SET status = 'assigned', updated_at = ?
                    WHERE tenant_id = ? AND policy_no = ? AND subject_ref = ?
                    """, now, tenantId, policyNo, subject.subjectRef());
        }
        return new InsurancePolicyResponse.LinkResult(
                policyNo, subject.subjectRef(), subject.userName(), now);
    }

    //update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单取消关联】取消保单与 App 用户的关联-----------
    /**
     * Cancels the link between a basic policy and an APP user. The link row
     * is soft-cancelled ({@code status='removed'}, history kept). Any active
     * plan binding the user holds on this policy is terminated
     * ({@code status='unbound'}); when no other active binding remains for
     * the subject in the tenant, the subject consent falls back to pending
     * so the user leaves the insurer workbench queue.
     */
    @org.springframework.transaction.annotation.Transactional
    public InsurancePolicyResponse.UnlinkResult unlink(
            int tenantId, InsuranceAssignmentScope scope,
            InsurancePolicyResponse.UnlinkRequest request
    ) {
        String policyNo = required(request.policyNo(), "policyNo", 128);
        String phone = trim(request.phone(), 45);
        String enrollmentId = trim(request.enrollmentId(), 64);
        String subjectRef = trim(request.subjectRef(), 64);
        int provided = (phone == null ? 0 : 1) + (enrollmentId == null ? 0 : 1) + (subjectRef == null ? 0 : 1);
        if (provided != 1) {
            throw InsuranceApiException.badRequest("phone、enrollmentId、subjectRef 必须且只能提供一个");
        }
        if (policyRow(tenantId, policyNo) == null) {
            throw InsuranceApiException.notFound("该保单号不存在于当前机构");
        }
        SubjectRef subject = subjectRef != null
                ? subjectByRef(tenantId, subjectRef)
                : enrollmentId != null
                        ? subjectByEnrollment(tenantId, enrollmentId)
                        : subjectByPhone(tenantId, userIdByPhone(phone));
        dispatchAccess.requireDispatchable(tenantId, scope, subject.subjectRef());

        InsurancePolicyLinkRow existing = linkRow(tenantId, policyNo, subject.subjectRef());
        if (existing == null) {
            throw InsuranceApiException.notFound("该保单尚未添加给该用户");
        }
        LocalDateTime now = LocalDateTime.now();
        boolean bindingCancelled = false;
        if (!"removed".equals(existing.status())) {
            jdbc.update("""
                    UPDATE rehealth_insurance_policy_link
                    SET status = 'removed', updated_at = ?
                    WHERE tenant_id = ? AND policy_no = ? AND subject_ref = ?
                    """, now, tenantId, policyNo, subject.subjectRef());
            String policyId = policyId(tenantId, policyNo);
            int terminated = jdbc.update("""
                    UPDATE rehealth_insurance_plan_binding
                    SET status = 'unbound', unbound_at = ?
                    WHERE tenant_id = ? AND subject_ref = ? AND policy_id = ? AND status = 'active'
                    """, now, tenantId, subject.subjectRef(), policyId);
            bindingCancelled = terminated > 0;
            Integer activeBindings = jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM rehealth_insurance_plan_binding
                    WHERE tenant_id = ? AND subject_ref = ? AND status = 'active'
                    """, Integer.class, tenantId, subject.subjectRef());
            if (activeBindings == null || activeBindings < 1) {
                jdbc.update("""
                        UPDATE rehealth_insurance_subject
                        SET consent_status = 'pending', updated_at = ?
                        WHERE tenant_id = ? AND subject_ref = ?
                        """, now, tenantId, subject.subjectRef());
            }
        }
        return new InsurancePolicyResponse.UnlinkResult(
                policyNo, subject.subjectRef(), subject.userName(), now, bindingCancelled);
    }

    /** Linked users of a policy, filtered to the caller's responsibility scope. */
    public List<InsurancePolicyResponse.PolicyLinkInfo> links(
            int tenantId, InsuranceAssignmentScope scope, String policyNo
    ) {
        String normalizedPolicyNo = required(policyNo, "policyNo", 128);
        StringBuilder sql = new StringBuilder("""
                SELECT link.subject_ref, link.status, link.created_at, link.created_by,
                       COALESCE(profile.name, u.realname, '未命名用户') AS user_name,
                       emp.realname AS employee_name
                FROM rehealth_insurance_policy_link link
                JOIN rehealth_insurance_subject s ON s.tenant_id=link.tenant_id AND s.subject_ref=link.subject_ref
                LEFT JOIN sys_user u ON u.id = CONVERT(s.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                LEFT JOIN sys_user emp ON emp.id = CONVERT(link.created_by USING utf8mb3) COLLATE utf8mb3_general_ci
                WHERE link.tenant_id=? AND link.policy_no=?
                """);
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.add(normalizedPolicyNo);
        if (scope != null) {
            sql.append("""
                      AND EXISTS (
                          SELECT 1 FROM rehealth_insurance_enrollment e
                          JOIN rehealth_insurance_user_assignment a
                            ON a.enrollment_id=e.id AND a.status='active'
                          WHERE e.tenant_id=link.tenant_id AND e.subject_ref=link.subject_ref
                            AND (a.employee_id=?
                                 OR (?='TEAM' AND EXISTS (
                                     SELECT 1 FROM sys_user_depart my_dept
                                     JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id=my_dept.dep_id
                                     WHERE my_dept.user_id=?
                                       AND assignee_dept.user_id = CONVERT(a.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                                 )))
                      )
                    """);
            args.add(scope.userId());
            args.add(scope.mode());
            args.add(scope.userId());
        }
        sql.append(" ORDER BY link.created_at DESC LIMIT 200");
        return jdbc.query(sql.toString(), (rs, rowNum) -> new InsurancePolicyResponse.PolicyLinkInfo(
                rs.getString("subject_ref"),
                rs.getString("user_name"),
                rs.getString("employee_name"),
                rs.getString("status"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        ), args.toArray());
    }
    //update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单取消关联】取消保单与 App 用户的关联-----------

    private String policyId(int tenantId, String policyNo) {
        try {
            return jdbc.queryForObject("""
                    SELECT id
                    FROM rehealth_insurance_policy
                    WHERE tenant_id = ? AND policy_no = ?
                    LIMIT 1
                    """, String.class, tenantId, policyNo);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("该保单号不存在于当前机构");
        }
    }

    private SubjectRef subjectByRef(int tenantId, String subjectRef) {
        try {
            return jdbc.queryForObject("""
                    SELECT subject.subject_ref,
                           COALESCE(profile.name, u.realname, '未命名用户') AS user_name
                    FROM rehealth_insurance_subject subject
                    LEFT JOIN sys_user u ON u.id = CONVERT(subject.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    LEFT JOIN rehealth_patient_profile profile ON profile.user_id = subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                    WHERE subject.tenant_id = ? AND subject.subject_ref = ?
                      AND subject.enrollment_status = 'active'
                    LIMIT 1
                    """, (rs, rowNum) -> new SubjectRef(rs.getString(1), rs.getString(2)),
                    tenantId, subjectRef);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("该用户在本机构没有有效参保关系");
        }
    }

    private PolicyRow policyRow(int tenantId, String policyNo) {
        try {
            return jdbc.queryForObject("""
                    SELECT status
                    FROM rehealth_insurance_policy
                    WHERE tenant_id = ? AND policy_no = ?
                    LIMIT 1
                    """, (rs, rowNum) -> new PolicyRow(rs.getString(1)),
                    tenantId, policyNo);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private InsurancePolicyLinkRow linkRow(int tenantId, String policyNo, String subjectRef) {
        try {
            return jdbc.queryForObject("""
                    SELECT status
                    FROM rehealth_insurance_policy_link
                    WHERE tenant_id = ? AND policy_no = ? AND subject_ref = ?
                    LIMIT 1
                    """, (rs, rowNum) -> new InsurancePolicyLinkRow(rs.getString(1)),
                    tenantId, policyNo, subjectRef);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private String userIdByPhone(String phone) {
        try {
            return jdbc.queryForObject("""
                    SELECT u.id
                    FROM sys_user u
                    WHERE u.phone = ? AND u.del_flag = 0
                    """ + EXCLUDE_PLATFORM_ADMINS + " LIMIT 1",
                    String.class, phone);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("没有注册账号与该手机号匹配，请确认用户已注册 App");
        }
    }

    private SubjectRef subjectByPhone(int tenantId, String userId) {
        try {
            return jdbc.queryForObject("""
                    SELECT subject.subject_ref,
                           COALESCE(profile.name, u.realname, '未命名用户') AS user_name
                    FROM rehealth_insurance_subject subject
                    LEFT JOIN sys_user u ON u.id = CONVERT(subject.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    LEFT JOIN rehealth_patient_profile profile ON profile.user_id = subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                    WHERE subject.tenant_id = ? AND subject.rehealth_user_id = ?
                      AND subject.enrollment_status = 'active'
                    LIMIT 1
                    """, (rs, rowNum) -> new SubjectRef(rs.getString(1), rs.getString(2)),
                    tenantId, userId);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.badRequest("该手机号用户尚未在贵机构参保，请先通过「导入参保人」建立参保关系");
        }
    }

    private SubjectRef subjectByEnrollment(int tenantId, String enrollmentId) {
        try {
            return jdbc.queryForObject("""
                    SELECT e.subject_ref,
                           COALESCE(profile.name, u.realname, '未命名用户') AS user_name
                    FROM rehealth_insurance_enrollment e
                    LEFT JOIN sys_user u ON u.id = CONVERT(e.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                    LEFT JOIN rehealth_patient_profile profile ON profile.user_id = e.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                    WHERE e.tenant_id = ? AND e.id = ? AND e.enrollment_status = 'active'
                    LIMIT 1
                    """, (rs, rowNum) -> new SubjectRef(rs.getString(1), rs.getString(2)),
                    tenantId, enrollmentId);
        } catch (EmptyResultDataAccessException ignored) {
            throw InsuranceApiException.notFound("该参与记录不存在或已失效");
        }
    }

    private static String required(String value, String field, int maxLength) {
        String normalized = trim(value, maxLength);
        if (normalized == null) {
            throw InsuranceApiException.badRequest(field + " is required");
        }
        return normalized;
    }

    private static String trim(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw InsuranceApiException.badRequest("field must be at most " + maxLength + " characters");
        }
        return normalized;
    }

    private static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    /** Package-visible for focused unit tests. */
    record PolicyRow(String status) {
    }

    /** Package-visible for focused unit tests. */
    record SubjectRef(String subjectRef, String userName) {
    }

    /** Package-visible for focused unit tests. */
    record InsurancePolicyLinkRow(String status) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】保单列表与添加保单关联-----------
