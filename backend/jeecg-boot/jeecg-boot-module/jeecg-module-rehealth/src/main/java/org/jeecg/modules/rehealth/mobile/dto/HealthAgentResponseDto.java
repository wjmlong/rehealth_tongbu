package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class HealthAgentResponseDto {
    @JSONField(name = "request_id")
    public String requestId;
    public String status;
    public String answer;
    @JSONField(name = "medical_disclaimer")
    public String medicalDisclaimer;
    public String provider;
    @JSONField(name = "model_version")
    public String modelVersion;
    @JSONField(name = "is_demo")
    public Boolean isDemo;
    public Boolean retryable;
}
