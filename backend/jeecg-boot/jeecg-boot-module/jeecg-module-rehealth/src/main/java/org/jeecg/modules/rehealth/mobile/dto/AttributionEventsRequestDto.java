package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class AttributionEventsRequestDto {
    @JSONField(name = "risk_history")
    @JsonProperty("risk_history")
    @JsonAlias("riskHistory")
    public List<AttributionHistoryPointDto> riskHistory = new ArrayList<>();
    @JSONField(name = "forecast_days")
    @JsonProperty("forecast_days")
    @JsonAlias("forecastDays")
    public Integer forecastDays = 30;
    public String language = "zh";

    public static class AttributionHistoryPointDto {
        public String date;
        @JSONField(name = "Y")
        @JsonProperty("Y")
        @JsonAlias({"risk_score", "riskScore"})
        public Double riskScore;
        @JSONField(name = "Z")
        @JsonProperty("Z")
        @JsonAlias({"intervention", "is_intervention_day", "isInterventionDay"})
        public Integer intervention;
    }
}
