package com.rehealth.device.port;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.device.domain.DeviceClaims;

public interface TelemetryReadPort {
    RecentTelemetryResponse recent(DeviceClaims claims, int limit);

    default boolean ready() {
        return true;
    }
}
