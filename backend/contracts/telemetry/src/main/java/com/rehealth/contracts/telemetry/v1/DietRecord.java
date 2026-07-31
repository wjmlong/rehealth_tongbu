package com.rehealth.contracts.telemetry.v1;

public final class DietRecord extends TelemetryRecord {
    public String id;
    public Long consumedAt;
    public String mealType;
    public String description;
    public Double caloriesKcal;
    public Double proteinGrams;
    public Double carbohydrateGrams;
    public Double fatGrams;
    public Double fiberGrams;
    public Double sodiumMilligrams;
    public String source;
}
