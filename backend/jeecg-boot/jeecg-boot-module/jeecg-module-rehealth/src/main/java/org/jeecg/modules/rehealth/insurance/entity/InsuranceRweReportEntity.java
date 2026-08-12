package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_rwe_report")
public class InsuranceRweReportEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("report_no") private String reportNo;
    @TableField("study_id") private String studyId;
    @TableField("report_type") private String reportType;
    @TableField("report_version") private Integer reportVersion;
    private String title;
    @TableField("period_start") private LocalDate periodStart;
    @TableField("period_end") private LocalDate periodEnd;
    private String status;
    @TableField("evidence_hash") private String evidenceHash;
    @TableField("report_json") private String reportJson;
    @TableField("created_by") private String createdBy;
    @TableField("submitted_at") private LocalDateTime submittedAt;
    @TableField("approved_by") private String approvedBy;
    @TableField("approved_at") private LocalDateTime approvedAt;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
