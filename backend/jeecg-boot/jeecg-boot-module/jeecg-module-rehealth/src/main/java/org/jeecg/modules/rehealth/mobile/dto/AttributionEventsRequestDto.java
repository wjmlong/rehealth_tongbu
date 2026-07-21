package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class AttributionEventsRequestDto {
    public List<AttributionEventDto> events = new ArrayList<>();
    @JSONField(name = "baselineRiskScore")
    public Double baselineRiskScore;

    public static class AttributionEventDto {
        public String date;
        @JSONField(name = "risk_score")
        public Double riskScore;
        @JSONField(name = "intervention_id")
        public String interventionId;
        public Double adherence;
    }
}
