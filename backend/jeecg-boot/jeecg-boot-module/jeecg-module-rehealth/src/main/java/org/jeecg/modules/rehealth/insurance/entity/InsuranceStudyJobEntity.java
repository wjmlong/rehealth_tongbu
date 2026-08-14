package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_study_job")
public class InsuranceStudyJobEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("study_id") private String studyId;
    @TableField("snapshot_id") private String snapshotId;
    @TableField("job_type") private String jobType;
    private String status;
    @TableField("request_id") private String requestId;
    private Integer attempt;
    @TableField("error_message") private String errorMessage;
    @TableField("result_id") private String resultId;
    @TableField("created_by") private String createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("started_at") private LocalDateTime startedAt;
    @TableField("finished_at") private LocalDateTime finishedAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
