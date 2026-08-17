package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_intervention_feedback")
public class InsuranceInterventionFeedbackEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("binding_id") private String bindingId;
    @TableField("subject_ref") private String subjectRef;
    @TableField("intervention_id") private String interventionId;
    @TableField("plan_item_id") private String planItemId;
    @TableField("feedback_type") private String feedbackType;
    @TableField("occurred_at") private LocalDateTime occurredAt;
    @TableField("completion_rate") private BigDecimal completionRate;
    @TableField("adherence_score") private BigDecimal adherenceScore;
    @TableField("expected_count") private BigDecimal expectedCount;
    @TableField("completed_count") private BigDecimal completedCount;
    @TableField("verification_type") private String verificationType;
    @TableField("calculation_version") private String calculationVersion;
    @TableField("outcome_summary_json") private String outcomeSummaryJson;
    @TableField("source_system") private String sourceSystem;
    @TableField("source_record_id") private String sourceRecordId;
    @TableField("created_at") private LocalDateTime createdAt;
}
