package org.jeecg.modules.rehealth.insurance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceClaimEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceConsentEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceCoverageEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceInterventionEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePolicyEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceClaimMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceConsentMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceCoverageMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceInterventionMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** MyBatis-Plus read model for the first insurer business dashboard phase. */
@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class MybatisPlusInsuranceBusinessRepository implements InsuranceBusinessRepository {
    private static final String ACTIVE = "active";

    private final InsurancePolicyMapper policyMapper;
    private final InsuranceCoverageMapper coverageMapper;
    private final InsuranceClaimMapper claimMapper;
    private final InsuranceInterventionMapper interventionMapper;
    private final InsuranceConsentMapper consentMapper;

    public MybatisPlusInsuranceBusinessRepository(
            InsurancePolicyMapper policyMapper,
            InsuranceCoverageMapper coverageMapper,
            InsuranceClaimMapper claimMapper,
            InsuranceInterventionMapper interventionMapper,
            InsuranceConsentMapper consentMapper
    ) {
        this.policyMapper = policyMapper;
        this.coverageMapper = coverageMapper;
        this.claimMapper = claimMapper;
        this.interventionMapper = interventionMapper;
        this.consentMapper = consentMapper;
    }

    @Override
    public InsuranceRiskRepository.BusinessSnapshot tenant(int tenantId) {
        return snapshot(
                tenantId,
                null,
                policyCount(tenantId, null),
                coverageCount(tenantId, null),
                claimAggregate(tenantId, null),
                interventionCount(tenantId, null),
                null
        );
    }

    @Override
    public InsuranceRiskRepository.BusinessSnapshot subject(int tenantId, String subjectRef) {
        ClaimAggregate claims = claimAggregate(tenantId, subjectRef);
        String consentStatus = consentStatus(tenantId, subjectRef);
        return snapshot(
                tenantId,
                subjectRef,
                policyCount(tenantId, subjectRef),
                coverageCount(tenantId, subjectRef),
                claims,
                interventionCount(tenantId, subjectRef),
                consentStatus
        );
    }

    private InsuranceRiskRepository.BusinessSnapshot snapshot(
            int tenantId,
            String subjectRef,
            long policies,
            long coverages,
            ClaimAggregate claims,
            long interventions,
            String consentStatus
    ) {
        Timestamp latest = max(
                latestPolicy(tenantId, subjectRef),
                latestCoverage(tenantId, subjectRef),
                claims.latestUpdatedAt(),
                latestIntervention(tenantId, subjectRef)
        );
        return new InsuranceRiskRepository.BusinessSnapshot(
                policies,
                coverages,
                claims.count(),
                claims.billedAmount(),
                claims.paidAmount(),
                interventions,
                consentStatus == null ? "unknown" : consentStatus,
                latest
        );
    }

    private long policyCount(int tenantId, String subjectRef) {
        LambdaQueryWrapper<InsurancePolicyEntity> query = new LambdaQueryWrapper<InsurancePolicyEntity>()
                .eq(InsurancePolicyEntity::getTenantId, tenantId)
                .eq(InsurancePolicyEntity::getStatus, ACTIVE);
        if (subjectRef != null) {
            query.eq(InsurancePolicyEntity::getInsuredSubjectRef, subjectRef);
        }
        return policyMapper.selectCount(query);
    }

    private long coverageCount(int tenantId, String subjectRef) {
        LambdaQueryWrapper<InsuranceCoverageEntity> query = new LambdaQueryWrapper<InsuranceCoverageEntity>()
                .eq(InsuranceCoverageEntity::getTenantId, tenantId)
                .eq(InsuranceCoverageEntity::getStatus, ACTIVE);
        if (subjectRef != null) {
            query.eq(InsuranceCoverageEntity::getSubjectRef, subjectRef);
        }
        return coverageMapper.selectCount(query);
    }

    private long interventionCount(int tenantId, String subjectRef) {
        LambdaQueryWrapper<InsuranceInterventionEntity> query = new LambdaQueryWrapper<InsuranceInterventionEntity>()
                .eq(InsuranceInterventionEntity::getTenantId, tenantId)
                .in(InsuranceInterventionEntity::getStatus, "enrolled", ACTIVE);
        if (subjectRef != null) {
            query.eq(InsuranceInterventionEntity::getSubjectRef, subjectRef);
        }
        return interventionMapper.selectCount(query);
    }

    private ClaimAggregate claimAggregate(int tenantId, String subjectRef) {
        QueryWrapper<InsuranceClaimEntity> query = new QueryWrapper<>();
        query.select(
                        "COUNT(*) AS claim_count",
                        "COALESCE(SUM(billed_amount), 0) AS billed_amount",
                        "COALESCE(SUM(paid_amount), 0) AS paid_amount",
                        "MAX(updated_at) AS latest_updated_at"
                )
                .eq("tenant_id", tenantId);
        if (subjectRef != null) {
            query.eq("subject_ref", subjectRef);
        }
        Map<String, Object> row = first(claimMapper.selectMaps(query));
        return new ClaimAggregate(
                number(row, "claim_count").longValue(),
                decimal(row, "billed_amount"),
                decimal(row, "paid_amount"),
                timestamp(row, "latest_updated_at")
        );
    }

    private String consentStatus(int tenantId, String subjectRef) {
        QueryWrapper<InsuranceConsentEntity> query = new QueryWrapper<>();
        query.select("status")
                .eq("tenant_id", tenantId)
                .eq("subject_ref", subjectRef)
                .orderByDesc("updated_at")
                .last("LIMIT 1");
        InsuranceConsentEntity consent = consentMapper.selectOne(query);
        return consent == null ? "unknown" : consent.getStatus();
    }

    private Timestamp latestPolicy(int tenantId, String subjectRef) {
        QueryWrapper<InsurancePolicyEntity> query = new QueryWrapper<>();
        query.select("MAX(updated_at) AS latest_updated_at").eq("tenant_id", tenantId);
        if (subjectRef != null) {
            query.eq("insured_subject_ref", subjectRef);
        }
        return timestamp(first(policyMapper.selectMaps(query)), "latest_updated_at");
    }

    private Timestamp latestCoverage(int tenantId, String subjectRef) {
        QueryWrapper<InsuranceCoverageEntity> query = new QueryWrapper<>();
        query.select("MAX(updated_at) AS latest_updated_at").eq("tenant_id", tenantId);
        if (subjectRef != null) {
            query.eq("subject_ref", subjectRef);
        }
        return timestamp(first(coverageMapper.selectMaps(query)), "latest_updated_at");
    }

    private Timestamp latestIntervention(int tenantId, String subjectRef) {
        QueryWrapper<InsuranceInterventionEntity> query = new QueryWrapper<>();
        query.select("MAX(updated_at) AS latest_updated_at").eq("tenant_id", tenantId);
        if (subjectRef != null) {
            query.eq("subject_ref", subjectRef);
        }
        return timestamp(first(interventionMapper.selectMaps(query)), "latest_updated_at");
    }

    private static Map<String, Object> first(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private static Number number(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value instanceof Number number ? number : 0;
    }

    private static BigDecimal decimal(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private static Timestamp timestamp(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return Timestamp.valueOf(localDateTime);
        }
        return value == null ? null : Timestamp.valueOf(value.toString());
    }

    private static Object value(Map<String, Object> row, String key) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Object value = row.get(key);
        if (value != null || row.containsKey(key)) {
            return value;
        }
        return row.get(key.toUpperCase());
    }

    private static Timestamp max(Timestamp... timestamps) {
        Timestamp result = null;
        for (Timestamp timestamp : timestamps) {
            if (timestamp != null && (result == null || timestamp.after(result))) {
                result = timestamp;
            }
        }
        return result;
    }

    private record ClaimAggregate(long count, BigDecimal billedAmount, BigDecimal paidAmount, Timestamp latestUpdatedAt) {
    }
}
