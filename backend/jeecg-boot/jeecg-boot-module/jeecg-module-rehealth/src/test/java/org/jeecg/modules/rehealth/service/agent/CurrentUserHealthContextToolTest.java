package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewAnswerDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RhiManualHealthInputDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.BehaviorRecordRepository;
import org.jeecg.modules.rehealth.service.intervention.DeviceInterventionContext;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserHealthContextToolTest {
    private static final Instant NOW = Instant.parse("2026-08-21T08:00:00Z");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsAllAuthorizedHealthCategoriesForOnlyTheAuthenticatedOwner() throws Exception {
        StubReHealthBusinessRepository repository = populatedRepository();
        RecordingBehaviorRepository behaviorRepository = new RecordingBehaviorRepository();
        BehaviorRecordDto meal = new BehaviorRecordDto();
        meal.category = "FOOD";
        meal.summary = "午餐蔬菜和米饭";
        meal.caloriesKcal = 520.0;
        meal.occurredAt = NOW.minusSeconds(3600).toEpochMilli();
        behaviorRepository.records = List.of(meal);

        RecordingDeviceClient deviceClient = new RecordingDeviceClient();
        DeviceInterventionContext device = new DeviceInterventionContext();
        device.localDate = "2026-08-21";
        device.timeZone = "Asia/Shanghai";
        device.todayBehavior = new DeviceInterventionContext.TodayBehavior();
        device.todayBehavior.steps = 6380;
        DeviceInterventionContext.MetricSnapshot heartRate = new DeviceInterventionContext.MetricSnapshot();
        heartRate.metricType = "HEART_RATE";
        heartRate.latestValue = 72.0;
        heartRate.unit = "bpm";
        device.todayBehavior.measurements = List.of(heartRate);
        deviceClient.context = device;

        RecordingLongitudinalReader longitudinal = new RecordingLongitudinalReader();
        longitudinal.context = Map.of(
                "rhi", Map.of("latest", Map.of("displayScore", 78.0)),
                "rdi", Map.of("latest", Map.of("displayScore", 54.0, "isMock", false)),
                "latestAttribution", Map.of("status", "insufficient_data"),
                "recentInterventionFeedback", List.of(Map.of("status", "completed"))
        );
        CurrentUserHealthContextTool tool = tool(
                repository, behaviorRepository, deviceClient, longitudinal);

        JsonNode context = objectMapper.readTree(tool.execute(
                "tenant-trusted", "user-trusted", ZoneId.of("Asia/Shanghai")));

        assertEquals("health-agent-authorized-context-v2", context.path("contextVersion").asText());
        assertEquals("小禾", context.path("profile").path("name").asText());
        assertEquals(7.2, context.path("manualClinicalArchive").path("hba1cPercent").asDouble());
        assertEquals("睡眠", context.path("latestInterview").path("answers").get(0).path("topic").asText());
        assertEquals(31.0, context.path("latestRisk").path("scorePercent").asDouble());
        assertEquals(30.0, context.path("recentRiskHistory").get(0).path("scorePercent").asDouble());
        assertEquals("每天步行", context.path("latestIntervention").path("priority").asText());
        assertEquals(520.0, context.path("recentBehaviorRecords").get(0).path("caloriesKcal").asDouble());
        assertEquals(6380, context.path("deviceHealthSummary").path("today").path("steps").asInt());
        assertEquals(72.0, context.path("deviceHealthSummary").path("today")
                .path("measurements").get(0).path("latestValue").asDouble());
        assertEquals(78.0, context.path("longitudinalHealth").path("rhi")
                .path("latest").path("displayScore").asDouble());
        assertEquals("available", context.path("coverage").path("deviceHealthSummary").asText());
        assertFalse(context.toString().contains("patientId"));
        assertFalse(context.toString().contains("user-trusted"));
        assertFalse(context.toString().contains("tenant-trusted"));
        assertEquals(List.of("tenant-trusted:user-trusted"), behaviorRepository.queries);
        assertEquals(List.of("tenant-trusted:user-trusted:Asia/Shanghai"), deviceClient.queries);
        assertEquals(List.of("user-trusted"), longitudinal.users);
        assertTrue(repository.queriedUsers.stream().allMatch("user-trusted"::equals));
    }

    @Test
    void marksOnlyUnavailableSourcesWithoutLosingOtherHealthContext() throws Exception {
        StubReHealthBusinessRepository repository = populatedRepository();
        BehaviorRecordRepository brokenBehavior = new RecordingBehaviorRepository() {
            @Override
            public List<BehaviorRecordDto> findInWindow(
                    String tenantId, String userId, Instant startInclusive, Instant endExclusive
            ) {
                throw new IllegalStateException("software db unavailable");
            }
        };
        CurrentUserHealthContextTool tool = tool(
                repository,
                brokenBehavior,
                (tenantId, userId, timeZone) -> {
                    throw new IllegalStateException("device service unavailable");
                },
                userId -> {
                    throw new IllegalStateException("projection unavailable");
                }
        );

        JsonNode context = objectMapper.readTree(tool.execute(
                "tenant-a", "user-a", ZoneId.of("Asia/Shanghai")));

        assertEquals("小禾", context.path("profile").path("name").asText());
        assertEquals("unavailable", context.path("coverage").path("recentBehaviorRecords").asText());
        assertEquals("unavailable", context.path("coverage").path("deviceHealthSummary").asText());
        assertEquals("unavailable", context.path("coverage").path("longitudinalHealth").asText());
    }

    private StubReHealthBusinessRepository populatedRepository() {
        StubReHealthBusinessRepository repository = new StubReHealthBusinessRepository();
        repository.profile = new PatientProfileDto();
        repository.profile.patientId = "must-not-leak";
        repository.profile.name = "小禾";
        repository.profile.age = 54;
        repository.profile.diagnoses = List.of("高血压");

        repository.manualHealthInput = new RhiManualHealthInputDto();
        repository.manualHealthInput.hba1cPercent = 7.2;
        repository.manualHealthInput.labConfirmed = true;

        repository.interview = new HealthInterviewSubmitRequestDto();
        HealthInterviewAnswerDto answer = new HealthInterviewAnswerDto();
        answer.topic = "睡眠";
        answer.content = "最近容易早醒";
        repository.interview.answers = List.of(answer);

        repository.risk = new RiskEvaluateResponseDto();
        repository.risk.riskScore = 0.31;
        repository.risk.riskLevel = "moderate";
        repository.risk.isMock = false;
        repository.risk.factorContributions = Map.of("sleep", 0.12);

        AttributionEventsRequestDto.AttributionHistoryPointDto history =
                new AttributionEventsRequestDto.AttributionHistoryPointDto();
        history.date = "2026-08-20";
        history.riskScore = 0.30;
        repository.riskHistory = List.of(history);

        repository.intervention = new InterventionGenerateResponseDto();
        repository.intervention.priorityIntervention = "每天步行";
        repository.intervention.isMock = false;
        return repository;
    }

    private CurrentUserHealthContextTool tool(
            StubReHealthBusinessRepository repository,
            BehaviorRecordRepository behaviorRepository,
            org.jeecg.modules.rehealth.service.intervention.DeviceInterventionContextClient deviceClient,
            HealthAgentLongitudinalContextReader longitudinalReader
    ) {
        return new CurrentUserHealthContextTool(
                repository,
                behaviorRepository,
                deviceClient,
                longitudinalReader,
                objectMapper,
                Clock.fixed(NOW, ZoneId.of("UTC"))
        );
    }

    private static class RecordingBehaviorRepository implements BehaviorRecordRepository {
        List<BehaviorRecordDto> records = List.of();
        final List<String> queries = new ArrayList<>();

        @Override
        public Optional<BehaviorRecordDto> findByRequestId(String tenantId, String userId, String requestId) {
            return Optional.empty();
        }

        @Override
        public BehaviorRecordDto save(String tenantId, String userId, BehaviorRecordDto record) {
            return record;
        }

        @Override
        public List<BehaviorRecordDto> findInWindow(
                String tenantId, String userId, Instant startInclusive, Instant endExclusive
        ) {
            queries.add(tenantId + ":" + userId);
            return records;
        }
    }

    private static class RecordingDeviceClient implements
            org.jeecg.modules.rehealth.service.intervention.DeviceInterventionContextClient {
        DeviceInterventionContext context;
        final List<String> queries = new ArrayList<>();

        @Override
        public DeviceInterventionContext fetch(String tenantId, String userId, ZoneId timeZone) {
            queries.add(tenantId + ":" + userId + ":" + timeZone.getId());
            return context;
        }
    }

    private static class RecordingLongitudinalReader implements HealthAgentLongitudinalContextReader {
        Map<String, Object> context = Map.of();
        final List<String> users = new ArrayList<>();

        @Override
        public Map<String, Object> read(String authenticatedUserId) {
            users.add(authenticatedUserId);
            return context;
        }
    }
}
