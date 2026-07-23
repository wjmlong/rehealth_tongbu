package com.rehealth.contracts.telemetry.v1;

public record TelemetryValidationError(String code, String path, String message) {
}
