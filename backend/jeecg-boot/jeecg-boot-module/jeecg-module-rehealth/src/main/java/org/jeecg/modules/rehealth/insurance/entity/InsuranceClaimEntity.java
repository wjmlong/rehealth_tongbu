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
@TableName("rehealth_insurance_claim")
public class InsuranceClaimEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("claim_no")
    private String claimNo;
    @TableField("policy_id")
    private String policyId;
    @TableField("subject_ref")
    private String subjectRef;
    @TableField("claim_type")
    private String claimType;
    @TableField("event_on")
    private LocalDate eventOn;
    @TableField("submitted_at")
    private LocalDateTime submittedAt;
    @TableField("decided_at")
    private LocalDateTime decidedAt;
    private String status;
    @TableField("billed_amount")
    private BigDecimal billedAmount;
    @TableField("approved_amount")
    private BigDecimal approvedAmount;
    @TableField("paid_amount")
    private BigDecimal paidAmount;
    private String currency;
    @TableField("coverage_code")
    private String coverageCode;
    @TableField("outcome_code")
    private String outcomeCode;
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
