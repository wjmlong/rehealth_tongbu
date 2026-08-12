package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_consent")
public class InsuranceConsentRecordEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("subject_ref") private String subjectRef;
    @TableField("consent_type") private String consentType;
    @TableField("consent_version") private String consentVersion;
    private String status;
    @TableField("granted_at") private LocalDateTime grantedAt;
    @TableField("revoked_at") private LocalDateTime revokedAt;
    @TableField("evidence_ref") private String evidenceRef;
    @TableField("evidence_hash") private String evidenceHash;
    @TableField("source_system") private String sourceSystem;
    @TableField("source_record_id") private String sourceRecordId;
    @TableField("metadata_json") private String metadataJson;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
