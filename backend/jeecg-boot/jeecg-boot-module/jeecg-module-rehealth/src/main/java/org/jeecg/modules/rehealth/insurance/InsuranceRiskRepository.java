package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface InsuranceRiskRepository {
    DashboardSnapshot dashboard(int tenantId);

    SubjectPage subjects(int tenantId, int pageNo, int pageSize, String keyword, String riskLevel);

    Optional<SubjectSnapshot> subject(int tenantId, String subjectId);

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

    record SubjectSnapshot(
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
