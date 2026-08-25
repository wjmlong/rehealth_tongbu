package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface InsuranceRiskRepository {
    DashboardSnapshot dashboard(int tenantId);

    //update-begin---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】仓储接口增加三级范围查询-----------
    default DashboardSnapshot dashboard(int tenantId, InsuranceAssignmentScope scope) {
        return dashboard(tenantId);
    }

    default DashboardSnapshot dashboard(int tenantId, String managerUserId) {
        return dashboard(tenantId);
    }

    SubjectPage subjects(int tenantId, int pageNo, int pageSize, String keyword, String riskLevel);

    default SubjectPage subjects(int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String riskLevel) {
        return subjects(tenantId, pageNo, pageSize, keyword, riskLevel);
    }

    default SubjectPage subjects(int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String riskLevel) {
        return subjects(tenantId, pageNo, pageSize, keyword, riskLevel);
    }

    default SubjectPage subjects(
            int tenantId, InsuranceAssignmentScope scope, int pageNo, int pageSize, String keyword, String riskLevel,
            String channel, Integer minAge, Integer maxAge
    ) {
        return subjects(tenantId, scope, pageNo, pageSize, keyword, riskLevel);
    }

    default SubjectPage subjects(
            int tenantId, String managerUserId, int pageNo, int pageSize, String keyword, String riskLevel,
            String channel, Integer minAge, Integer maxAge
    ) {
        return subjects(tenantId, managerUserId, pageNo, pageSize, keyword, riskLevel);
    }

    default FilterOptions filterOptions(int tenantId, InsuranceAssignmentScope scope) {
        return new FilterOptions(List.of(), null, null);
    }

    default FilterOptions filterOptions(int tenantId, String managerUserId) {
        return new FilterOptions(List.of(), null, null);
    }

    Optional<SubjectSnapshot> subject(int tenantId, String subjectId);

    default Optional<SubjectSnapshot> subject(int tenantId, InsuranceAssignmentScope scope, String subjectId) {
        return subject(tenantId, subjectId);
    }

    default Optional<SubjectSnapshot> subject(int tenantId, String managerUserId, String subjectId) {
        return subject(tenantId, subjectId);
    }
    //update-end---author:ai-agent ---date:2026-08-25  for：【保险侧用户服务关系一期】仓储接口增加三级范围查询-----------

    record DashboardSnapshot(
            long totalInsured,
            long assessedInsured,
            long syntheticInsured,
            long unassessedInsured,
            long highRisk,
            long mediumRisk,
            long lowRisk,
            Timestamp latestEvaluatedAt
    ) {
    }

    record BusinessSnapshot(
            long activePolicies,
            long activeCoverages,
            long claimCount,
            BigDecimal billedAmount,
            BigDecimal paidAmount,
            long activeInterventions,
            String consentStatus,
            Timestamp latestUpdatedAt
    ) {
        public BusinessSnapshot(
                long activePolicies,
                long activeCoverages,
                long claimCount,
                BigDecimal billedAmount,
                BigDecimal paidAmount,
                long activeInterventions,
                Timestamp latestUpdatedAt
        ) {
            this(activePolicies, activeCoverages, claimCount, billedAmount, paidAmount,
                    activeInterventions, "unknown", latestUpdatedAt);
        }
    }

    record SubjectPage(long total, List<SubjectSnapshot> records) {
    }

    record FilterOptions(List<String> channels, Integer minAge, Integer maxAge) {
    }

    record SubjectSnapshot(
            String subjectId,
            String name,
            Integer age,
            String gender,
            BigDecimal bmi,
            String productName,
            String channelName,
            Boolean riskMock,
            Double riskScore,
            String riskLevel,
            String modelVersion,
            Timestamp evaluatedAt,
            String contributionJson,
            Boolean interventionMock,
            String interventionSummary,
            Timestamp interventionGeneratedAt
    ) {
        public SubjectSnapshot(
                String subjectId,
                String name,
                Integer age,
                String gender,
                BigDecimal bmi,
                Boolean riskMock,
                Double riskScore,
                String riskLevel,
                String modelVersion,
                Timestamp evaluatedAt,
                String contributionJson,
                Boolean interventionMock,
                String interventionSummary,
                Timestamp interventionGeneratedAt
        ) {
            this(subjectId, name, age, gender, bmi, null, null, riskMock, riskScore,
                    riskLevel, modelVersion, evaluatedAt, contributionJson,
                    interventionMock, interventionSummary, interventionGeneratedAt);
        }

        public boolean hasRisk() {
            return evaluatedAt != null;
        }

        public boolean hasVerifiedRisk() {
            return hasRisk() && Boolean.FALSE.equals(riskMock);
        }

        public boolean hasVerifiedIntervention() {
            return interventionGeneratedAt != null && Boolean.FALSE.equals(interventionMock);
        }
    }
}
