package org.jeecg.modules.rehealth.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Component
public final class ReHealthRuntimeSafetyValidator implements InitializingBean {
    private final Environment environment;

    public ReHealthRuntimeSafetyValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        validate(environment);
    }

    static void validate(Environment environment) {
        RuntimeMode runtimeMode = RuntimeMode.parse(property(environment, "rehealth.runtime.mode", "development"));
        AttributionMode attributionMode = AttributionMode.parse(property(
                environment,
                "rehealth.attribution.mode",
                "pias"
        ));

        if (runtimeMode.isProtected()) {
            requireEnabled(environment, "rehealth.software-db.enabled", "SOFTWARE_DB_REQUIRED");
            requireEnabled(environment, "rehealth.device-service.enabled", "DEVICE_SERVICE_REQUIRED");
            requireEnabled(environment, "rehealth.timescale.enabled", "TIMESCALE_REQUIRED");
            requireEnabled(environment, "rehealth.model-service.require-real-model", "REAL_MODEL_REQUIRED");
            if (attributionMode != AttributionMode.PIAS) {
                reject("ATTRIBUTION_MODE_UNSAFE", "demo_mock attribution is forbidden in production and staging");
            }
            for (String propertyName : List.of(
                    "rehealth.model-service.base-url",
                    "rehealth.device-service.base-url",
                    "rehealth.attribution-service.base-url"
            )) {
                requireSecureUrl(environment, propertyName);
            }
            if (property(environment, "rehealth.attribution-service.internal-token", "").isBlank()
                    && property(environment, "rehealth.attribution-service.internal-token-file", "").isBlank()) {
                reject(
                        "PIAS_INTERNAL_TOKEN_REQUIRED",
                        "rehealth.attribution-service.internal-token is required in production and staging"
                );
            }
            if (!property(environment, "rehealth.provider-credentials.embedded-secret", "").isBlank()) {
                reject("EMBEDDED_SECRET_FORBIDDEN", "provider credentials must come from an external secret store");
            }
        }

        if (runtimeMode == RuntimeMode.DEMO && !enabled(environment, "rehealth.demo.enabled")) {
            reject("DEMO_FLAG_REQUIRED", "demo runtime requires rehealth.demo.enabled=true");
        }
        if (attributionMode == AttributionMode.DEMO_MOCK) {
            if (!enabled(environment, "rehealth.demo.enabled")) {
                reject("DEMO_FLAG_REQUIRED", "demo_mock attribution requires rehealth.demo.enabled=true");
            }
            if (!"demo_mock".equals(property(environment, "rehealth.attribution.provenance", ""))) {
                reject("DEMO_PROVENANCE_REQUIRED", "demo_mock attribution must expose demo_mock provenance");
            }
        }
    }

    private static void requireEnabled(Environment environment, String propertyName, String code) {
        if (!enabled(environment, propertyName)) {
            reject(code, propertyName + " must be true in production and staging");
        }
    }

    private static void requireSecureUrl(Environment environment, String propertyName) {
        String value = property(environment, propertyName, "");
        try {
            URI uri = URI.create(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
                reject("SECURE_URL_REQUIRED", propertyName + " must be an HTTPS URL without embedded credentials");
            }
        } catch (IllegalArgumentException error) {
            reject("SECURE_URL_REQUIRED", propertyName + " must be a valid HTTPS URL");
        }
    }

    private static boolean enabled(Environment environment, String propertyName) {
        return Boolean.parseBoolean(property(environment, propertyName, "false"));
    }

    private static String property(Environment environment, String name, String fallback) {
        return environment.getProperty(name, fallback).trim();
    }

    private static void reject(String code, String detail) {
        throw new IllegalStateException("REHEALTH_CONFIG_" + code + ": " + detail);
    }

    private enum RuntimeMode {
        PRODUCTION,
        STAGING,
        DEVELOPMENT,
        DEMO;

        private static RuntimeMode parse(String value) {
            try {
                return valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException error) {
                throw new IllegalStateException(
                        "REHEALTH_CONFIG_INVALID_RUNTIME_MODE: expected production, staging, development, or demo",
                        error
                );
            }
        }

        private boolean isProtected() {
            return this == PRODUCTION || this == STAGING;
        }
    }

}
