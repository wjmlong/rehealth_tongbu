package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_audit_event")
public class InsuranceAuditEventEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("actor_user_id") private String actorUserId;
    private String action;
    @TableField("resource_type") private String resourceType;
    @TableField("resource_id") private String resourceId;
    @TableField("request_id") private String requestId;
    @TableField("before_hash") private String beforeHash;
    @TableField("after_hash") private String afterHash;
    @TableField("metadata_json") private String metadataJson;
    @TableField("created_at") private LocalDateTime createdAt;
}
