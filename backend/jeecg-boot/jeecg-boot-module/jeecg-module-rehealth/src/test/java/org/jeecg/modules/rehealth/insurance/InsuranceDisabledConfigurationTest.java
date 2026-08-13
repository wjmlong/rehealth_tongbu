package org.jeecg.modules.rehealth.insurance;

import org.jeecg.common.api.vo.Result;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsuranceDisabledConfigurationTest {
    @Test
    void softwareDatabaseDisabledStartsWithoutJdbcAndKeepsA503Endpoint() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "insurance-disabled-test",
                    Map.of("rehealth.software-db.enabled", "false")
            ));
            context.scan("org.jeecg.modules.rehealth.insurance");

            assertDoesNotThrow(context::refresh);
            assertTrue(context.getBeansOfType(InsuranceRiskRepository.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceRiskService.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceTenantAccessGuard.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceRiskController.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceImportService.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceStudyService.class).isEmpty());
            assertTrue(context.getBeansOfType(InsuranceMobilePlanService.class).isEmpty());

            DisabledInsuranceRiskController controller = context.getBean(DisabledInsuranceRiskController.class);
            ResponseEntity<Result<InsuranceRiskResponse.Dashboard>> response = controller.dashboard("1000");

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
            assertNotNull(response.getBody());
            assertFalse(response.getBody().isSuccess());
            assertEquals(503, response.getBody().getCode());
        }
    }
}
