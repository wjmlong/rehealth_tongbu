package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.AttributionEventsRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewAnswerDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewBaselineItemDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RhiManualHealthInputDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.BehaviorRecordRepository;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.jeecg.modules.rehealth.service.intervention.DeviceInterventionContext;
import org.jeecg.modules.rehealth.service.intervention.DeviceInterventionContextClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server-side tool for the current authenticated user's bounded health context.
 * It never accepts a caller-supplied tenant or user id as model tool arguments.
 */
@Component
public class CurrentUserHealthContextTool {
    static final String NAME = "get_current_user_health_context";
    static final String CONTEXT_VERSION = "health-agent-authorized-context-v2";

    private final ReHealthBusinessRepository repository;
    private final BehaviorRecordRepository behaviorRepository;
    private final DeviceInterventionContextClient deviceContextClient;
    private final HealthAgentLongitudinalContextReader longitudinalReader;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public CurrentUserHealthContextTool(
            ReHealthBusinessRepository repository,
            BehaviorRecordRepository behaviorRepository,
            DeviceInterventionContextClient deviceContextClient,
            HealthAgentLongitudinalContextReader longitudinalReader,
            ObjectMapper objectMapper
    ) {
        this(repository, behaviorRepository, deviceContextClient, longitudinalReader, objectMapper, Clock.systemUTC());
    }

