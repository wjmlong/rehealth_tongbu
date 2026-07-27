package com.rehealth.contracts.telemetry.v1;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TelemetryBatchRequest {
    @JsonAlias("schema_version") public String schemaVersion;
    public String batchId;
    public String userId;
    public String deviceId;
    public Long collectedFrom;
    public Long collectedTo;
    public String source;
    public List<MeasurementRecord> measurements = new ArrayList<>();
    public List<SleepSessionRecord> sleepSessions = new ArrayList<>();
    public List<ActivitySessionRecord> activitySessions = new ArrayList<>();
    public List<Map<String, Object>> signalChunks = new ArrayList<>();
    public Map<String, Object> quality = new LinkedHashMap<>();

    public String resolvedSchemaVersion() {
        if (schemaVersion != null && !schemaVersion.isBlank()) {
            return schemaVersion;
        }
        Object legacyVersion = quality == null ? null : quality.get("schemaVersion");
        if (legacyVersion == null && quality != null) {
            legacyVersion = quality.get("schema_version");
        }
        return legacyVersion == null ? TelemetryContractVersions.LEGACY_ANDROID_V1 : String.valueOf(legacyVersion);
    }
}
