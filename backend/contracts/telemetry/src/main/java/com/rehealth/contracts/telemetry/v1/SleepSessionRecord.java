package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.annotation.JsonAlias;

public final class SleepSessionRecord extends TelemetryRecord {
    public String id;
    @JsonAlias("started_at") public Long startedAt;
    @JsonAlias("ended_at") public Long endedAt;
    @JsonAlias("deep_minutes") public Integer deepMinutes;
    @JsonAlias("light_minutes") public Integer lightMinutes;
    @JsonAlias("awake_minutes") public Integer awakeMinutes;
    @JsonAlias("rem_minutes") public Integer remMinutes;
    @JsonAlias("interruption_minutes") public Integer interruptionMinutes;
    public String source;
}
