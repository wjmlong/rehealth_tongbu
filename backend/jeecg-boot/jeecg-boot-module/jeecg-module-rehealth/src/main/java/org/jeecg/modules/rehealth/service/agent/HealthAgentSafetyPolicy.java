package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class HealthAgentSafetyPolicy {
    private static final List<String> CLINICIAN_ONLY = List.of(
            "diagnose", "diagnosis", "prescribe", "确诊", "诊断", "开药"
    );
    private static final List<String> EMERGENCY = List.of(
            "胸痛", "呼吸困难", "昏迷", "意识不清", "严重出血", "自杀", "自伤",
            "chest pain", "difficulty breathing", "unconscious", "suicide", "self-harm"
    );
    private static final List<String> UNSAFE_OUTPUT = List.of(
            "you have ", "stop taking", "你患有", "已经确诊", "立即停药"
    );

    public Optional<HealthAgentResponseDto> preflight(HealthAgentModelRequestDto request) {
        String normalized = normalize(request.message);
        if (containsAny(normalized, EMERGENCY)) {
            return Optional.of(response(
                    request.requestId,
                    "safety_refusal",
                    "你描述的情况可能需要紧急处理。请立即联系当地急救电话或尽快前往急诊，不要等待在线答复；如可以，请让身边的人陪同。",
                    "emergency-safety-policy-v1"
            ));
        }
        if (containsAny(normalized, CLINICIAN_ONLY)) {
            return Optional.of(response(
                    request.requestId,
                    "safety_refusal",
                    "我不能为你确诊疾病或开具、调整药物，但可以帮助整理症状、生活习惯和就医时要询问医生的问题。",
                    "clinical-safety-policy-v1"
            ));
        }
        return Optional.empty();
    }

    public HealthAgentResponseDto postflight(HealthAgentResponseDto response) {
        if (response == null) {
            throw new IllegalStateException("health-agent returned an empty response");
        }
        response.medicalDisclaimer = HealthAgentResponseDefaults.MEDICAL_DISCLAIMER_ZH;
        if (response.answer != null && containsAny(normalize(response.answer), UNSAFE_OUTPUT)) {
            return response(
                    response.requestId,
                    "safety_refusal",
                    "我不能根据当前信息给出诊断或要求你停药。请让医生结合症状、检查和用药情况进行判断。",
                    "output-safety-policy-v1"
            );
        }
        return response;
    }

    private HealthAgentResponseDto response(String requestId, String status, String answer, String version) {
        HealthAgentResponseDto response = new HealthAgentResponseDto();
        response.requestId = requestId;
        response.status = status;
        response.answer = answer;
        response.medicalDisclaimer = HealthAgentResponseDefaults.MEDICAL_DISCLAIMER_ZH;
        response.provider = "rehealth-safety-policy";
        response.modelVersion = version;
        response.isDemo = false;
        response.retryable = false;
        return response;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }
}
