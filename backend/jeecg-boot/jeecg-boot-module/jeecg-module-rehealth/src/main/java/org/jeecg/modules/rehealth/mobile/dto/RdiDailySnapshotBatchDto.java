package org.jeecg.modules.rehealth.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RdiDailySnapshotBatchDto {
    public String userId;
    public List<Snapshot> snapshots;

    public static class Snapshot {
        @JsonProperty("scored_on") public String scoredOn;
        @JsonProperty("raw_score") public Double rawScore;
        @JsonProperty("display_score") public Double displayScore;
        @JsonProperty("data_confidence") public Double dataConfidence;
        public String status;
        @JsonProperty("is_mock") public Boolean isMock;
        @JsonProperty("algorithm_version") public String algorithmVersion;
        @JsonProperty("calculation_source") public String calculationSource;
        public List<Contribution> contributions;
    }

    public static class Contribution {
        @JsonProperty("factor_code") public String factorCode;
        public String domain;
        public String source;
        @JsonProperty("current_value") public Double currentValue;
        @JsonProperty("baseline_value") public Double baselineValue;
        public String unit;
        @JsonProperty("raw_points") public Double rawPoints;
        public Double confidence;
        @JsonProperty("final_points") public Double finalPoints;
        @JsonProperty("source_factor_id") public String sourceFactorId;
    }
}
