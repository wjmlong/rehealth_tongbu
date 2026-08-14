package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_study")
public class InsuranceStudyEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("study_no") private String studyNo;
    private String title;
    @TableField("period_start") private LocalDate periodStart;
    @TableField("period_end") private LocalDate periodEnd;
    @TableField("population_rule_json") private String populationRuleJson;
    @TableField("intervention_rule_json") private String interventionRuleJson;
    @TableField("outcome_rule_json") private String outcomeRuleJson;
    private String methodology;
    private String status;
    @TableField("model_version") private String modelVersion;
    @TableField("created_by") private String createdBy;
    @TableField("approved_by") private String approvedBy;
    @TableField("approved_at") private LocalDateTime approvedAt;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
