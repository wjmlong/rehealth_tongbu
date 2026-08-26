package org.jeecg.modules.rehealth.insurance;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Scope guard for insurance-side policy dispatch: a policy may only be
 * imported/dispatched for a subject the current staff is responsible for.
 *
 * <p>Unlike {@link InsuranceCarePlanSubjectAccess} this check deliberately
 * does not require user consent — policies are dispatched before the APP user
 * grants the program authorization, and the consent gate stays in the APP
 * binding step.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】保单只能派发给当前员工负责的被保人-----------
@Component
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class InsuranceDispatchAccess {
    private final JdbcTemplate jdbc;

    public InsuranceDispatchAccess(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    /**
     * Requires an active assignment between the current staff (or, for a TEAM
     * scope, any employee sharing a department) and the subject's enrollment.
     * A {@code null} scope means unrestricted and returns immediately.
     */
    public void requireDispatchable(int tenantId, InsuranceAssignmentScope scope, String subjectRef) {
        if (scope == null) {
            return;
        }
        if (subjectRef == null || subjectRef.isBlank() || subjectRef.trim().length() > 64) {
            throw InsuranceApiException.badRequest("subjectRef is required");
        }
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM rehealth_insurance_subject subject
                INNER JOIN rehealth_insurance_enrollment enrollment
                  ON enrollment.tenant_id=subject.tenant_id
                 AND enrollment.subject_ref=subject.subject_ref
                INNER JOIN rehealth_insurance_user_assignment assignment
                  ON assignment.enrollment_id=enrollment.id
                 AND assignment.status='active'
                WHERE subject.tenant_id=?
                  AND subject.subject_ref=?
                  AND (assignment.employee_id=?
                       OR (?='TEAM' AND EXISTS (
                           SELECT 1 FROM sys_user_depart my_dept
                           JOIN sys_user_depart assignee_dept ON assignee_dept.dep_id=my_dept.dep_id
                           WHERE my_dept.user_id=?
                             AND assignee_dept.user_id = CONVERT(assignment.employee_id USING utf8mb3) COLLATE utf8mb3_general_ci
                       )))
                LIMIT 1
                """, Integer.class, tenantId, subjectRef.trim(),
                scope.userId(), scope.mode(), scope.userId());
        if (count == null || count < 1) {
            throw InsuranceApiException.forbidden("该被保人不在您的负责范围内，请先认领该用户或选择您负责的用户");
        }
    }
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧保单派发】保单只能派发给当前员工负责的被保人-----------
