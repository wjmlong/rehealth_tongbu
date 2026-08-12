package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_settlement_approval")
public class InsuranceSettlementApprovalEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("package_id") private String packageId;
    private String action;
    private String comment;
    @TableField("actor_user_id") private String actorUserId;
    @TableField("request_id") private String requestId;
    @TableField("created_at") private LocalDateTime createdAt;
}
