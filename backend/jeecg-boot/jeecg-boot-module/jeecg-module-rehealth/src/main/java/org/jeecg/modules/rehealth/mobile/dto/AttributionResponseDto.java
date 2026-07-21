package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class AttributionResponseDto {
    @JSONField(name = "model_version")
    public String modelVersion;
    @JSONField(name = "trend_delta")
    public Double trendDelta;
    @JSONField(name = "adherence_average")
    public Double adherenceAverage;
    public String interpretation;
}
