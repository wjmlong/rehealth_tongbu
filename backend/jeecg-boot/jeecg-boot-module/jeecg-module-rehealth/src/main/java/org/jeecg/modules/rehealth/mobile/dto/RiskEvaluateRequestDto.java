package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class RiskEvaluateRequestDto {
    @JSONField(name = "featureVector")
    public CvdFeatureVectorDto featureVector;
    @JSONField(name = "requestId")
    public String requestId;
}
