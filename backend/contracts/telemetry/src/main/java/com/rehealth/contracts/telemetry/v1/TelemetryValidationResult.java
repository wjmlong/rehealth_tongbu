package com.rehealth.contracts.telemetry.v1;

import java.util.List;

public record TelemetryValidationResult(
        List<TelemetryValidationError> errors,
        int measurementCount,
        int sleepSessionCount,
        int activitySessionCount,
        int signalChunkCount
) {
    public TelemetryValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public int recordCount() {
        return measurementCount + sleepSessionCount + activitySessionCount + signalChunkCount;
    }
}
