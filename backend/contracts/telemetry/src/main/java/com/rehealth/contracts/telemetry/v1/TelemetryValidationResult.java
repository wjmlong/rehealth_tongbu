package com.rehealth.contracts.telemetry.v1;

import java.util.List;

public record TelemetryValidationResult(
        List<TelemetryValidationError> errors,
        int measurementCount,
        int sleepSessionCount,
        int activitySessionCount,
        int signalChunkCount,
        int dietRecordCount
) {
    public TelemetryValidationResult(
            List<TelemetryValidationError> errors,
            int measurementCount,
            int sleepSessionCount,
            int activitySessionCount,
            int signalChunkCount
    ) {
        this(
                errors,
                measurementCount,
                sleepSessionCount,
                activitySessionCount,
                signalChunkCount,
                0
        );
    }

    public TelemetryValidationResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() {
        return errors.isEmpty();
    }

    public int recordCount() {
        return measurementCount
                + sleepSessionCount
                + activitySessionCount
                + signalChunkCount
                + dietRecordCount;
    }
}
