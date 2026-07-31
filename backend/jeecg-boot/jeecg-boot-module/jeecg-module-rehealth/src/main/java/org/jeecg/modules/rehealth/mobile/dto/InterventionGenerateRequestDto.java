package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.LinkedHashMap;
import java.util.Map;

public class InterventionGenerateRequestDto {
    @JSONField(name = "request_id")
    @JsonAlias({"request_id", "requestId"})
    public String requestId;
    /**
     * Backward-compatible transport fields. Personalized generation ignores these
     * client-provided values and reloads authorized server-side context every time.
     */
    @JSONField(name = "riskResult")
    public RiskEvaluateResponseDto riskResult;
    @JSONField(name = "featureVector")
    public CvdFeatureVectorDto featureVector;
    @JSONField(name = "patientContext")
    public Map<String, Object> patientContext = new LinkedHashMap<>();
}
