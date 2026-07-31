package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.List;

public class BehaviorRecordDto {
    public String id;
    public String requestId;
    public String category;
    public String title;
    public String summary;
    public List<String> items = new ArrayList<>();
    public Double caloriesKcal;
    public Double proteinGrams;
    public Double carbohydrateGrams;
    public Double fatGrams;
    public String ocrText;
    public Double confidence;
    public String modelVersion;
    public Long occurredAt;
    public Long createdAt;
}
