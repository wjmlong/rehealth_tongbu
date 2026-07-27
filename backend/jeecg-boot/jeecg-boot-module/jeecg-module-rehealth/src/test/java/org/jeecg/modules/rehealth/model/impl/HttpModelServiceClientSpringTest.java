package org.jeecg.modules.rehealth.model.impl;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class HttpModelServiceClientSpringTest {
    @Test
    void springCreatesClientWhenTestConstructorsAlsoExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "modelServiceClientTest",
                    Map.of(
                            "rehealth.model-service.base-url", "http://127.0.0.1:8000",
                            "rehealth.attribution-service.base-url", "http://127.0.0.1:8010",
                            "rehealth.model-service.timeout-seconds", "2",
                            "rehealth.attribution.mode", "pias",
                            "rehealth.attribution-service.internal-token", "test-only-token",
                            "rehealth.attribution-service.internal-token-file", ""
                    )
            ));
            context.register(HttpModelServiceClient.class);

            context.refresh();

            assertNotNull(context.getBean(HttpModelServiceClient.class));
        }
    }
}
