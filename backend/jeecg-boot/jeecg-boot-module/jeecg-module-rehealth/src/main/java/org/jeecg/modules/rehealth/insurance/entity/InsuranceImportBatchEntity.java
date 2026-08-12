package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_import_batch")
public class InsuranceImportBatchEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("import_type")
    private String importType;
    @TableField("source_system")
    private String sourceSystem;
    @TableField("idempotency_key")
    private String idempotencyKey;
    @TableField("content_hash")
    private String contentHash;
    private String status;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("success_count")
    private Integer successCount;
    @TableField("failure_count")
    private Integer failureCount;
    @TableField("error_json")
    private String errorJson;
    @TableField("created_by")
    private String createdBy;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("completed_at")
    private LocalDateTime completedAt;
}
