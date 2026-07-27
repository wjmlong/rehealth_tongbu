package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;

public class HealthAgentModelRequestDto {
    @JSONField(name = "request_id")
    public String requestId;
    public String message;
    public String locale;
    public Context context = new Context();

    public static class Context {
        @JSONField(name = "age_band")
        public String ageBand;
        @JSONField(name = "risk_level")
        public String riskLevel;
        @JSONField(name = "risk_score_percent")
        public Double riskScorePercent;
        @JSONField(name = "recommended_action")
        public String recommendedAction;
        public String trend = "unknown";
    }
}
