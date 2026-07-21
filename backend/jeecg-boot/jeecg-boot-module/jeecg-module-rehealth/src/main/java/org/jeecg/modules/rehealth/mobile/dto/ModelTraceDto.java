package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

//update-begin---author:codex---date:2026-07-10 for：【M1-P0c followup】Add ModelTrace passthrough DTO for RiskEvaluateResponseDto-----------
public class ModelTraceDto {
    @JSONField(name = "feature_schema_version")
    public String featureSchemaVersion;
    @JSONField(name = "model_version")
    public String modelVersion;
    @JSONField(name = "artifact_name")
    public String artifactName;
    @JSONField(name = "scorer_mode")
    public String scorerMode;
    @JSONField(name = "fallback_reason")
    public String fallbackReason;
    @JSONField(name = "request_id")
    public String requestId;
}
//update-end---author:codex---date:2026-07-10 for：【M1-P0c followup】Add ModelTrace passthrough DTO for RiskEvaluateResponseDto-----------
