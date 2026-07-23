package com.rehealth.contracts.telemetry.v1;

import java.util.ArrayList;
import java.util.List;

public final class RecentTelemetryResponse {
    public String userId;
    public int limit;
    public List<MeasurementRecord> measurements = new ArrayList<>();
    public List<SleepSessionRecord> sleepSessions = new ArrayList<>();
    public List<ActivitySessionRecord> activities = new ArrayList<>();
}
