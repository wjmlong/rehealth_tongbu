package org.jeecg.modules.rehealth.insurance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceStudyMemberEntity;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InsuranceStudyMemberMapper extends BaseMapper<InsuranceStudyMemberEntity> {
    @Select("""
            SELECT s.subject_ref,
                   CASE WHEN EXISTS (
                       SELECT 1 FROM rehealth_insurance_intervention i
                       WHERE i.tenant_id = s.tenant_id AND i.subject_ref = s.subject_ref
                         AND i.status IN ('enrolled', 'active')
                         AND (#{periodEnd} IS NULL OR i.enrolled_at IS NULL OR DATE(i.enrolled_at) <= #{periodEnd})
                         AND (#{periodStart} IS NULL OR i.ended_at IS NULL OR DATE(i.ended_at) >= #{periodStart})
                   ) THEN 'treated' ELSE 'control' END AS cohort_group,
                   risk.risk_score AS baseline_risk,
                   COALESCE(claims.outcome_value, 0) AS outcome_value,
                   intervention.status AS intervention_status,
                   JSON_OBJECT(
                       'age', profile.age,
                       'gender', profile.gender,
                       'bmi', profile.bmi,
                       'baselineRisk', risk.risk_score
                   ) AS covariate_json
            FROM rehealth_insurance_subject s
            JOIN rehealth_insurance_policy_link link
              ON link.tenant_id = s.tenant_id AND link.subject_ref = s.subject_ref
             AND link.status = 'assigned'
            JOIN rehealth_insurance_policy p
              ON p.tenant_id = link.tenant_id AND p.policy_no = link.policy_no
             AND p.status = 'active'
             AND (#{periodEnd} IS NULL OR p.effective_on IS NULL OR p.effective_on <= #{periodEnd})
             AND (#{periodStart} IS NULL OR p.expires_on IS NULL OR p.expires_on >= #{periodStart})
            LEFT JOIN rehealth_patient_profile profile ON profile.user_id = s.rehealth_user_id
            LEFT JOIN rehealth_cvd_risk_result risk ON risk.id = (
                SELECT candidate.id FROM rehealth_cvd_risk_result candidate
                WHERE candidate.user_id = s.rehealth_user_id AND candidate.is_mock = 0
                  AND (#{periodStart} IS NULL OR DATE(candidate.evaluated_at) <= #{periodStart})
                ORDER BY candidate.evaluated_at DESC, candidate.id DESC LIMIT 1
            )
            LEFT JOIN (
                SELECT tenant_id, subject_ref, SUM(COALESCE(paid_amount, 0)) outcome_value
                FROM rehealth_insurance_claim
                WHERE (#{periodStart} IS NULL OR event_on >= #{periodStart})
                  AND (#{periodEnd} IS NULL OR event_on <= #{periodEnd})
                  AND status NOT IN ('rejected', 'reversed', 'cancelled')
                GROUP BY tenant_id, subject_ref
            ) claims ON claims.tenant_id = s.tenant_id AND claims.subject_ref = s.subject_ref
            LEFT JOIN rehealth_insurance_intervention intervention ON intervention.id = (
                SELECT candidate.id FROM rehealth_insurance_intervention candidate
                WHERE candidate.tenant_id = s.tenant_id AND candidate.subject_ref = s.subject_ref
                ORDER BY candidate.updated_at DESC, candidate.id DESC LIMIT 1
            )
            WHERE s.tenant_id = #{tenantId}
              AND s.enrollment_status = 'active'
              AND s.consent_status = 'granted'
            GROUP BY s.subject_ref, risk.risk_score, claims.outcome_value, intervention.status,
                     profile.age, profile.gender, profile.bmi
            ORDER BY s.subject_ref
            """)
    List<SnapshotCandidate> selectSnapshotCandidates(
            @Param("tenantId") int tenantId,
            @Param("periodStart") LocalDate periodStart,
            @Param("periodEnd") LocalDate periodEnd
    );

    record SnapshotCandidate(
            String subjectRef,
            String cohortGroup,
            java.math.BigDecimal baselineRisk,
            java.math.BigDecimal outcomeValue,
            String interventionStatus,
            String covariateJson
    ) {
    }
}
