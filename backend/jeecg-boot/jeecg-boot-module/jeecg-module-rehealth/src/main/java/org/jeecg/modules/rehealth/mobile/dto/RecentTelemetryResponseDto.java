package org.jeecg.modules.rehealth.mobile.dto;

import java.util.ArrayList;
import java.util.List;

public class RecentTelemetryResponseDto {
    public String userId;
    public int limit;
    public List<Measurement> measurements = new ArrayList<>();
    public List<SleepSession> sleepSessions = new ArrayList<>();
    public List<Activity> activities = new ArrayList<>();

    public static class Measurement {
        public String deviceId;
        public String metricType;
        public Long measuredAt;
        public Double primaryValue;
        public Double secondaryValue;
        public String unit;
        public String qualityCode;
        public String source;
    }

    public static class SleepSession {
        public String deviceId;
        public Long startedAt;
        public Long endedAt;
        public Integer deepMinutes;
        public Integer lightMinutes;
        public Integer awakeMinutes;
        public Integer remMinutes;
        public Integer interruptionMinutes;
        public String source;
    }

    public static class Activity {
        public String deviceId;
        public Long startedAt;
        public Long endedAt;
        public String activityType;
        public Integer steps;
        public Double distanceMeters;
        public Double caloriesKcal;
        public Integer durationMinutes;
        public Double averageHeartRate;
        public String source;
    }
}
