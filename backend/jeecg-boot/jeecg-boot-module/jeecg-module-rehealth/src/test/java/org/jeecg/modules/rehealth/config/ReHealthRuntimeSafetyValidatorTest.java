package org.jeecg.modules.rehealth.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReHealthRuntimeSafetyValidatorTest {
    @Test
    void acceptsSafeProductionAndStagingConfigurations() {
        assertDoesNotThrow(() -> validate(safeConfiguration("production")));
        assertDoesNotThrow(() -> validate(safeConfiguration("staging")));
    }

    @Test
    void acceptsExplicitDevelopmentAndDemoConfigurations() {
        assertDoesNotThrow(() -> validate(Map.of(
                "rehealth.runtime.mode", "development",
                "rehealth.attribution.mode", "pias"
        )));
        assertDoesNotThrow(() -> validate(Map.of(
                "rehealth.runtime.mode", "demo",
                "rehealth.demo.enabled", "true",
                "rehealth.attribution.mode", "demo_mock",
                "rehealth.attribution.provenance", "demo_mock"
        )));
    }

    @Test
    void rejectsDemoAttributionInProduction() {
        Map<String, Object> properties = safeConfiguration("production");
        properties.put("rehealth.attribution.mode", "demo_mock");

        assertRejected(properties, "ATTRIBUTION_MODE_UNSAFE");
    }

    @Test
    void rejectsDisabledSoftwarePersistenceInProduction() {
        Map<String, Object> properties = safeConfiguration("production");
        properties.put("rehealth.software-db.enabled", "false");

        assertRejected(properties, "SOFTWARE_DB_REQUIRED");
    }

    @Test
    void rejectsExternalHttpServiceUrlInProduction() {
        Map<String, Object> properties = safeConfiguration("production");
        properties.put("rehealth.model-service.base-url", "http://models.example.com");

        assertRejected(properties, "SECURE_URL_REQUIRED");
    }

    @Test
    void rejectsEmbeddedProviderSecretInProduction() {
        Map<String, Object> properties = safeConfiguration("production");
        properties.put("rehealth.provider-credentials.embedded-secret", "do-not-ship");

        assertRejected(properties, "EMBEDDED_SECRET_FORBIDDEN");
    }

    private static Map<String, Object> safeConfiguration(String mode) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("rehealth.runtime.mode", mode);
        properties.put("rehealth.software-db.enabled", "true");
        properties.put("rehealth.device-service.enabled", "true");
        properties.put("rehealth.timescale.enabled", "true");
        properties.put("rehealth.model-service.require-real-model", "true");
        properties.put("rehealth.model-service.base-url", "https://model.internal.example");
        properties.put("rehealth.device-service.base-url", "https://device.internal.example");
        properties.put("rehealth.attribution-service.base-url", "https://pias.internal.example");
        properties.put("rehealth.attribution-service.internal-token", "synthetic-test-token");
        properties.put("rehealth.attribution.mode", "pias");
        return properties;
    }

    private static void validate(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        ReHealthRuntimeSafetyValidator.validate(environment);
    }

    private static void assertRejected(Map<String, Object> properties, String code) {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> validate(properties));
        assertTrue(error.getMessage().contains(code));
    }
}
