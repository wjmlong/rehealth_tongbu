package org.jeecg.modules.rehealth.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class RhiDailySnapshotBatchDto {
    public String userId;
    public List<Snapshot> snapshots;

    public static class Snapshot {
        @JsonProperty("scored_on") public String scoredOn;
        @JsonProperty("raw_score") public Double rawScore;
        @JsonProperty("display_score") public Double displayScore;
        @JsonProperty("data_confidence") public Double dataConfidence;
        public String status;
        @JsonProperty("product_tier") public String productTier;
        @JsonProperty("available_days") public Integer availableDays;
        @JsonProperty("available_feature_count") public Integer availableFeatureCount;
        @JsonProperty("smoothing_alpha") public Double smoothingAlpha;
        @JsonProperty("algorithm_version") public String algorithmVersion;
        @JsonProperty("calculation_source") public String calculationSource;
        public JsonNode domains;
        public JsonNode features;
        public JsonNode quality;
    }
}
