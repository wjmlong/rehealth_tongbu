package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RiskEvaluateResponseDto {
    @JSONField(name = "risk_score")
    public Double riskScore;
    @JSONField(name = "risk_level")
    public String riskLevel;
    @JSONField(name = "feature_contributions")
    public Map<String, Double> featureContributions = new LinkedHashMap<>();
    @JSONField(name = "factor_contributions")
    public Map<String, Double> factorContributions = new LinkedHashMap<>();
    @JSONField(name = "factor_contribution_version")
    public String factorContributionVersion;
    @JSONField(name = "factor_measured_components")
    public Map<String, Double> factorMeasuredComponents = new LinkedHashMap<>();
    @JSONField(name = "factor_control_support_components")
    public Map<String, Double> factorControlSupportComponents = new LinkedHashMap<>();
    @JSONField(name = "model_version")
    public String modelVersion;
    @JSONField(name = "is_mock")
    public Boolean isMock;
    @JSONField(name = "missing_fields")
    public List<String> missingFields;
    @JSONField(name = "quality_warnings")
    public List<String> qualityWarnings;
    public String summary;
    //update-begin---author:codex---date:2026-07-10 for：【M1-P0c followup】Add ModelTrace passthrough field to RiskEvaluateResponseDto-----------
    @JSONField(name = "model_trace")
    public ModelTraceDto modelTrace;
    //update-end---author:codex---date:2026-07-10 for：【M1-P0c followup】Add ModelTrace passthrough field to RiskEvaluateResponseDto-----------
}
