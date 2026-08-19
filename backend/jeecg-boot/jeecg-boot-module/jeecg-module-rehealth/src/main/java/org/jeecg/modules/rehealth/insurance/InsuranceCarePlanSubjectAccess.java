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

    public Subject requireAssigned(int tenantId, String managerUserId, String subjectId) {
        if (subjectId == null || subjectId.isBlank() || subjectId.trim().length() > 64) {
            throw InsuranceApiException.badRequest("subjectId is required");
        }
        return jdbc.query("""
                SELECT subject.subject_ref, subject.rehealth_user_id
                FROM rehealth_insurance_subject subject
                INNER JOIN rehealth_insurance_subject_manager scope
                  ON scope.tenant_id=subject.tenant_id
                 AND scope.subject_ref=subject.subject_ref
                 AND scope.manager_user_id=?
                 AND scope.status='active'
                WHERE subject.tenant_id=?
                  AND (subject.id=? OR subject.subject_ref=?)
                  AND subject.enrollment_status='active'
                  AND subject.consent_status='granted'
                LIMIT 1
                """, (rs, rowNum) -> new Subject(rs.getString(1), rs.getString(2)),
                managerUserId, tenantId, subjectId.trim(), subjectId.trim()).stream().findFirst()
                .orElseThrow(() -> InsuranceApiException.notFound(
                        "assigned and consented insurance subject was not found"));
    }

    public record Subject(String subjectRef, String rehealthUserId) {
    }
}
