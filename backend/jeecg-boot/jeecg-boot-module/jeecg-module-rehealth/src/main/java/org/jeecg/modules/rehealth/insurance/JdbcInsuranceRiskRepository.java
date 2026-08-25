package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcInsuranceRiskRepository implements InsuranceRiskRepository {
    private static final String TENANT_AND_RISK_CTE = """
            WITH tenant_subject AS (
                SELECT DISTINCT
                       insurance_subject.tenant_id AS tenant_id,
                       insurance_subject.rehealth_user_id AS internal_user_id,
                       insurance_subject.subject_ref AS subject_id
                FROM rehealth_insurance_subject insurance_subject
                INNER JOIN sys_user account
                    ON account.id = CONVERT(insurance_subject.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci
                INNER JOIN sys_tenant tenant ON tenant.id = insurance_subject.tenant_id
                WHERE insurance_subject.tenant_id = ?
                  AND insurance_subject.enrollment_status = 'active'
                  AND account.status = 1
                  AND account.del_flag = 0
                  AND tenant.status = 1
                  AND tenant.del_flag = 0
                  AND (
                      EXISTS (
                          SELECT 1
                          FROM rehealth_patient_profile candidate_profile
                          WHERE candidate_profile.user_id
                              = insurance_subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                      )
                      OR EXISTS (
                          SELECT 1
                          FROM rehealth_cvd_risk_result candidate_risk
                          WHERE candidate_risk.user_id
                              = insurance_subject.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
                      )
                  )
                  AND (? IS NULL OR EXISTS (
                      SELECT 1
                      FROM rehealth_insurance_user_assignment assignment
                      JOIN rehealth_insurance_enrollment enrollment
                          ON enrollment.id = assignment.enrollment_id
                      WHERE assignment.tenant_id = insurance_subject.tenant_id
                        AND enrollment.subject_ref = insurance_subject.subject_ref
                        AND assignment.status = 'active'
                        AND (
                            assignment.employee_id = ?
                            OR (
                                ? = 'TEAM'
                                AND EXISTS (
                                    SELECT 1
                                    FROM sys_user_depart manager_depart
                                    JOIN sys_user_depart assignee_depart
                                        ON assignee_depart.dep_id = manager_depart.dep_id
                                    WHERE manager_depart.user_id = ?
                                      AND assignee_depart.user_id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                                )
                            )
                        )
                  ))
            ), latest_risk AS (
                SELECT r.user_id, r.is_mock, r.risk_score, r.risk_level, r.model_version,
                       r.evaluated_at, r.contribution_json
                FROM tenant_subject ts
                INNER JOIN rehealth_cvd_risk_result r ON r.id = (
                    SELECT candidate.id
                    FROM rehealth_cvd_risk_result candidate
                    WHERE candidate.user_id
                        = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
                    ORDER BY candidate.evaluated_at DESC, candidate.id DESC
                    LIMIT 1
                )
            )
            """;

    private static final String SUBJECT_CTE = TENANT_AND_RISK_CTE + """
            , latest_intervention AS (
                SELECT p.user_id, p.is_mock, p.generated_at
                FROM tenant_subject ts
                INNER JOIN rehealth_intervention_plan p ON p.id = (
                    SELECT candidate.id
                    FROM rehealth_intervention_plan candidate
                    WHERE candidate.user_id
                        = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
                    ORDER BY candidate.generated_at DESC, candidate.id DESC
                    LIMIT 1
                )
            ), latest_policy AS (
                SELECT p.product_name, ts.internal_user_id AS policy_user_id,
                       JSON_UNQUOTE(JSON_EXTRACT(p.metadata_json, '$.channel')) AS channel_name
                FROM tenant_subject ts
                INNER JOIN rehealth_insurance_policy p ON p.id = (
                    SELECT candidate.id
                    FROM rehealth_insurance_policy candidate
                    WHERE candidate.tenant_id = ts.tenant_id
                      AND candidate.insured_subject_ref = ts.subject_id
                      AND candidate.status = 'active'
                    ORDER BY candidate.effective_on DESC, candidate.created_at DESC, candidate.id DESC
                    LIMIT 1
                )
            )
            """;

    private static final String NORMALIZED_RISK_LEVEL = """
            CASE
                WHEN LOWER(TRIM(r.risk_level)) IN ('high', 'very_high', 'severe') THEN 'high'
                WHEN LOWER(TRIM(r.risk_level)) IN ('medium', 'moderate') THEN 'medium'
                WHEN LOWER(TRIM(r.risk_level)) = 'low' THEN 'low'
                ELSE NULL
            END
            """;

    private static final String SUBJECT_FROM = """
            FROM tenant_subject ts
            LEFT JOIN rehealth_patient_profile profile
                ON profile.user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
            LEFT JOIN latest_risk r
                ON r.user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
            LEFT JOIN latest_intervention intervention
                ON intervention.user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
            LEFT JOIN latest_policy policy
                ON policy.policy_user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
            """;

    private final JdbcTemplate jdbc;

    public JdbcInsuranceRiskRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    @Override
    public DashboardSnapshot dashboard(int tenantId) {
        return dashboardScoped(tenantId, null);
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】范围查询改查新表并支持三级范围-----------
    @Override
    public DashboardSnapshot dashboard(int tenantId, InsuranceAssignmentScope scope) {
        return dashboardScoped(tenantId, scope);
    }

    @Override
    public DashboardSnapshot dashboard(int tenantId, String managerUserId) {
        return dashboardScoped(tenantId, new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF));
    }

    private DashboardSnapshot dashboardScoped(int tenantId, InsuranceAssignmentScope scope) {
        String sql = TENANT_AND_RISK_CTE + """
                SELECT COUNT(*) AS total_insured,
                       COALESCE(SUM(CASE
                           WHEN r.evaluated_at IS NOT NULL AND r.is_mock = 0 THEN 1 ELSE 0
                       END), 0) AS assessed_insured,
                       COALESCE(SUM(CASE
                           WHEN r.evaluated_at IS NOT NULL AND COALESCE(r.is_mock, 1) <> 0 THEN 1 ELSE 0
                       END), 0) AS synthetic_insured,
                       COALESCE(SUM(CASE
                           WHEN r.evaluated_at IS NULL THEN 1 ELSE 0
                       END), 0) AS unassessed_insured,
                       COALESCE(SUM(CASE
                           WHEN r.is_mock = 0 AND LOWER(TRIM(r.risk_level)) IN ('high', 'very_high', 'severe') THEN 1 ELSE 0
                       END), 0) AS high_risk,
                       COALESCE(SUM(CASE
                           WHEN r.is_mock = 0 AND LOWER(TRIM(r.risk_level)) IN ('medium', 'moderate') THEN 1 ELSE 0
                       END), 0) AS medium_risk,
                       COALESCE(SUM(CASE
                           WHEN r.is_mock = 0 AND LOWER(TRIM(r.risk_level)) = 'low' THEN 1 ELSE 0
                       END), 0) AS low_risk,
                       MAX(CASE WHEN r.is_mock = 0 THEN r.evaluated_at ELSE NULL END) AS latest_evaluated_at
                FROM tenant_subject ts
                LEFT JOIN latest_risk r
                    ON r.user_id = ts.internal_user_id COLLATE utf8mb4_0900_ai_ci
                """;
        DashboardSnapshot snapshot = jdbc.queryForObject(sql, (resultSet, rowNum) -> new DashboardSnapshot(
                longValue(resultSet, "total_insured"),
                longValue(resultSet, "assessed_insured"),
                longValue(resultSet, "synthetic_insured"),
                longValue(resultSet, "unassessed_insured"),
                longValue(resultSet, "high_risk"),
                longValue(resultSet, "medium_risk"),
                longValue(resultSet, "low_risk"),
                resultSet.getTimestamp("latest_evaluated_at")
                ), scopeArgs(tenantId, scope));
        return snapshot == null
                ? new DashboardSnapshot(0, 0, 0, 0, 0, 0, 0, null)
                : snapshot;
    }

    @Override
    public SubjectPage subjects(int tenantId, int pageNo, int pageSize, String keyword, String riskLevel) {
        return subjectsScoped(tenantId, null, pageNo, pageSize, keyword, riskLevel, null, null, null);
    }

    @Override
    public SubjectPage subjects(int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String riskLevel) {
        return subjectsScoped(tenantId, scope, pageNo, pageSize, keyword, riskLevel, null, null, null);
    }

    @Override
    public SubjectPage subjects(int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String riskLevel) {
        return subjectsScoped(tenantId, new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF),
                pageNo, pageSize, keyword, riskLevel, null, null, null);
    }

    @Override
    public SubjectPage subjects(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String riskLevel,
            String channel, Integer minAge, Integer maxAge
    ) {
        return subjectsScoped(tenantId, scope, pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge);
    }

    @Override
    public SubjectPage subjects(
            int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String riskLevel,
            String channel, Integer minAge, Integer maxAge
    ) {
        return subjectsScoped(tenantId, new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF),
                pageNo, pageSize, keyword, riskLevel, channel, minAge, maxAge);
    }

    private SubjectPage subjectsScoped(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String riskLevel,
            String channel, Integer minAge, Integer maxAge
    ) {
        String keywordLike = keyword == null ? null : "%" + escapeLike(keyword) + "%";
        String filters = """
                WHERE (? IS NULL OR profile.name LIKE ? ESCAPE '!' OR ts.subject_id = ?)
                  AND (? IS NULL OR (r.is_mock = 0 AND
                """ + NORMALIZED_RISK_LEVEL + """
                      = ?))
                  AND (? IS NULL OR policy.channel_name = ?)
                  AND (? IS NULL OR profile.age >= ?)
                  AND (? IS NULL OR profile.age <= ?)
                """;
        String countSql = SUBJECT_CTE + "SELECT COUNT(*) " + SUBJECT_FROM + filters;
        Long total = jdbc.queryForObject(
                countSql,
                Long.class,
                concat(scopeArgs(tenantId, scope),
                        keyword,
                        keywordLike,
                        keyword,
                        riskLevel,
                        riskLevel,
                        channel,
                        channel,
                        minAge,
                        minAge,
                        maxAge,
                        maxAge)
        );

        String pageSql = SUBJECT_CTE + """
                SELECT ts.subject_id, profile.name, profile.age, profile.gender, profile.bmi,
                       policy.product_name, policy.channel_name,
                       r.is_mock AS risk_is_mock, r.risk_score, r.risk_level, r.model_version,
                       r.evaluated_at, r.contribution_json,
                       intervention.is_mock AS intervention_is_mock,
                       NULL AS intervention_summary,
                       intervention.generated_at AS intervention_generated_at
                """ + SUBJECT_FROM + filters + """
                ORDER BY CASE
                    WHEN r.is_mock = 0 AND
                """ + NORMALIZED_RISK_LEVEL + """
                        = 'high' THEN 1
                    WHEN r.is_mock = 0 AND
                """ + NORMALIZED_RISK_LEVEL + """
                        = 'medium' THEN 2
                    WHEN r.is_mock = 0 AND
                """ + NORMALIZED_RISK_LEVEL + """
                        = 'low' THEN 3
                    WHEN r.evaluated_at IS NOT NULL THEN 4
                    ELSE 5
                END,
                CASE WHEN r.is_mock = 0 THEN r.risk_score ELSE NULL END DESC,
                ts.subject_id ASC
                LIMIT ? OFFSET ?
                """;
        List<SubjectSnapshot> records = jdbc.query(
                pageSql,
                (resultSet, rowNum) -> mapSubject(resultSet),
                concat(scopeArgs(tenantId, scope),
                        keyword,
                        keywordLike,
                        keyword,
                        riskLevel,
                        riskLevel,
                        channel,
                        channel,
                        minAge,
                        minAge,
                        maxAge,
                        maxAge,
                        pageSize,
                        (pageNo - 1) * pageSize)
        );
        return new SubjectPage(total == null ? 0 : total, records);
    }

    @Override
    public FilterOptions filterOptions(int tenantId, InsuranceAssignmentScope scope) {
        List<String> channels = jdbc.query(SUBJECT_CTE + """
                SELECT DISTINCT TRIM(policy.channel_name) AS channel_name
                """ + SUBJECT_FROM + """
                WHERE policy.channel_name IS NOT NULL AND TRIM(policy.channel_name) <> ''
                ORDER BY channel_name
                """, (resultSet, rowNum) -> resultSet.getString("channel_name"),
                scopeArgs(tenantId, scope));
        return jdbc.queryForObject(SUBJECT_CTE + """
                SELECT MIN(profile.age) AS min_age, MAX(profile.age) AS max_age
                """ + SUBJECT_FROM,
                (resultSet, rowNum) -> new FilterOptions(
                        channels,
                        nullableInteger(resultSet, "min_age"),
                        nullableInteger(resultSet, "max_age")
                ), scopeArgs(tenantId, scope));
    }

    @Override
    public FilterOptions filterOptions(int tenantId, String managerUserId) {
        return filterOptions(tenantId, new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF));
    }

    @Override
    public Optional<SubjectSnapshot> subject(int tenantId, String subjectId) {
        return subjectScoped(tenantId, null, subjectId);
    }

    @Override
    public Optional<SubjectSnapshot> subject(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        return subjectScoped(tenantId, scope, subjectId);
    }

    @Override
    public Optional<SubjectSnapshot> subject(int tenantId, String managerUserId, String subjectId) {
        return subjectScoped(tenantId, new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF), subjectId);
    }

    private Optional<SubjectSnapshot> subjectScoped(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        String sql = SUBJECT_CTE + """
                SELECT ts.subject_id, profile.name, profile.age, profile.gender, profile.bmi,
                       policy.product_name, policy.channel_name,
                       r.is_mock AS risk_is_mock, r.risk_score, r.risk_level, r.model_version,
                       r.evaluated_at, r.contribution_json,
                       intervention.is_mock AS intervention_is_mock,
                       NULL AS intervention_summary,
                       intervention.generated_at AS intervention_generated_at
                """ + SUBJECT_FROM + """
                WHERE ts.subject_id = ?
                LIMIT 1
                """;
        return jdbc.query(sql, (resultSet, rowNum) -> mapSubject(resultSet),
                        concat(scopeArgs(tenantId, scope), subjectId))
                .stream()
                .findFirst();
    }

    private static Object[] scopeArgs(int tenantId, InsuranceAssignmentScope scope) {
        if (scope == null) {
            return new Object[]{tenantId, null, null, null, null};
        }
        return new Object[]{tenantId, scope.userId(), scope.userId(), scope.mode(), scope.userId()};
    }

    private static Object[] concat(Object[] head, Object... tail) {
        Object[] result = new Object[head.length + tail.length];
        System.arraycopy(head, 0, result, 0, head.length);
        System.arraycopy(tail, 0, result, head.length, tail.length);
        return result;
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】范围查询改查新表并支持三级范围-----------

    private SubjectSnapshot mapSubject(ResultSet resultSet) throws SQLException {
        return new SubjectSnapshot(
                resultSet.getString("subject_id"),
                resultSet.getString("name"),
                nullableInteger(resultSet, "age"),
                resultSet.getString("gender"),
                resultSet.getBigDecimal("bmi"),
                resultSet.getString("product_name"),
                resultSet.getString("channel_name"),
                nullableBoolean(resultSet, "risk_is_mock"),
                nullableDouble(resultSet, "risk_score"),
                resultSet.getString("risk_level"),
                resultSet.getString("model_version"),
                resultSet.getTimestamp("evaluated_at"),
                resultSet.getString("contribution_json"),
                nullableBoolean(resultSet, "intervention_is_mock"),
                resultSet.getString("intervention_summary"),
                resultSet.getTimestamp("intervention_generated_at")
        );
    }

    private static long longValue(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? 0 : value.longValue();
    }

    private static Integer nullableInteger(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.intValue();
    }

    private static Double nullableDouble(ResultSet resultSet, String column) throws SQLException {
        Number value = (Number) resultSet.getObject(column);
        return value == null ? null : value.doubleValue();
    }

    private static Boolean nullableBoolean(ResultSet resultSet, String column) throws SQLException {
        Object value = resultSet.getObject(column);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    static String escapeLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
