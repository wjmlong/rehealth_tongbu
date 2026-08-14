package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_study_member")
public class InsuranceStudyMemberEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("snapshot_id") private String snapshotId;
    @TableField("subject_ref") private String subjectRef;
    @TableField("cohort_group") private String cohortGroup;
    @TableField("baseline_risk") private BigDecimal baselineRisk;
    @TableField("outcome_value") private BigDecimal outcomeValue;
    @TableField("intervention_status") private String interventionStatus;
    @TableField("covariate_json") private String covariateJson;
    @TableField("source_row_hash") private String sourceRowHash;
    @TableField("created_at") private LocalDateTime createdAt;
}
