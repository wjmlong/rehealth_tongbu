package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class FeatureQualityDto {
    public String status;
    public String source;
    @JSONField(name = "observedAt")
    public Long observedAt;
    public String reason;
}
