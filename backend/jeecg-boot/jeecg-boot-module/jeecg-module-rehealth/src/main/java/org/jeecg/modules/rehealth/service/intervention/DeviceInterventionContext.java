package org.jeecg.modules.rehealth.service.intervention;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInterventionContext {
    public Long generatedAt;
    public String localDate;
    public String timeZone;
    public Long latestDataAt;
    public TodayBehavior todayBehavior;
    public List<RecentChange> recentChanges = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TodayBehavior {
        public Integer steps;
        public Integer activeMinutes;
        public Double activityCaloriesKcal;
        public Double averageActivityHeartRate;
        public Integer sleepMinutes;
        public Long sleepEndedAt;
        public List<DietSnapshot> dietRecords = new ArrayList<>();
        public List<MetricSnapshot> measurements = new ArrayList<>();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DietSnapshot {
        public String mealType;
        public String description;
        public Long consumedAt;
        public Double caloriesKcal;
        public Double proteinGrams;
        public Double carbohydrateGrams;
        public Double fatGrams;
        public Double fiberGrams;
        public Double sodiumMilligrams;
        public String source;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetricSnapshot {
        public String metricType;
        public Double latestValue;
        public Double averageValue;
        public Double minimumValue;
        public Double maximumValue;
        public String unit;
        public Integer sampleCount;
        public Long latestObservedAt;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RecentChange {
        public String metricType;
        public String unit;
        public Double recentAverage;
        public Double previousAverage;
        public Double delta;
        public String trend;
        public Integer recentSampleCount;
        public Integer previousSampleCount;
    }
}
