package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Link between a basic insurance policy and an APP user (subject): staff
 * "adds" a policy to a user here. One policy may link to many users.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】保单-用户关联实体-----------
@Data
@TableName("rehealth_insurance_policy_link")
public class InsurancePolicyLinkEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("policy_no")
    private String policyNo;
    @TableField("subject_ref")
    private String subjectRef;
    private String status;
    @TableField("source_system")
    private String sourceSystem;
    @TableField("source_record_id")
    private String sourceRecordId;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧基础保单库】保单-用户关联实体-----------
