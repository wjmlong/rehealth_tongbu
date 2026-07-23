package com.rehealth.contracts.telemetry.v1;

public record TelemetryValidationPolicy(int maxRecordsPerBatch, boolean rawSignalUploadEnabled) {
    public TelemetryValidationPolicy {
        if (maxRecordsPerBatch < 1) {
            throw new IllegalArgumentException("maxRecordsPerBatch must be positive");
        }
    }

    public static TelemetryValidationPolicy productionDefault() {
        return new TelemetryValidationPolicy(5_000, false);
    }
}
