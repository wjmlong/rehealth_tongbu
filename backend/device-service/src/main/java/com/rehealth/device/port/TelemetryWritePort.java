package com.rehealth.device.port;

import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.domain.DeviceClaims;

public interface TelemetryWritePort {
    TelemetryBatchResponse write(
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation
    );

    default boolean ready() {
        return true;
    }
}
