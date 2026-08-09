package org.jeecg.modules.rehealth.vo;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

/**
 * Tenant-scoped administrative patient view. Deliberately excludes account
 * username, phone and email. Telemetry is populated only by the detail route.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RehealthUserHealthVO {
    private String id;
    private String displayName;
    private Integer sex;
    private Integer status;
    private Date createTime;
    private ProfileSummary profile;
    private RiskSummary latestRisk;
    private JSONObject telemetry;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProfileSummary {
        private String name;
        private String gender;
        private Integer age;
        private Double heightCm;
        private Double weightKg;
        private Double bmi;
        private Boolean familyHistory;
        private Boolean smoking;
        private Boolean drinking;
        private Boolean diabetesHistory;
        private Boolean hypertensionHistory;
        private Date updatedAt;
    }

    @Data
    public static class RiskSummary {
        private Double score;
        private String level;
        private String modelVersion;
        private Date evaluatedAt;
    }
}
