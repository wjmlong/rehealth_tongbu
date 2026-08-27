package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Scan session between an APP user scanning an employee QR and confirming
 * the service relationship. One-shot: pending → confirmed/cancelled/expired.
 */
//update-begin---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码会话实体-----------
@Data
@TableName("rehealth_insurance_scan_session")
public class InsuranceScanSessionEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("tenant_id")
    private Integer tenantId;
    @TableField("employee_id")
    private String employeeId;
    @TableField("qr_id")
    private String qrId;
    @TableField("user_id")
    private String userId;
    private String status;
    @TableField("expires_at")
    private LocalDateTime expiresAt;
    @TableField("confirmed_at")
    private LocalDateTime confirmedAt;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
//update-end---author:ai-agent ---date:2026-08-26  for：【保险侧扫码关联】扫码会话实体-----------
