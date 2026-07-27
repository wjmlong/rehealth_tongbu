package com.rehealth.device;

import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import com.rehealth.device.port.TelemetryReadPort;
import com.rehealth.device.port.TelemetryWritePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {DeviceServiceApplication.class, DeviceApiBoundaryIT.Fakes.class})
@AutoConfigureMockMvc
class DeviceApiBoundaryIT {
    private static final Set<String> SUPPORTED_CASES = Set.of("unauthenticated", "cross_user", "malformed");

    private final MockMvc mvc;
    private final FakeTelemetryStore store;

    @Autowired
    DeviceApiBoundaryIT(MockMvc mvc, FakeTelemetryStore store) {
        this.mvc = mvc;
        this.store = store;
    }

    @BeforeEach
    void resetStore() {
        store.reset();
    }

    @Test
    void acceptsAuthenticatedMixedBatchAndScopesRecentRead() throws Exception {
        runDefaultCase();
        mvc.perform(post("/rehealth/mobile/measurements/batch")
                        .header("X-Access-Token", "owner-token")
                        .header("X-Tenant-Id", "tenant-a")
                        .contentType("application/json")
                        .content(mixedBatch("ring-owner")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.result.accepted").value(true))
                .andExpect(jsonPath("$.result.persisted").value(true))
                .andExpect(jsonPath("$.result.recordCount").value(3));

        mvc.perform(get("/rehealth/mobile/measurements/recent")
                        .header("X-Access-Token", "owner-token")
                        .header("X-Tenant-Id", "tenant-a")
                        .header("X-ReHealth-Device-Id", "ring-owner")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").value("user-owner"))
                .andExpect(jsonPath("$.result.measurements.length()").value(1));
        assertEquals(1, store.writeCount());
    }

    @Test
    void readinessIsUpWhenRequiredAdaptersAreReady() throws Exception {
        runDefaultCase();
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void rejectsUnauthenticatedWithoutWriting() throws Exception {
        runCase("unauthenticated");
        mvc.perform(post("/rehealth/mobile/measurements/batch")
                        .header("X-Tenant-Id", "tenant-a")
                        .contentType("application/json")
                        .content(mixedBatch("ring-owner")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("USER_TOKEN_REQUIRED"));
        assertEquals(0, store.writeCount());
    }

    @Test
    void rejectsCrossUserDeviceWithoutWriting() throws Exception {
        runCase("cross_user");
        mvc.perform(post("/rehealth/mobile/measurements/batch")
                        .header("X-Access-Token", "owner-token")
                        .header("X-Tenant-Id", "tenant-a")
                        .contentType("application/json")
                        .content(mixedBatch("ring-other-user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("DEVICE_BINDING_REJECTED"));
        assertEquals(0, store.writeCount());
    }

    @Test
    void rejectsMalformedJsonWithoutWriting() throws Exception {
        runCase("malformed");
        mvc.perform(post("/rehealth/mobile/measurements/batch")
                        .header("X-Access-Token", "owner-token")
                        .header("X-Tenant-Id", "tenant-a")
                        .contentType("application/json")
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("MALFORMED_REQUEST"));
        assertEquals(0, store.writeCount());
    }

    private String mixedBatch(String deviceId) {
        return """
                {
                  "schemaVersion":"telemetry-v1",
                  "batchId":"batch-1",
                  "deviceId":"%s",
                  "source":"MRD_RING",
                  "measurements":[{"metricType":"heart_rate","measuredAt":1721000000000,"primaryValue":72.0,"unit":"bpm"}],
                  "sleepSessions":[{"startedAt":1720915200000,"endedAt":1720944000000,"deepMinutes":80}],
                  "activitySessions":[{"startedAt":1721000000000,"activityType":"walking","steps":1200}]
                }
                """.formatted(deviceId);
    }

    private void runCase(String caseName) {
        Set<String> selected = selectedCases();
        assumeTrue(selected.isEmpty() || selected.contains(caseName), () -> "case not selected: " + caseName);
    }

    private void runDefaultCase() {
        assumeTrue(selectedCases().isEmpty(), "default happy case excluded by -Dcases");
    }

    private static Set<String> selectedCases() {
        String cases = System.getProperty("cases", "").trim();
        if (cases.isEmpty()) {
            return Set.of();
        }
        Set<String> selected = Arrays.stream(cases.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        if (!SUPPORTED_CASES.containsAll(selected)) {
            throw new IllegalArgumentException("Unsupported -Dcases values: " + selected);
        }
        return selected;
    }

    @TestConfiguration
    static class Fakes {
        @Bean
        @Primary
        IdentityAuthorizationPort fakeIdentityAuthorization() {
            return (accessToken, tenantId, deviceId) -> {
                if (!"owner-token".equals(accessToken)) {
                    throw new DeviceRequestException(HttpStatus.UNAUTHORIZED, "USER_TOKEN_REJECTED");
                }
                if (!"tenant-a".equals(tenantId)) {
                    throw new DeviceRequestException(HttpStatus.FORBIDDEN, "TENANT_MEMBERSHIP_REJECTED");
                }
                if (!"ring-owner".equals(deviceId)) {
                    throw new DeviceRequestException(HttpStatus.FORBIDDEN, "DEVICE_BINDING_REJECTED");
                }
                return new DeviceClaims("user-owner", tenantId, deviceId);
            };
        }

        @Bean
        @Primary
        FakeTelemetryStore fakeTelemetryStore() {
            return new FakeTelemetryStore();
        }
    }

    static final class FakeTelemetryStore implements TelemetryWritePort, TelemetryReadPort {
        private final AtomicInteger writes = new AtomicInteger();
        private TelemetryBatchRequest latest;

        @Override
        public TelemetryBatchResponse write(
                DeviceClaims claims,
                TelemetryBatchRequest request,
                TelemetryValidationResult validation
        ) {
            latest = request;
            writes.incrementAndGet();
            TelemetryBatchResponse response = new TelemetryBatchResponse();
            response.batchId = request.batchId;
            response.receiptId = "receipt-" + request.batchId;
            response.status = "ACCEPTED";
            response.accepted = true;
            response.persisted = true;
            response.ingestStage = "PERSISTED";
            response.ingestMode = "test-port";
            response.recordCount = validation.recordCount();
            response.measurementCount = validation.measurementCount();
            response.sleepSessionCount = validation.sleepSessionCount();
            response.activitySessionCount = validation.activitySessionCount();
            return response;
        }

        @Override
        public RecentTelemetryResponse recent(DeviceClaims claims, int limit) {
            RecentTelemetryResponse response = new RecentTelemetryResponse();
            response.userId = claims.userId();
            response.limit = limit;
            if (latest != null) {
                response.measurements.addAll(latest.measurements);
                response.sleepSessions.addAll(latest.sleepSessions);
                response.activities.addAll(latest.activitySessions);
            }
            return response;
        }

        @Override
        public boolean ready() {
            return true;
        }

        int writeCount() {
            return writes.get();
        }

        void reset() {
            writes.set(0);
            latest = null;
        }
    }
}
