package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Insurance-side policy dispatch queries: the tenant policy list and the
 * dispatchable subjects, both restricted to the current staff's assignment
 * scope (SELF / TEAM / null = whole organization).
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人查询-----------
@Service
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsurancePolicyService {
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CANDIDATES = 200;

    private final JdbcTemplate jdbc;

    public InsurancePolicyService(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
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
        appendScope(where, args, scope);

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM rehealth_insurance_policy p"
                        + " JOIN rehealth_insurance_subject s ON s.tenant_id=p.tenant_id AND s.subject_ref=p.insured_subject_ref"
                        + " LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci"
                        + " WHERE " + where,
                Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(normalizedPageSize);
        pageArgs.add((normalizedPageNo - 1) * normalizedPageSize);
        List<InsurancePolicyResponse.Item> items = jdbc.query(
                "SELECT p.policy_no, p.product_name, p.policy_type, p.default_plan_id,"
                        + " cat.name AS plan_name, p.insured_subject_ref, p.status, p.effective_on,"
                        + " COALESCE(profile.name, u.realname, '未命名用户') AS user_name"
                        + " FROM rehealth_insurance_policy p"
                        + " JOIN rehealth_insurance_subject s ON s.tenant_id=p.tenant_id AND s.subject_ref=p.insured_subject_ref"
                        + " LEFT JOIN rehealth_insurance_plan_catalog cat"
                        + "   ON cat.tenant_id=p.tenant_id AND cat.plan_id=p.default_plan_id AND cat.status='active'"
                        + " LEFT JOIN sys_user u ON u.id = CONVERT(s.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci"
                        + " LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci"
                        + " WHERE " + where
                        + " ORDER BY p.effective_on DESC, p.created_at DESC"
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
                        toLocalDate(rs.getDate("effective_on"))
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

    private void appendScope(StringBuilder where, List<Object> args, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return;
        }
        where.append("""
                  AND EXISTS (
                      SELECT 1 FROM rehealth_insurance_enrollment e
                      JOIN rehealth_insurance_user_assignment a
                        ON a.enrollment_id=e.id AND a.status='active'
                      WHERE e.tenant_id=p.tenant_id AND e.subject_ref=p.insured_subject_ref
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

    private static LocalDate toLocalDate(Date value) {
        return value == null ? null : value.toLocalDate();
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】官网保单列表与可派发被保人查询-----------
