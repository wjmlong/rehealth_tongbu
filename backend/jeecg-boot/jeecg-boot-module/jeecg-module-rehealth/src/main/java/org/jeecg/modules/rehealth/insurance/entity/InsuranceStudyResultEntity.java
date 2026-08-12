package org.jeecg.modules.rehealth.insurance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rehealth_insurance_study_result")
public class InsuranceStudyResultEntity {
    @TableId(value = "id", type = IdType.INPUT) private String id;
    @TableField("tenant_id") private Integer tenantId;
    @TableField("study_id") private String studyId;
    @TableField("snapshot_id") private String snapshotId;
    @TableField("result_version") private Integer resultVersion;
    private String status;
    @TableField("att_estimate") private BigDecimal attEstimate;
    @TableField("ci_lower") private BigDecimal ciLower;
    @TableField("ci_upper") private BigDecimal ciUpper;
    @TableField("matched_pairs") private Integer matchedPairs;
    @TableField("balance_json") private String balanceJson;
    @TableField("cost_basis_json") private String costBasisJson;
    @TableField("model_version") private String modelVersion;
    @TableField("result_json") private String resultJson;
    @TableField("created_by") private String createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
}
