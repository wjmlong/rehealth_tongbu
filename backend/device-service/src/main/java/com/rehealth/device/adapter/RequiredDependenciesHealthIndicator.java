package com.rehealth.device.adapter;

import com.rehealth.device.port.IdentityAuthorizationPort;
import com.rehealth.device.port.TelemetryReadPort;
import com.rehealth.device.port.TelemetryWritePort;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("requiredDependencies")
public final class RequiredDependenciesHealthIndicator implements HealthIndicator {
    private final IdentityAuthorizationPort identity;
    private final TelemetryReadPort telemetryReader;
    private final TelemetryWritePort telemetryWriter;

    public RequiredDependenciesHealthIndicator(
            IdentityAuthorizationPort identity,
            TelemetryReadPort telemetryReader,
            TelemetryWritePort telemetryWriter
    ) {
        this.identity = identity;
        this.telemetryReader = telemetryReader;
        this.telemetryWriter = telemetryWriter;
    }

    @Override
    public Health health() {
        boolean identityReady = identity.ready();
        boolean telemetryReady = telemetryReader.ready() && telemetryWriter.ready();
        Health.Builder builder = identityReady && telemetryReady
                ? Health.up()
                : Health.outOfService();
        return builder
                .withDetail("identity", state(identityReady))
                .withDetail("telemetryStore", state(telemetryReady))
                .build();
    }

    private String state(boolean ready) {
        return ready ? "UP" : "OUT_OF_SERVICE";
    }
}
