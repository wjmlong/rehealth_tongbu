package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_policy")
public class InsurancePolicyEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("policy_no")
    private String policyNo;
    @TableField("product_code")
    private String productCode;
    @TableField("product_name")
    private String productName;
    @TableField("policy_type")
    private String policyType;
    @TableField("default_plan_id")
    private String defaultPlanId;
    @TableField("policyholder_subject_ref")
    private String policyholderSubjectRef;
    @TableField("insured_subject_ref")
    private String insuredSubjectRef;
    //update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】分配时间-----------
    @TableField("assigned_at")
    private LocalDateTime assignedAt;
    //update-end---author:ai-agent ---date:2026-08-26  for：【保险侧两步式保单派发】分配时间-----------
    @TableField("coverage_amount")
    private BigDecimal coverageAmount;
    @TableField("premium_amount")
    private BigDecimal premiumAmount;
    @TableField("deductible_amount")
    private BigDecimal deductibleAmount;
    @TableField("waiting_period_days")
    private Integer waitingPeriodDays;
    @TableField("effective_on")
    private LocalDate effectiveOn;
    @TableField("expires_on")
    private LocalDate expiresOn;
    private String status;
    @TableField("source_system")
    private String sourceSystem;
    @TableField("source_record_id")
    private String sourceRecordId;
    @TableField("metadata_json")
    private String metadataJson;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
