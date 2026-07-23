package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.annotation.JsonAlias;

public final class MeasurementRecord extends TelemetryRecord {
    public String id;
    @JsonAlias("metric_type") public String metricType;
    @JsonAlias("measured_at") public Long measuredAt;
    @JsonAlias({"primary_value", "value", "value_num"}) public Double primaryValue;
    @JsonAlias("secondary_value") public Double secondaryValue;
    public String unit;
    @JsonAlias({"quality", "quality_code"}) public String qualityCode;
    public String source;
}
