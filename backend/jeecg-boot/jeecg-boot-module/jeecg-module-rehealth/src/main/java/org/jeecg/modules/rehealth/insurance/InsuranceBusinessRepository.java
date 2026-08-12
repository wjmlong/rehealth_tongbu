package org.jeecg.modules.rehealth.insurance;

/**
 * MyBatis-Plus backed read model for insurer business summaries.
 *
 * <p>The legacy risk repository remains responsible for the existing risk
 * bridge. Insurance business tables are intentionally isolated here so every
 * query can enforce the tenant and subject scope independently.</p>
 */
public interface InsuranceBusinessRepository {
    InsuranceRiskRepository.BusinessSnapshot tenant(int tenantId);

    InsuranceRiskRepository.BusinessSnapshot subject(int tenantId, String subjectRef);
}
