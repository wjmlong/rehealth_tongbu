package org.jeecg.modules.rehealth.service.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewAnswerDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class HealthAgentContextAssembler {
    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private final ReHealthBusinessRepository repository;
    private final ObjectMapper objectMapper;

    public HealthAgentContextAssembler(ReHealthBusinessRepository repository) {
        this(repository, new ObjectMapper());
    }

    @Autowired
    public HealthAgentContextAssembler(ReHealthBusinessRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public HealthAgentModelRequestDto assemble(String userId, HealthAgentMessageRequestDto message) {
        return assemblePrompt(userId, message).legacyRequest();
    }

    public HealthAgentPromptContext assemblePrompt(String userId, HealthAgentMessageRequestDto message) {
        if (message == null || message.message == null || message.message.isBlank()) {
            throw new IllegalArgumentException("health-agent message is required");
        }
        if (message.message.length() > 1200) {
            throw new IllegalArgumentException("health-agent message exceeds 1200 characters");
        }
        PatientProfileDto profile = repository.findPatientProfile(userId).orElse(null);
        HealthInterviewSubmitRequestDto interview = repository.findLatestHealthInterview(userId).orElse(null);
        RiskEvaluateResponseDto risk = repository.findLatestRiskResult(userId).orElse(null);
        InterventionGenerateResponseDto intervention =
                repository.findLatestInterventionPlan(userId).orElse(null);

        HealthAgentModelRequestDto request = new HealthAgentModelRequestDto();
        request.requestId = requestId(message.requestId);
        request.message = message.message.strip();
        request.locale = message.locale == null || message.locale.isBlank() ? "zh-CN" : message.locale;
        request.context.ageBand = ageBand(profile == null ? null : profile.age);
        request.context.riskLevel = risk == null ? null : risk.riskLevel;
        request.context.riskScorePercent =
                risk == null || risk.riskScore == null ? null : risk.riskScore * 100.0;
        request.context.recommendedAction =
                intervention == null ? null : bounded(intervention.priorityIntervention, 240);
        return new HealthAgentPromptContext(
                request,
                serializeAuthorizedContext(profile, interview, risk, intervention)
        );
    }

    private String serializeAuthorizedContext(
            PatientProfileDto profile,
            HealthInterviewSubmitRequestDto interview,
            RiskEvaluateResponseDto risk,
            InterventionGenerateResponseDto intervention
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (profile != null) {
            Map<String, Object> profileContext = new LinkedHashMap<>();
            put(profileContext, "age", profile.age);
            put(profileContext, "gender", profile.gender);
            put(profileContext, "heightCm", profile.heightCm);
            put(profileContext, "weightKg", profile.weightKg);
            put(profileContext, "bmi", profile.bmi);
            put(profileContext, "familyHistory", profile.familyHistory);
            put(profileContext, "smoking", profile.smoking);
            put(profileContext, "drinking", profile.drinking);
            put(profileContext, "diabetesHistory", profile.diabetesHistory);
            put(profileContext, "hypertensionHistory", profile.hypertensionHistory);
            put(profileContext, "diagnoses", boundedList(profile.diagnoses, 20, 120));
            put(profileContext, "medications", boundedList(profile.medications, 20, 120));
            put(profileContext, "allergies", boundedList(profile.allergies, 20, 120));
            context.put("profile", profileContext);
        }
        if (interview != null && interview.answers != null) {
            List<Map<String, String>> answers = interview.answers.stream()
                    .filter(answer -> answer != null && answer.content != null && !answer.content.isBlank())
                    .limit(16)
                    .map(this::authorizedAnswer)
                    .toList();
            if (!answers.isEmpty()) {
                context.put("latestInterview", answers);
            }
            put(context, "focusAreas", boundedList(interview.focusAreas, 8, 80));
        }
        if (risk != null) {
            Map<String, Object> riskContext = new LinkedHashMap<>();
            put(riskContext, "level", risk.riskLevel);
            put(riskContext, "scorePercent", risk.riskScore == null ? null : risk.riskScore * 100.0);
            put(riskContext, "summary", bounded(risk.summary, 300));
            context.put("latestRisk", riskContext);
        }
        if (intervention != null) {
            Map<String, Object> interventionContext = new LinkedHashMap<>();
            put(interventionContext, "priority", bounded(intervention.priorityIntervention, 240));
            put(interventionContext, "rationale", bounded(intervention.rationale, 400));
            put(interventionContext, "expectedImpact", bounded(intervention.expectedImpact, 240));
            context.put("latestIntervention", interventionContext);
        }
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("health-agent authorized context cannot be serialized", failure);
        }
    }

    private Map<String, String> authorizedAnswer(HealthInterviewAnswerDto answer) {
        Map<String, String> item = new LinkedHashMap<>();
        if (answer.topic != null && !answer.topic.isBlank()) {
            item.put("topic", bounded(answer.topic, 64));
        }
        item.put("content", bounded(answer.content, 300));
        return item;
    }

    private List<String> boundedList(List<String> values, int maxItems, int maxLength) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .limit(maxItems)
                .map(value -> bounded(value, maxLength))
                .toList();
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value != null) {
            target.put(key, value);
        }
    }

    private String requestId(String candidate) {
        return candidate != null && REQUEST_ID.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }

    private String ageBand(Integer age) {
        if (age == null || age < 18 || age > 120) {
            return null;
        }
        int lower = age / 10 * 10;
        return lower + "-" + (lower + 9);
    }

    private String bounded(String value, int limit) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }
}
