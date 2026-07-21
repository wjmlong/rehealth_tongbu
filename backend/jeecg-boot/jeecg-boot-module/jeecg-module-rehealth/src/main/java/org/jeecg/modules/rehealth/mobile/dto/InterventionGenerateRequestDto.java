package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.LinkedHashMap;
import java.util.Map;

public class InterventionGenerateRequestDto {
    @JSONField(name = "riskResult")
    public RiskEvaluateResponseDto riskResult;
    @JSONField(name = "featureVector")
    public CvdFeatureVectorDto featureVector;
    @JSONField(name = "patientContext")
    public Map<String, Object> patientContext = new LinkedHashMap<>();
}
