package org.jeecg.modules.rehealth.miwi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.ingest.HardwareIngestionPort;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.AttributionResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.FeedbackRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.TelemetryBatchResponseDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MiwiPushServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String expectedDeviceId(String imei) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imei.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return "miwi4g-" + hex.substring(0, 24);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MiwiPushService service(
            AtomicReference<TelemetryBatchRequestDto> captured,
            String boundUserId
    ) {
        HardwareIngestionPort port = request -> {
            captured.set(request);
            TelemetryBatchResponseDto response = new TelemetryBatchResponseDto();
            response.accepted = true;
            response.batchId = request.batchId;
            response.measurementCount = request.measurements.size();
            return response;
        };
        ReHealthBusinessRepository repository = new NoopBusinessRepository() {
            @Override
            public Optional<String> findActiveUserIdByDeviceId(String deviceId) {
                return expectedDeviceId("7809101598").equals(deviceId)
                        ? Optional.ofNullable(boundUserId)
                        : Optional.empty();
            }
        };
        return new MiwiPushService(
                new MiwiProperties(),
                new MiwiHealthDataMapper(),
                port,
                repository,
                objectMapper
        );
    }

    private Map<String, Object> healthEnvelope() {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("DataType", "Health");
        envelope.put("ResultData",
                "{\"imei\":\"7809101598\",\"heartRate\":67,\"bloodPressureMax\":108,"
                        + "\"bloodPressureMin\":67,\"bloodOxygen\":95,\"timestamp\":1720000010}");
        return envelope;
    }

    @Test
    void acceptsHealthPushForBoundDeviceAndNormalizesMeasurements() {
        AtomicReference<TelemetryBatchRequestDto> captured = new AtomicReference<>();
        MiwiPushService.MiwiPushResult result = service(captured, "user-1").handlePush(healthEnvelope());

        assertTrue(result.accepted);
        assertEquals("ACCEPTED", result.status);
        TelemetryBatchRequestDto batch = captured.get();
        assertNotNull(batch);
        assertEquals("user-1", batch.userId);
        assertEquals(expectedDeviceId("7809101598"), batch.deviceId);
        assertEquals("MIWI_4G_CLOUD", batch.source);
        assertEquals(3, batch.measurements.size());

        Map<String, Object> heartRate = batch.measurements.get(0);
        assertEquals("HEART_RATE", heartRate.get("metricType"));
        assertEquals(67.0, heartRate.get("primaryValue"));
        assertEquals(1720000010000L, heartRate.get("measuredAt"));

        Map<String, Object> bloodPressure = batch.measurements.get(1);
        assertEquals("BLOOD_PRESSURE", bloodPressure.get("metricType"));
        assertEquals(108.0, bloodPressure.get("primaryValue"));
        assertEquals(67.0, bloodPressure.get("secondaryValue"));

        Map<String, Object> spo2 = batch.measurements.get(2);
        assertEquals("BLOOD_OXYGEN", spo2.get("metricType"));
        assertEquals(95.0, spo2.get("primaryValue"));
    }

    @Test
    void skipsUnboundDeviceWithoutIngesting() {
        AtomicReference<TelemetryBatchRequestDto> captured = new AtomicReference<>();
        MiwiPushService.MiwiPushResult result = service(captured, null).handlePush(healthEnvelope());

        assertFalse(result.accepted);
        assertEquals("SKIPPED_UNBOUND_DEVICE", result.status);
        assertEquals(null, captured.get());
    }

    @Test
    void rejectsPayloadWithoutImei() {
        AtomicReference<TelemetryBatchRequestDto> captured = new AtomicReference<>();
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("DataType", "Health");
        envelope.put("ResultData", "{\"heartRate\":67}");

        MiwiPushService.MiwiPushResult result = service(captured, "user-1").handlePush(envelope);
        assertFalse(result.accepted);
        assertTrue(result.status.startsWith("REJECTED_"));
    }

    /** Minimal no-op stub; only findActiveUserIdByDeviceId is overridden per test. */
    private static class NoopBusinessRepository implements ReHealthBusinessRepository {
        @Override
        public PatientProfileDto savePatientProfile(String userId, PatientProfileDto profile) {
            return profile;
        }

        @Override
        public Optional<PatientProfileDto> findPatientProfile(String userId) {
            return Optional.empty();
        }

        @Override
        public HealthInterviewSubmitRequestDto saveHealthInterview(String userId, HealthInterviewSubmitRequestDto request) {
            return request;
        }

        @Override
        public Optional<HealthInterviewSubmitRequestDto> findLatestHealthInterview(String userId) {
            return Optional.empty();
        }

        @Override
        public void recordModelRequest(String userId, ModelCallAudit audit) {
        }

        @Override
        public DeviceBindResponseDto recordDeviceBinding(String userId, DeviceBindRequestDto request) {
            return null;
        }

        @Override
        public boolean hasActiveDeviceBinding(String userId, String deviceId) {
            return false;
        }

        @Override
        public void saveRiskResult(String userId, String requestId, RiskEvaluateRequestDto request, RiskEvaluateResponseDto response) {
        }

        @Override
        public Optional<RiskEvaluateResponseDto> findLatestRiskResult(String userId) {
            return Optional.empty();
        }

        @Override
        public List<AttributionEventsRequestDto.AttributionHistoryPointDto> findAttributionHistory(String userId) {
            return List.of();
        }

        @Override
        public void saveInterventionPlan(String userId, InterventionGenerateResponseDto response) {
        }

        @Override
        public Optional<InterventionGenerateResponseDto> findLatestInterventionPlan(String userId) {
            return Optional.empty();
        }

        @Override
        public Optional<InterventionGenerateResponseDto> findInterventionPlanInWindow(String userId, Instant startInclusive, Instant endExclusive) {
            return Optional.empty();
        }

        @Override
        public void saveFeedback(String userId, String interventionId, FeedbackRequestDto request) {
        }

        @Override
        public void recordAttributionResult(String userId, AttributionEventsRequestDto request, AttributionResponseDto response) {
        }
    }
}
