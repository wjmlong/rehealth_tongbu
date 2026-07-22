package org.jeecg.modules.rehealth.mobile.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class CvdFeatureVectorDto {
    public Integer age;
    public Integer gender;
    public Double bmi;
    public Double sbp;
    public Double dbp;
    // Snake_case JSON aliases so the Android C1 contract (CvdFeatureFields.ALL / model-service
    // API_CONTRACT.md) snake_case keys map correctly. Fastjson (JeecgBoot) does NOT auto-map
    // snake_case JSON keys to camelCase Java fields, so without these aliases the App's
    // `exercise_days`, `fasting_glucose`, etc. arrived as null and were dropped before reaching
    // the model-service. The camelCase field names are kept for Java bean convention; the
    // @JSONField(name=...) aliases make both inbound parsing and outbound serialization use the
    // canonical snake_case contract, letting all 16 CVD fields flow App -> backend -> model-service.
    @JSONField(name = "fasting_glucose")
    @JsonProperty("fasting_glucose")
    @JsonAlias("fastingGlucose")
    public Double fastingGlucose;
    @JSONField(name = "total_cholesterol")
    @JsonProperty("total_cholesterol")
    @JsonAlias("totalCholesterol")
    public Double totalCholesterol;
    public Double ldl;
    public Double hdl;
    public Double triglycerides;
    @JSONField(name = "exercise_days")
    @JsonProperty("exercise_days")
    @JsonAlias("exerciseDays")
    public Integer exerciseDays;
    public Integer smoking;
    public Integer drinking;
    @JSONField(name = "diabetes_history")
    @JsonProperty("diabetes_history")
    @JsonAlias("diabetesHistory")
    public Integer diabetesHistory;
    @JSONField(name = "hypertension_history")
    @JsonProperty("hypertension_history")
    @JsonAlias("hypertensionHistory")
    public Integer hypertensionHistory;
    @JSONField(name = "family_history")
    @JsonProperty("family_history")
    @JsonAlias("familyHistory")
    public Integer familyHistory;
    public Map<String, FeatureQualityDto> featureQuality = new LinkedHashMap<>();
}
