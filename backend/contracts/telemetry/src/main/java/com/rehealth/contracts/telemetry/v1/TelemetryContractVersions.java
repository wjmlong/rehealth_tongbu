package com.rehealth.contracts.telemetry.v1;

import java.util.Set;

public final class TelemetryContractVersions {
    public static final String LEGACY_ANDROID_V1 = "d2-v1";
    public static final String CURRENT = "telemetry-v1";
    public static final Set<String> SUPPORTED = Set.of(LEGACY_ANDROID_V1, CURRENT);

    private TelemetryContractVersions() {
    }
}
