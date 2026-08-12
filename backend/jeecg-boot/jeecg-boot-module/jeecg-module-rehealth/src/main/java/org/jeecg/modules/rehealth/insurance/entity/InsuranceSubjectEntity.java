package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_subject")
public class InsuranceSubjectEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("subject_ref")
    private String subjectRef;
    @TableField("rehealth_user_id")
    private String rehealthUserId;
    @TableField("external_subject_ref_hash")
    private String externalSubjectRefHash;
    @TableField("enrollment_status")
    private String enrollmentStatus;
    @TableField("consent_status")
    private String consentStatus;
    @TableField("consent_version")
    private String consentVersion;
    @TableField("consented_at")
    private LocalDateTime consentedAt;
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
