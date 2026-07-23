package com.rehealth.device.adapter;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.TelemetryReadPort;
import com.rehealth.device.port.TelemetryWritePort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UnavailableTelemetryStore implements TelemetryWritePort, TelemetryReadPort {
    @Override
    public TelemetryBatchResponse write(
            DeviceClaims claims,
            TelemetryBatchRequest request,
            TelemetryValidationResult validation
    ) {
        throw unavailable();
    }

    @Override
    public RecentTelemetryResponse recent(DeviceClaims claims, int limit) {
        throw unavailable();
    }

    @Override
    public boolean ready() {
        return false;
    }

    private DeviceRequestException unavailable() {
        return new DeviceRequestException(HttpStatus.SERVICE_UNAVAILABLE, "HARDWARE_PERSISTENCE_UNAVAILABLE");
    }
}
