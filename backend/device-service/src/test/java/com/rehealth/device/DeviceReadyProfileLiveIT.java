package com.rehealth.device;

import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import com.rehealth.device.port.TelemetryReadPort;
import com.rehealth.device.port.TelemetryWritePort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test-ready")
@SpringBootTest(
        classes = {DeviceServiceApplication.class, DeviceReadyProfileLiveIT.ReadyAdapters.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class DeviceReadyProfileLiveIT {
    private final TestRestTemplate http;

    @Autowired
    DeviceReadyProfileLiveIT(TestRestTemplate http) {
        this.http = http;
    }

    @Test
    void readinessIsUpWhenIdentityAndTelemetryAdaptersAreReady() {
        ResponseEntity<String> response = http.getForEntity("/actuator/health/readiness", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"status\":\"UP\""));
    }

    @TestConfiguration
    static class ReadyAdapters {
        @Bean
        @Primary
        IdentityAuthorizationPort readyIdentity() {
            return (accessToken, tenantId, deviceId) -> new DeviceClaims("test-user", tenantId, deviceId);
        }

        @Bean
        @Primary
        ReadyTelemetryStore readyTelemetryStore() {
            return new ReadyTelemetryStore();
        }
    }

    static final class ReadyTelemetryStore implements TelemetryReadPort, TelemetryWritePort {
        @Override
        public RecentTelemetryResponse recent(DeviceClaims claims, int limit) {
            RecentTelemetryResponse response = new RecentTelemetryResponse();
            response.userId = claims.userId();
            response.limit = limit;
            return response;
        }

        @Override
        public TelemetryBatchResponse write(
                DeviceClaims claims,
                TelemetryBatchRequest request,
                TelemetryValidationResult validation
        ) {
            return new TelemetryBatchResponse();
        }

        @Override
        public boolean ready() {
            return true;
        }
    }
}
