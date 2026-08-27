package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Employee QR binding for insurance-side scan association: one active code
 * per employee per tenant, 30-day validity, refreshable and disableable.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】员工二维码实体-----------
@Data
@TableName("rehealth_insurance_employee_qr")
public class InsuranceEmployeeQrEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("employee_id")
    private String employeeId;
    private String code;
    private String status;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】员工二维码实体-----------
