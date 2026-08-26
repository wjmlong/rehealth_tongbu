package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceCarePlanSubjectAccess {
    private final JdbcTemplate jdbc;

    public InsuranceCarePlanSubjectAccess(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】关怀计划归属校验切到新服务关系表-----------
    public Subject requireAssigned(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        if (subjectId == null || subjectId.isBlank() || subjectId.trim().length() > 64) {
            throw InsuranceApiException.badRequest("subjectId is required");
        }
        StringBuilder sql = new StringBuilder("""
                SELECT subject.subject_ref, subject.rehealth_user_id
                FROM rehealth_insurance_subject subject
                INNER JOIN rehealth_insurance_enrollment enrollment
                  ON enrollment.tenant_id=subject.tenant_id
                 AND enrollment.subject_ref=subject.subject_ref
                INNER JOIN rehealth_insurance_user_assignment assignment
                  ON assignment.enrollment_id=enrollment.id
                 AND assignment.status='active'
                """);
        if (scope != null) {
            sql.append("""
                      AND (assignment.employee_id=?
                           OR (?='TEAM' AND EXISTS (
                               SELECT 1 FROM sys_user_depart my_dept
                               JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id=my_dept.dep_id
                               WHERE my_dept.user_id=? AND assignee_dept.user_id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                           )))
                    """);
        }
        sql.append("""
                WHERE subject.tenant_id=?
                  AND (subject.id=? OR subject.subject_ref=?)
                  AND subject.enrollment_status='active'
                  AND subject.consent_status='granted'
                LIMIT 1
                """);
        Object[] args = scope == null
                ? new Object[]{tenantId, subjectId.trim(), subjectId.trim()}
                : new Object[]{scope.userId(), scope.mode(), scope.userId(), tenantId, subjectId.trim(), subjectId.trim()};
        return jdbc.query(sql.toString(),
                (rs, rowNum) -> new Subject(rs.getString(1), rs.getString(2)),
                args).stream().findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound(
                        "assigned and consented insurance subject was not found"));
    }

    public Subject requireAssigned(int tenantId, String managerUserId, String subjectId) {
        return requireAssigned(tenantId, managerUserId == null ? null
                : new InsuranceAssignmentScope(managerUserId, InsuranceAssignmentScope.MODE_SELF), subjectId);
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】关怀计划归属校验切到新服务关系表-----------

    public record Subject(String subjectRef, String rehealthUserId) {
    }
}
