package com.rehealth.device.port;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.device.application.InterventionTelemetryContext;
import com.rehealth.device.application.UserHealthSummary;
import com.rehealth.device.domain.DeviceClaims;

import java.time.ZoneId;

public interface TelemetryReadPort {
    RecentTelemetryResponse recent(DeviceClaims claims, int limit);

    /**
     * Aggregated, user-scoped health summary across all devices and tenants.
     * Intended for administrative/diagnostic callers only.
     */
    default UserHealthSummary healthSummaryForUser(String userId) {
        throw new UnsupportedOperationException("user health summary is not supported");
    }

    default InterventionTelemetryContext interventionContext(
            String tenantId,
            String userId,
            ZoneId timeZone
    ) {
        throw new UnsupportedOperationException("intervention context is not supported");
    }

    default boolean ready() {
        return true;
    }
}
