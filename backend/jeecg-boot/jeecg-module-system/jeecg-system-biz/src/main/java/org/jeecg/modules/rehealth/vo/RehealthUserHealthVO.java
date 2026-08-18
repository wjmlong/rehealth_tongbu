package org.jeecg.modules.rehealth.vo;

import com.alibaba.fastjson.JSONArray;
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
    private RhiSummary latestRhi;
    private RdiSummary latestRdi;
    private InterventionSummary latestIntervention;
    /**
     * `unknown` on list rows because the list deliberately performs no per-user
     * telemetry calls. Detail responses replace it with `verified_real` or
     * `synthetic` from the tenant-scoped Device Service summary.
     */
    private String provenanceStatus = "unknown";
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
        private Boolean isMock;
        private JSONArray factorContributions;
    }

    @Data
    public static class RhiSummary {
        private Double displayScore;
        private Double dataConfidence;
        private String status;
        private Date scoredOn;
        private String algorithmVersion;
        private String calculationSource;
        private Boolean isMock;
    }

    @Data
    public static class RdiSummary {
        private Double displayScore;
        private Double dataConfidence;
        private String status;
        private Date scoredOn;
        private String algorithmVersion;
        private String calculationSource;
        private Boolean isMock;
    }

    @Data
    public static class InterventionSummary {
        private String priorityIntervention;
        private String rationale;
        private String expectedImpact;
        private Double confidence;
        private String modelVersion;
        private Date generatedAt;
        private Boolean isMock;
        private String medicalDisclaimer;
    }
}