    CurrentUserHealthContextTool(
            ReHealthBusinessRepository repository,
            BehaviorRecordRepository behaviorRepository,
            DeviceInterventionContextClient deviceContextClient,
            HealthAgentLongitudinalContextReader longitudinalReader,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.behaviorRepository = behaviorRepository;
        this.deviceContextClient = deviceContextClient;
        this.longitudinalReader = longitudinalReader;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public String execute(String tenantId, String authenticatedUserId, ZoneId timeZone) {
        requireIdentity(tenantId, authenticatedUserId);
        ZoneId trustedTimeZone = timeZone == null ? ZoneId.of("Asia/Shanghai") : timeZone;
        Instant now = clock.instant();
        Map<String, Object> context = new LinkedHashMap<>();
        Map<String, Object> coverage = new LinkedHashMap<>();
        context.put("contextVersion", CONTEXT_VERSION);
        context.put("generatedAt", now.toString());
        context.put("timeZone", trustedTimeZone.getId());
        context.put("scope", Map.of(
                "owner", "current_authenticated_user_only",
                "history", "bounded_recent_summary",
                "rawSignalsIncluded", false
        ));

        section(context, coverage, "profile", () -> repository.findPatientProfile(authenticatedUserId)
                .map(this::profile).orElse(null));
        section(context, coverage, "manualClinicalArchive", () -> repository
                .findRhiManualHealthInput(authenticatedUserId).map(this::manualArchive).orElse(null));
        section(context, coverage, "latestInterview", () -> repository
                .findLatestHealthInterview(authenticatedUserId).map(this::interview).orElse(null));
        section(context, coverage, "latestRisk", () -> repository
                .findLatestRiskResult(authenticatedUserId).map(this::risk).orElse(null));
        section(context, coverage, "recentRiskHistory", () -> riskHistory(
                repository.findRiskHistory(authenticatedUserId, 30)));
        section(context, coverage, "latestIntervention", () -> repository
                .findLatestInterventionPlan(authenticatedUserId).map(this::intervention).orElse(null));
        section(context, coverage, "recentBehaviorRecords", () -> behaviorRecords(
                behaviorRepository.findInWindow(
                        tenantId,
                        authenticatedUserId,
                        now.minus(Duration.ofDays(7)),
                        now.plusSeconds(1)
                )));
        section(context, coverage, "deviceHealthSummary", () -> deviceContext(
                deviceContextClient.fetch(tenantId, authenticatedUserId, trustedTimeZone)));
        section(context, coverage, "longitudinalHealth", () -> longitudinalReader.read(authenticatedUserId));
        context.put("coverage", coverage);
        return json(context);
    }

    private Map<String, Object> profile(PatientProfileDto value) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "name", bounded(value.name, 80));
        put(result, "gender", value.gender);
        put(result, "age", value.age);
        put(result, "heightCm", value.heightCm);
        put(result, "weightKg", value.weightKg);
        put(result, "bmi", value.bmi);
        put(result, "diagnoses", boundedList(value.diagnoses, 100, 160));
        put(result, "medications", boundedList(value.medications, 100, 160));
        put(result, "allergies", boundedList(value.allergies, 100, 160));
        put(result, "familyHistory", value.familyHistory);
        put(result, "smoking", value.smoking);
        put(result, "drinking", value.drinking);
        put(result, "diabetesHistory", value.diabetesHistory);
        put(result, "hypertensionHistory", value.hypertensionHistory);
        put(result, "updatedAt", value.updatedAt);
        return result;
    }

    private Map<String, Object> manualArchive(RhiManualHealthInputDto value) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "sedentaryHoursPerDay", value.sedentaryHoursPerDay);
        put(result, "waistCircumferenceCm", value.waistCircumferenceCm);
        put(result, "vo2MaxMlKgMin", value.vo2MaxMlKgMin);
        put(result, "hba1cPercent", value.hba1cPercent);
        put(result, "egfrMlMin173m2", value.egfrMlMin173m2);
        put(result, "cuffSbp7dMean", value.cuffSbp7dMean);
        put(result, "cuffDbp7dMean", value.cuffDbp7dMean);
        put(result, "cuffValidDays", value.cuffValidDays);
        put(result, "cuffConfirmed", value.cuffConfirmed);
        put(result, "fastingGlucoseMmolL", value.fastingGlucoseMmolL);
        put(result, "totalCholesterolMmolL", value.totalCholesterolMmolL);
        put(result, "ldlMmolL", value.ldlMmolL);
        put(result, "hdlMmolL", value.hdlMmolL);
        put(result, "triglyceridesMmolL", value.triglyceridesMmolL);
        put(result, "labConfirmed", value.labConfirmed);
        put(result, "labRecordedAt", value.labRecordedAt);
        put(result, "updatedAt", value.updatedAt);
        return result;
    }

    private Map<String, Object> interview(HealthInterviewSubmitRequestDto value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value.answers != null) {
            List<Map<String, Object>> answers = value.answers.stream()
                    .filter(item -> item != null && item.content != null && !item.content.isBlank())
                    .limit(32)
                    .map(this::answer)
                    .toList();
            put(result, "answers", answers);
        }
        if (value.baselineItems != null) {
            List<Map<String, Object>> baseline = value.baselineItems.stream()
                    .filter(item -> item != null)
                    .limit(32)
                    .map(this::baseline)
                    .filter(item -> !item.isEmpty())
                    .toList();
            put(result, "baselineItems", baseline);
        }
        put(result, "focusAreas", boundedList(value.focusAreas, 16, 100));
        put(result, "generatedAt", value.generatedAt);
        return result;
    }

    private Map<String, Object> answer(HealthInterviewAnswerDto value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "topic", bounded(value.topic, 80));
        put(item, "content", bounded(value.content, 500));
        return item;
    }

    private Map<String, Object> baseline(HealthInterviewBaselineItemDto value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "label", bounded(value.label, 100));
        put(item, "value", bounded(value.value, 300));
        return item;
    }

    private Map<String, Object> risk(RiskEvaluateResponseDto value) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "scorePercent", value.riskScore == null ? null : value.riskScore * 100.0);
        put(result, "level", value.riskLevel);
        put(result, "summary", bounded(value.summary, 600));
        put(result, "isMock", value.isMock);
        put(result, "modelVersion", value.modelVersion);
        put(result, "featureContributions", boundedMap(value.featureContributions, 32));
        put(result, "factorContributions", boundedMap(value.factorContributions, 32));
        put(result, "missingFields", boundedList(value.missingFields, 32, 100));
        put(result, "qualityWarnings", boundedList(value.qualityWarnings, 32, 200));
        return result;
    }

    private List<Map<String, Object>> riskHistory(
            List<AttributionEventsRequestDto.AttributionHistoryPointDto> values
    ) {
        if (values == null) return List.of();
        return values.stream().filter(item -> item != null).limit(30).map(item -> {
            Map<String, Object> result = new LinkedHashMap<>();
            put(result, "date", item.date);
            put(result, "scorePercent", item.riskScore == null ? null : item.riskScore * 100.0);
            put(result, "interventionDay", item.intervention);
            return result;
        }).toList();
    }

    private Map<String, Object> intervention(InterventionGenerateResponseDto value) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "generatedAt", value.generatedAt);
        put(result, "focusDate", value.focusDate);
        put(result, "priority", bounded(value.priorityIntervention, 300));
        put(result, "rationale", bounded(value.rationale, 600));
        put(result, "expectedImpact", bounded(value.expectedImpact, 300));
        put(result, "contraindications", boundedList(value.contraindications, 20, 200));
        put(result, "confidence", value.confidence);
        put(result, "isMock", value.isMock);
        put(result, "summary", bounded(value.summary, 600));
        if (value.items != null) {
            List<Map<String, Object>> items = value.items.stream()
                    .filter(item -> item != null)
                    .limit(20)
                    .map(this::interventionItem)
                    .toList();
            put(result, "items", items);
        }
        return result;
    }

    private Map<String, Object> interventionItem(InterventionGenerateResponseDto.InterventionActionDto value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "category", value.category);
        put(item, "title", bounded(value.title, 160));
        put(item, "action", bounded(value.action, 500));
        put(item, "rationale", bounded(value.rationale, 500));
        put(item, "target", bounded(value.target, 240));
        put(item, "timing", bounded(value.timing, 160));
        put(item, "priority", value.priority);
        return item;
    }

    private List<Map<String, Object>> behaviorRecords(List<BehaviorRecordDto> values) {
        if (values == null) return List.of();
        return values.stream().filter(item -> item != null).limit(20).map(value -> {
            Map<String, Object> item = new LinkedHashMap<>();
            put(item, "category", value.category);
            put(item, "title", bounded(value.title, 160));
            put(item, "summary", bounded(value.summary, 400));
            put(item, "items", boundedList(value.items, 20, 200));
            put(item, "caloriesKcal", value.caloriesKcal);
            put(item, "proteinGrams", value.proteinGrams);
            put(item, "carbohydrateGrams", value.carbohydrateGrams);
            put(item, "fatGrams", value.fatGrams);
            put(item, "confidence", value.confidence);
            put(item, "occurredAt", value.occurredAt);
            return item;
        }).toList();
    }

    private Map<String, Object> deviceContext(DeviceInterventionContext value) {
        if (value == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "generatedAt", value.generatedAt);
        put(result, "localDate", value.localDate);
        put(result, "timeZone", value.timeZone);
        put(result, "latestDataAt", value.latestDataAt);
        if (value.todayBehavior != null) put(result, "today", todayBehavior(value.todayBehavior));
        if (value.recentChanges != null) {
            put(result, "recentChanges", value.recentChanges.stream()
                    .filter(item -> item != null).limit(32).map(this::recentChange).toList());
        }
        return result;
    }

    private Map<String, Object> todayBehavior(DeviceInterventionContext.TodayBehavior value) {
        Map<String, Object> result = new LinkedHashMap<>();
        put(result, "steps", value.steps);
        put(result, "activeMinutes", value.activeMinutes);
        put(result, "activityCaloriesKcal", value.activityCaloriesKcal);
        put(result, "averageActivityHeartRate", value.averageActivityHeartRate);
        put(result, "sleepMinutes", value.sleepMinutes);
        put(result, "sleepEndedAt", value.sleepEndedAt);
        if (value.dietRecords != null) {
            put(result, "dietRecords", value.dietRecords.stream()
                    .filter(item -> item != null).limit(20).map(this::diet).toList());
        }
        if (value.measurements != null) {
            put(result, "measurements", value.measurements.stream()
                    .filter(item -> item != null).limit(64).map(this::measurement).toList());
        }
        return result;
    }

    private Map<String, Object> diet(DeviceInterventionContext.DietSnapshot value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "mealType", value.mealType);
        put(item, "description", bounded(value.description, 300));
        put(item, "consumedAt", value.consumedAt);
        put(item, "caloriesKcal", value.caloriesKcal);
        put(item, "proteinGrams", value.proteinGrams);
        put(item, "carbohydrateGrams", value.carbohydrateGrams);
        put(item, "fatGrams", value.fatGrams);
        put(item, "fiberGrams", value.fiberGrams);
        put(item, "sodiumMilligrams", value.sodiumMilligrams);
        return item;
    }

    private Map<String, Object> measurement(DeviceInterventionContext.MetricSnapshot value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "metricType", value.metricType);
        put(item, "latestValue", value.latestValue);
        put(item, "averageValue", value.averageValue);
        put(item, "minimumValue", value.minimumValue);
        put(item, "maximumValue", value.maximumValue);
        put(item, "unit", value.unit);
        put(item, "sampleCount", value.sampleCount);
        put(item, "latestObservedAt", value.latestObservedAt);
        return item;
    }

    private Map<String, Object> recentChange(DeviceInterventionContext.RecentChange value) {
        Map<String, Object> item = new LinkedHashMap<>();
        put(item, "metricType", value.metricType);
        put(item, "unit", value.unit);
        put(item, "recentAverage", value.recentAverage);
        put(item, "previousAverage", value.previousAverage);
        put(item, "delta", value.delta);
        put(item, "trend", value.trend);
        put(item, "recentSampleCount", value.recentSampleCount);
        put(item, "previousSampleCount", value.previousSampleCount);
        return item;
    }

    private void section(
            Map<String, Object> context,
            Map<String, Object> coverage,
            String name,
            Supplier<Object> supplier
    ) {
        try {
            Object value = supplier.get();
            if (empty(value)) {
                coverage.put(name, "no_data");
            } else {
                context.put(name, value);
                coverage.put(name, "available");
            }
        } catch (RuntimeException failure) {
            coverage.put(name, "unavailable");
        }
    }

    private String json(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("current user health context cannot be serialized", failure);
        }
    }

    private static boolean empty(Object value) {
        return value == null
                || value instanceof Map<?, ?> map && map.isEmpty()
                || value instanceof List<?> list && list.isEmpty();
    }

    private static void requireIdentity(String tenantId, String userId) {
        if (tenantId == null || tenantId.isBlank() || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("authenticated tenant and user are required");
        }
    }

    private static List<String> boundedList(List<String> values, int maxItems, int maxLength) {
        if (values == null) return List.of();
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(maxItems)
                .map(value -> bounded(value, maxLength))
                .toList();
    }

    private static Map<String, Double> boundedMap(Map<String, Double> values, int maxItems) {
        if (values == null) return Map.of();
        Map<String, Double> result = new LinkedHashMap<>();
        values.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null)
                .limit(maxItems)
                .forEach(entry -> result.put(bounded(entry.getKey(), 100), entry.getValue()));
        return result;
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (!empty(value) && (!(value instanceof String text) || !text.isBlank())) target.put(key, value);
    }

    private static String bounded(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
