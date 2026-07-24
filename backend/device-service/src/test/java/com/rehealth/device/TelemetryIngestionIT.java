package com.rehealth.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehealth.contracts.telemetry.v1.ActivitySessionRecord;
import com.rehealth.contracts.telemetry.v1.MeasurementRecord;
import com.rehealth.contracts.telemetry.v1.RecentTelemetryResponse;
import com.rehealth.contracts.telemetry.v1.SleepSessionRecord;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchRequest;
import com.rehealth.contracts.telemetry.v1.TelemetryBatchResponse;
import com.rehealth.contracts.telemetry.v1.TelemetryContractValidator;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationPolicy;
import com.rehealth.contracts.telemetry.v1.TelemetryValidationResult;
import com.rehealth.device.adapter.TimescaleTelemetryStore;
import com.rehealth.device.adapter.TimescaleTelemetryReader;
import com.rehealth.device.api.ApiExceptionHandler;
import com.rehealth.device.api.DeviceTelemetryController;
import com.rehealth.device.application.DeviceRequestException;
import com.rehealth.device.application.DeviceTelemetryService;
import com.rehealth.device.domain.DeviceClaims;
import com.rehealth.device.port.IdentityAuthorizationPort;
import com.rehealth.device.port.TelemetryReadPort;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TelemetryIngestionIT {
    private static final Set<String> FAILURE_CASES =
            Set.of("concurrent_duplicate", "mid_batch_failure", "db_down");
    private static GenericContainer<?> timescale;
    private static String timescaleAdminUrl;

    @BeforeAll
    static void startTimescale() {
        timescaleAdminUrl = System.getenv("TIMESCALE_TEST_JDBC_URL");
        if (timescaleAdminUrl == null || timescaleAdminUrl.isBlank()) {
            timescale = TimescaleTestDatabase.start(TimescaleTestDatabase.TIMESCALE_IMAGE);
        }
    }

    @AfterAll
    static void stopTimescale() {
        if (timescale != null) {
            timescale.stop();
        }
    }

    @Test
    void firstAndReplayReturnOriginalReceiptAndOneLogicalEventSet() throws Exception {
        requireHappy();
        TestStore store = newStore();
        TelemetryBatchRequest request = mixedBatch("batch-replay");
        DeviceClaims owner = owner("tenant-a", "user-a", "ring-a");

        TelemetryBatchResponse first = store.write(owner, request, validate(request));
        TelemetryBatchResponse replay = store.write(owner, request, validate(request));

        assertTrue(first.persisted);
        assertEquals(first.receiptId, replay.receiptId);
        assertEquals("ACCEPTED_DUPLICATE", replay.status);
        assertCounts(store.jdbc(), 1, 1, 1, 1, 2, 1, 2);
        assertEquals("EVENT_PENDING", scalarText(store.jdbc(),
                "SELECT state FROM hardware_reconciliation"));
        assertEquals(1, scalarInt(store.jdbc(),
                "SELECT min(event_version) FROM hardware_outbox"));
        assertEquals(1, scalarInt(store.jdbc(), """
                SELECT count(*) FROM hardware_outbox
                WHERE event_type = 'rehealth.telemetry.persisted.v1'
                """));
        assertEquals(1, scalarInt(store.jdbc(), """
                SELECT count(*) FROM hardware_outbox
                WHERE event_type = 'rehealth.telemetry.quality.v1'
                """));
        assertEquals(first.receiptId, scalarText(store.jdbc(), """
                SELECT event_metadata->>'batch_id'
                FROM hardware_outbox
                WHERE event_type = 'rehealth.telemetry.persisted.v1'
                """));
        assertEquals(2, scalarInt(store.jdbc(), """
                SELECT count(*) FROM hardware_outbox
                WHERE event_metadata->>'tenant_ref' LIKE 'opaque_%'
                  AND NOT event_metadata ? 'heart_rate'
                """));
    }

    @Test
    void recentReadReturnsOnlyTheAuthorizedOwnerAndDevice() throws Exception {
        requireHappy();
        TestStore store = newStore();
        TelemetryBatchRequest request = mixedBatch("batch-recent");
        DeviceClaims owner = owner("tenant-a", "user-a", "ring-a");
        store.write(owner, request, validate(request));

        RecentTelemetryResponse response = store.reader().recent(owner, 10);

        assertEquals("user-a", response.userId);
        assertEquals(10, response.limit);
        assertEquals(1, response.measurements.size());
        assertEquals("measurement-1", response.measurements.get(0).id);
        assertEquals(1, response.sleepSessions.size());
        assertEquals("sleep-1", response.sleepSessions.get(0).id);
        assertEquals(1, response.activities.size());
        assertEquals("activity-1", response.activities.get(0).id);
    }

    @Test
    void ownerScopeAllowsSameBatchIdWithoutCrossOwnerReplay() throws Exception {
        requireHappy();
        TestStore store = newStore();
        TelemetryBatchRequest firstRequest = mixedBatch("shared-batch");
        TelemetryBatchRequest secondRequest = mixedBatch("shared-batch");

        TelemetryBatchResponse first = store.write(
                owner("tenant-a", "user-a", "ring-a"), firstRequest, validate(firstRequest));
        TelemetryBatchResponse second = store.write(
                owner("tenant-a", "user-b", "ring-a"), secondRequest, validate(secondRequest));

        assertTrue(!first.receiptId.equals(second.receiptId));
        assertEquals(2, scalarInt(store.jdbc(), "SELECT count(*) FROM hardware_upload_batch"));
        assertEquals(1, scalarInt(store.jdbc(), """
                SELECT count(*) FROM hardware_upload_batch
                WHERE tenant_id = 'tenant-a' AND user_id = 'user-a'
                """));
        assertEquals(1, scalarInt(store.jdbc(), """
                SELECT count(*) FROM hardware_upload_batch
                WHERE tenant_id = 'tenant-a' AND user_id = 'user-b'
                """));
    }

    @Test
    void operatorReplayIsIdempotentAndRecordsActorAndReason() throws Exception {
        requireHappy();
        TestStore store = newStore();
        TelemetryBatchRequest request = mixedBatch("batch-operator-replay");
        DeviceClaims owner = owner("tenant-a", "user-a", "ring-a");
        TelemetryBatchResponse persisted = store.write(owner, request, validate(request));

        String first = store.writer().replay(
                "tenant-a", persisted.receiptId, "operator-a", "retry publisher");
        String second = store.writer().replay(
                "tenant-a", persisted.receiptId, "operator-a", "retry publisher");

        assertEquals(persisted.receiptId, first);
        assertEquals(first, second);
        assertEquals(1, scalarInt(store.jdbc(),
                "SELECT attempt_count FROM hardware_reconciliation"));
        assertEquals("operator-a", scalarText(store.jdbc(),
                "SELECT operator_actor_id FROM hardware_reconciliation"));
        assertEquals("retry publisher", scalarText(store.jdbc(),
                "SELECT operator_reason FROM hardware_reconciliation"));
        assertCounts(store.jdbc(), 1, 1, 1, 1, 2, 1, 2);
    }

    @Test
    void concurrentDuplicateReturnsOneReceiptAndOneEventSet() throws Exception {
        requireCase("concurrent_duplicate");
        TestStore store = newStore();
        TelemetryBatchRequest request = mixedBatch("batch-concurrent");
        DeviceClaims owner = owner("tenant-a", "user-a", "ring-a");
        TelemetryValidationResult validation = validate(request);
        CountDownLatch start = new CountDownLatch(1);
        Callable<TelemetryBatchResponse> write = () -> {
            start.await();
            return store.write(owner, request, validation);
        };

        List<TelemetryBatchResponse> responses;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<TelemetryBatchResponse> first = executor.submit(write);
            Future<TelemetryBatchResponse> second = executor.submit(write);
            start.countDown();
            responses = List.of(first.get(), second.get());
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, responses.stream().map(response -> response.receiptId).distinct().count());
        assertEquals(1, responses.stream()
                .filter(response -> "ACCEPTED_DUPLICATE".equals(response.status)).count());
        assertCounts(store.jdbc(), 1, 1, 1, 1, 2, 1, 2);
    }

    @Test
    void midBatchConstraintFailureRollsBackEveryRow() throws Exception {
        requireCase("mid_batch_failure");
        TestStore store = newStore();
        TelemetryBatchRequest request = mixedBatch("batch-rollback");
        MeasurementRecord invalid = measurement("measurement-invalid");
        invalid.unit = "unit-value-that-exceeds-the-database-column-limit-of-thirty-two-characters";
        request.measurements.add(invalid);

        DeviceRequestException failure = assertThrows(
                DeviceRequestException.class,
                () -> store.write(owner("tenant-a", "user-a", "ring-a"), request, validate(request))
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, failure.status());
        assertEquals("HARDWARE_PERSISTENCE_UNAVAILABLE", failure.errorCode());
        assertCounts(store.jdbc(), 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void databaseFailureReturnsStableRetryable503WithoutFalseSuccess() throws Exception {
        requireCase("db_down");
        DataSource unavailable = new DriverManagerDataSource(
                "jdbc:postgresql://127.0.0.1:1/rehealth_unavailable",
                TimescaleTestDatabase.USER,
                TimescaleTestDatabase.PASSWORD
        );
        TimescaleTelemetryStore store = createStore(unavailable);
        TelemetryContractValidator validator =
                new TelemetryContractValidator(TelemetryValidationPolicy.productionDefault());
        IdentityAuthorizationPort identity =
                (token, tenant, device) -> owner(tenant, "user-a", device);
        TelemetryReadPort reader = (claims, limit) -> {
            throw new AssertionError("reader must not be called");
        };
        DeviceTelemetryService service = new DeviceTelemetryService(validator, identity, store, reader);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new DeviceTelemetryController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        mvc.perform(post("/rehealth/mobile/measurements/batch")
                        .header("X-Access-Token", "owner-token")
                        .header("X-Tenant-Id", "tenant-a")
                        .contentType("application/json")
                        .content("""
                                {
                                  "schemaVersion":"telemetry-v1",
                                  "batchId":"batch-db-down",
                                  "deviceId":"ring-a",
                                  "source":"MRD_RING",
                                  "measurements":[{
                                    "id":"measurement-1",
                                    "metricType":"heart_rate",
                                    "measuredAt":1784793600000,
                                    "primaryValue":72.0,
                                    "unit":"bpm"
                                  }]
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.message").value("HARDWARE_PERSISTENCE_UNAVAILABLE"));
    }

    private static TestStore newStore() throws SQLException {
        TimescaleTestDatabase database = timescale == null
                ? TimescaleTestDatabase.create(timescaleAdminUrl)
                : TimescaleTestDatabase.create(timescale);
        database.flyway().migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                database.url(), TimescaleTestDatabase.USER, TimescaleTestDatabase.PASSWORD);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        return new TestStore(
                createStore(dataSource),
                new TimescaleTelemetryReader(jdbc),
                jdbc
        );
    }

    private static TimescaleTelemetryStore createStore(DataSource dataSource) {
        return new TimescaleTelemetryStore(
                new JdbcTemplate(dataSource),
                new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new ObjectMapper()
        );
    }

    private static TelemetryBatchRequest mixedBatch(String batchId) {
        TelemetryBatchRequest request = new TelemetryBatchRequest();
        request.schemaVersion = "telemetry-v1";
        request.batchId = batchId;
        request.deviceId = "ring-a";
        request.source = "ANDROID_ROOM";
        request.collectedFrom = 1784793600000L;
        request.collectedTo = 1784797200000L;
        MeasurementRecord measurement = measurement("measurement-1");
        measurement.qualityCode = "LOW_CONFIDENCE";
        request.measurements.add(measurement);

        SleepSessionRecord sleep = new SleepSessionRecord();
        sleep.id = "sleep-1";
        sleep.startedAt = 1784750400000L;
        sleep.endedAt = 1784779200000L;
        sleep.deepMinutes = 80;
        sleep.lightMinutes = 300;
        sleep.awakeMinutes = 20;
        sleep.remMinutes = 80;
        sleep.interruptionMinutes = 10;
        sleep.source = "MRD";
        request.sleepSessions.add(sleep);

        ActivitySessionRecord activity = new ActivitySessionRecord();
        activity.id = "activity-1";
        activity.startedAt = 1784793600000L;
        activity.endedAt = 1784795400000L;
        activity.activityType = "walking";
        activity.steps = 1200;
        activity.distanceMeters = 850.5;
        activity.caloriesKcal = 65.0;
        activity.durationMinutes = 30;
        activity.averageHeartRate = 88.0;
        activity.source = "MRD";
        request.activitySessions.add(activity);
        request.quality.put("rejectedCount", 1);
        request.quality.put("rejectionCode", "OUT_OF_WINDOW");
        return request;
    }

    private static MeasurementRecord measurement(String id) {
        MeasurementRecord measurement = new MeasurementRecord();
        measurement.id = id;
        measurement.metricType = "heart_rate";
        measurement.measuredAt = 1784793600000L;
        measurement.primaryValue = 72.0;
        measurement.unit = "bpm";
        measurement.source = "MRD";
        return measurement;
    }

    private static TelemetryValidationResult validate(TelemetryBatchRequest request) {
        TelemetryValidationResult result =
                new TelemetryContractValidator(TelemetryValidationPolicy.productionDefault())
                        .validateClientRequest(request);
        assertTrue(result.valid(), () -> "fixture must be valid: " + result.errors());
        return result;
    }

    private static DeviceClaims owner(String tenantId, String userId, String deviceId) {
        return new DeviceClaims(userId, tenantId, deviceId);
    }

    private static void assertCounts(
            JdbcTemplate jdbc,
            int batch,
            int measurements,
            int sleep,
            int activity,
            int quality,
            int reconciliation,
            int outbox
    ) {
        assertEquals(batch, scalarInt(jdbc, "SELECT count(*) FROM hardware_upload_batch"));
        assertEquals(measurements, scalarInt(jdbc, "SELECT count(*) FROM hardware_measurement"));
        assertEquals(sleep, scalarInt(jdbc, "SELECT count(*) FROM hardware_sleep_session"));
        assertEquals(activity, scalarInt(jdbc, "SELECT count(*) FROM hardware_activity"));
        assertEquals(quality, scalarInt(jdbc, "SELECT count(*) FROM hardware_data_quality_event"));
        assertEquals(reconciliation, scalarInt(jdbc, "SELECT count(*) FROM hardware_reconciliation"));
        assertEquals(outbox, scalarInt(jdbc, "SELECT count(*) FROM hardware_outbox"));
    }

    private static int scalarInt(JdbcTemplate jdbc, String sql) {
        Integer result = jdbc.queryForObject(sql, Integer.class);
        return result == null ? 0 : result;
    }

    private static String scalarText(JdbcTemplate jdbc, String sql) {
        return jdbc.queryForObject(sql, String.class);
    }

    private static boolean caseEnabled(String name) {
        return selectedCases().contains(name);
    }

    private static void requireHappy() {
        if (!selectedCases().isEmpty()) {
            throw new TestAbortedException("happy case excluded by -Dcases");
        }
    }

    private static void requireCase(String name) {
        if (!caseEnabled(name)) {
            throw new TestAbortedException("case not selected: " + name);
        }
    }

    private static Set<String> selectedCases() {
        String configured = System.getProperty("cases", "").trim();
        if (configured.isEmpty()) {
            return Set.of();
        }
        Set<String> selected = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        if (!FAILURE_CASES.containsAll(selected)) {
            throw new IllegalArgumentException("Unsupported -Dcases values: " + selected);
        }
        return selected;
    }

    private record TestStore(
            TimescaleTelemetryStore writer,
            TimescaleTelemetryReader reader,
            JdbcTemplate jdbc
    ) {
        TelemetryBatchResponse write(
                DeviceClaims claims,
                TelemetryBatchRequest request,
                TelemetryValidationResult validation
        ) {
            return writer.write(claims, request, validation);
        }
    }
}
