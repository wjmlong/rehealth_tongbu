package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;

public class InterventionGenerateResponseDto {
    @JSONField(name = "plan_id")
    @JsonAlias("plan_id")
    public String planId;
    @JSONField(name = "generated_at")
    @JsonAlias("generated_at")
    public String generatedAt;
    @JSONField(name = "priority_intervention")
    @JsonAlias("priority_intervention")
    public String priorityIntervention;
    public String rationale;
    @JSONField(name = "expected_impact")
    @JsonAlias("expected_impact")
    public String expectedImpact;
    public List<String> contraindications;
    public Double confidence;
    @JSONField(name = "model_version")
    @JsonAlias("model_version")
    public String modelVersion;
    @JSONField(name = "is_mock")
    @JsonAlias("is_mock")
    public Boolean isMock;
    @JSONField(name = "medical_disclaimer")
    @JsonAlias("medical_disclaimer")
    public String medicalDisclaimer;
}
