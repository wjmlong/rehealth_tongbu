package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class HealthAgentContextAssembler {
    private static final Pattern REQUEST_ID = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private final ReHealthBusinessRepository repository;

    public HealthAgentContextAssembler(ReHealthBusinessRepository repository) {
        this.repository = repository;
    }

    public HealthAgentModelRequestDto assemble(String userId, HealthAgentMessageRequestDto message) {
        if (message == null || message.message == null || message.message.isBlank()) {
            throw new IllegalArgumentException("health-agent message is required");
        }
        if (message.message.length() > 1200) {
            throw new IllegalArgumentException("health-agent message exceeds 1200 characters");
        }
        PatientProfileDto profile = repository.findPatientProfile(userId).orElse(null);
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
        return request;
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
