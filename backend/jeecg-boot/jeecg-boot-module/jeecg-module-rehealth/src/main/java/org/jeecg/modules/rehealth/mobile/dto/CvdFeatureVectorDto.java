package org.jeecg.modules.rehealth.mobile.dto;

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
    public Double fastingGlucose;
    @JSONField(name = "total_cholesterol")
    public Double totalCholesterol;
    public Double ldl;
    public Double hdl;
    public Double triglycerides;
    @JSONField(name = "exercise_days")
    public Integer exerciseDays;
    public Integer smoking;
    public Integer drinking;
    @JSONField(name = "diabetes_history")
    public Integer diabetesHistory;
    @JSONField(name = "hypertension_history")
    public Integer hypertensionHistory;
    @JSONField(name = "family_history")
    public Integer familyHistory;
    public Map<String, FeatureQualityDto> featureQuality = new LinkedHashMap<>();
}
