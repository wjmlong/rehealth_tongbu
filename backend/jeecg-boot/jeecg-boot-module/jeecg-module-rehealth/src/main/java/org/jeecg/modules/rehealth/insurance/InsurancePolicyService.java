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

/**
 * Insurance-side policy dispatch queries: the tenant policy list (unassigned
 * pool policies are visible to every importer, assigned policies stay inside
 * the staff's responsibility scope), the dispatchable subjects, and the
 * two-step assignment by phone.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】保单列表与按手机号分配-----------
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

    public InsurancePolicyResponse.PolicyPage list(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword
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
                    + " OR LOWER(COALESCE(p.product_name,'')) LIKE ?"
                    + " OR LOWER(COALESCE(profile.name,'')) LIKE ?)");
            String like = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        // 未分配保单（被保人为空）属于机构保单库，对全部导入人可见；
        // 已分配保单只对负责范围内的员工可见。
        if (scope != null) {
            where.append(" AND (p.insured_subject_ref IS NULL OR EXISTS (")
                    .append("      SELECT 1 FROM rehealth_insurance_enrollment e")
                    .append("      JOIN rehealth_insurance_user_assignment a")
                    .append("        ON a.enrollment_id=e.id AND a.status='active'")
                    .append("      WHERE e.tenant_id=p.tenant_id AND e.subject_ref=p.insured_subject_ref")
                    .append("        AND (a.employee_id=?")
                    .append("             OR (?='TEAM' AND EXISTS (")
                    .append("                 SELECT 1 FROM sys_user_depart my_dept")
                    .append("                 JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id=my_dept.dep_id")
                    .append("                 WHERE my_dept.user_id=?")
                    .append("                   AND assignee_dept.user_id = CONVERT(a.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci")
                    .append("             )))")
                    .append("  ))");
            args.add(scope.userId());
            args.add(scope.mode());
            args.add(scope.userId());
        }

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rehealth_insurance_policy p"
                        + " LEFT JOIN rehealth_insurance_subject s ON s.tenant_id=p.tenant_id AND s.subject_ref=p.insured_subject_ref"
                        + " LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci"
                        + " WHERE " + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(normalizedPageSize);
        pageArgs.add((normalizedPageNo - 1) * normalizedPageSize);
        List<InsurancePolicyResponse.Item> items = jdbc.query(
                "SELECT p.policy_no, p.product_name, p.policy_type, p.default_plan_id,"
                        + " cat.name AS plan_name, p.insured_subject_ref, p.status, p.effective_on, p.assigned_at,"
                        + " COALESCE(profile.name, u.realname, '未分配') AS user_name"
                        + " FROM rehealth_insurance_policy p"
                        + " LEFT JOIN rehealth_insurance_subject s ON s.tenant_id=p.tenant_id AND s.subject_ref=p.insured_subject_ref"
                        + " LEFT JOIN rehealth_insurance_plan_catalog cat"
                        + "   ON cat.tenant_id=p.tenant_id AND cat.plan_id=p.default_plan_id AND cat.status='active'"
                        + " LEFT JOIN sys_user u ON u.id = CONVERT(s.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci"
                        + " LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci"
                        + " WHERE " + where
                        + " ORDER BY p.assigned_at IS NULL DESC, p.effective_on DESC, p.created_at DESC"
                        + " LIMIT ? OFFSET ?",
                (rs, rowNum) -> new InsurancePolicyResponse.Item(
                        rs.getString("policy_no"),
                        rs.getString("product_name"),
                        rs.getString("policy_type"),
                        rs.getString("default_plan_id"),
                        rs.getString("plan_name"),
                        rs.getString("insured_subject_ref"),
                        rs.getString("user_name"),
                        rs.getString("status"),
                        toLocalDate(rs.getDate("effective_on")),
                        toLocalDateTime(rs.getTimestamp("assigned_at"))
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
     * Two-step dispatch: assigns an (unassigned) policy to an APP user by
     * phone. The user must be a registered APP account, enrolled in the
     * tenant, and inside the caller's responsibility scope; a policy already
     * assigned to another subject cannot be reassigned.
     */
    public InsurancePolicyResponse.AssignResult assign(
            int tenantId, InsuranceAssignmentScope scope, InsurancePolicyResponse.AssignRequest request
    ) {
        String policyNo = required(request.policyNo(), "policyNo", 128);
        String phone = required(request.phone(), "phone", 45);

        PolicyRow policy = policyRow(tenantId, policyNo);
        if (policy == null) {
            throw InsuranceApiException.notFound("该保单号不存在于当前机构");
        }
        if (!"active".equalsIgnoreCase(policy.status())) {
            throw InsuranceApiException.badRequest("该保单非生效状态，无法分配");
        }
        String userId = userIdByPhone(phone);
        SubjectRef subject = activeSubject(tenantId, userId);
        dispatchAccess.requireDispatchable(tenantId, scope, subject.subjectRef());
        if (policy.insuredSubjectRef() != null && !policy.insuredSubjectRef().equals(subject.subjectRef())) {
            throw InsuranceApiException.conflict("该保单已分配给其他被保人，不能重复分配");
        }
        LocalDateTime now = LocalDateTime.now();
        if (policy.insuredSubjectRef() == null) {
            jdbc.update("""
                    UPDATE rehealth_insurance_policy
                    SET insured_subject_ref = ?, assigned_at = ?, updated_at = ?
                    WHERE tenant_id = ? AND policy_no = ?
                    """, subject.subjectRef(), now, now, tenantId, policyNo);
        }
        return new InsurancePolicyResponse.AssignResult(
                policyNo, subject.subjectRef(), subject.userName(), now);
    }

    private PolicyRow policyRow(int tenantId, String policyNo) {
        try {
            return jdbc.queryForObject("""
                    SELECT insured_subject_ref, status
                    FROM rehealth_insurance_policy
                    WHERE tenant_id = ? AND policy_no = ?
                    LIMIT 1
                    """, (rs, rowNum) -> new PolicyRow(rs.getString(1), rs.getString(2)),
                    tenantId, policyNo);
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

    private SubjectRef activeSubject(int tenantId, String userId) {
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

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    /** Package-visible for focused unit tests. */
    record PolicyRow(String insuredSubjectRef, String status) {
    }

    /** Package-visible for focused unit tests. */
    record SubjectRef(String subjectRef, String userName) {
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】保单列表与按手机号分配-----------
