package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_settlement_package")
public class InsuranceSettlementPackageEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("package_no") private String packageNo;
    @TableField("study_id") private String studyId;
    @TableField("report_id") private String reportId;
    @TableField("package_version") private Integer packageVersion;
    private String status;
    private String currency;
    @TableField("estimated_savings") private BigDecimal estimatedSavings;
    @TableField("approved_amount") private BigDecimal approvedAmount;
    @TableField("snapshot_hash") private String snapshotHash;
    @TableField("evidence_manifest_json") private String evidenceManifestJson;
    @TableField("package_json") private String packageJson;
    @TableField("content_hash") private String contentHash;
    @TableField("created_by") private String createdBy;
    @TableField("approved_by") private String approvedBy;
    @TableField("approved_at") private LocalDateTime approvedAt;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
