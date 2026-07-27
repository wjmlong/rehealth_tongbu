package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.annotation.JsonAlias;

public final class ActivitySessionRecord extends TelemetryRecord {
    public String id;
    @JsonAlias("started_at") public Long startedAt;
    @JsonAlias("ended_at") public Long endedAt;
    @JsonAlias("activity_type") public String activityType;
    public Integer steps;
    @JsonAlias("distance_meters") public Double distanceMeters;
    @JsonAlias({"calories_kcal", "calories"}) public Double caloriesKcal;
    @JsonAlias("duration_minutes") public Integer durationMinutes;
    @JsonAlias("average_heart_rate") public Double averageHeartRate;
    public String source;
}
