package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class InterventionGenerateResponseDto {
    @JSONField(name = "plan_id")
    public String planId;
    @JSONField(name = "generated_at")
    public String generatedAt;
    @JSONField(name = "priority_intervention")
    public String priorityIntervention;
    public String rationale;
    @JSONField(name = "expected_impact")
    public String expectedImpact;
    public List<String> contraindications;
    public Double confidence;
    @JSONField(name = "model_version")
    public String modelVersion;
    @JSONField(name = "is_mock")
    public Boolean isMock;
    @JSONField(name = "medical_disclaimer")
    public String medicalDisclaimer;
}
