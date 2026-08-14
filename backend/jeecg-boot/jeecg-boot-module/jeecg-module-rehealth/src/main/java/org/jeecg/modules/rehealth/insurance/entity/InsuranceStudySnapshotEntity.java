package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_study_snapshot")
public class InsuranceStudySnapshotEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("study_id") private String studyId;
    @TableField("snapshot_version") private Integer snapshotVersion;
    @TableField("snapshot_hash") private String snapshotHash;
    @TableField("source_watermark") private String sourceWatermark;
    @TableField("cohort_total") private Integer cohortTotal;
    @TableField("treated_total") private Integer treatedTotal;
    @TableField("control_total") private Integer controlTotal;
    @TableField("source_summary_json") private String sourceSummaryJson;
    private Boolean immutable;
    @TableField("created_by") private String createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
}
