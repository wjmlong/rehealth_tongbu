package org.jeecg.modules.rehealth.config;

import java.util.Locale;

public enum AttributionMode {
    PIAS,
    DEMO_MOCK;

    public static AttributionMode parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException(
                    "REHEALTH_CONFIG_INVALID_ATTRIBUTION_MODE: expected pias or demo_mock",
                    error
            );
        }

    }

    public String wireValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
