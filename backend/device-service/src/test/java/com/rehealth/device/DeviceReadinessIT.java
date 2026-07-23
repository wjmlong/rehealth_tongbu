package com.rehealth.device;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeviceReadinessIT {
    private final TestRestTemplate http;

    @Autowired
    DeviceReadinessIT(TestRestTemplate http) {
        this.http = http;
    }

    @Test
    void readinessFailsClosedWhenRequiredAdaptersAreUnavailable() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"OUT_OF_SERVICE\""));
    }
}
