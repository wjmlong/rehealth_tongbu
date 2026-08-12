package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_plan_binding")
public class InsurancePlanBindingEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("subject_ref") private String subjectRef;
    @TableField("policy_id") private String policyId;
    @TableField("plan_id") private String planId;
    @TableField("consent_id") private String consentId;
    private String status;
    @TableField("bound_at") private LocalDateTime boundAt;
    @TableField("unbound_at") private LocalDateTime unboundAt;
    @TableField("source_system") private String sourceSystem;
    @TableField("source_record_id") private String sourceRecordId;
    @TableField("metadata_json") private String metadataJson;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
