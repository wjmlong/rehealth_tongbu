package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class TelemetryRecord {
    private final Map<String, Object> extensions = new LinkedHashMap<>();

    @JsonAnySetter
    public void putExtension(String name, Object value) {
        extensions.put(name, value);
    }

    @JsonAnyGetter
    public Map<String, Object> extensions() {
        return extensions;
    }
}
