package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthAgentSafetyPolicyTest {
    private final HealthAgentSafetyPolicy policy = new HealthAgentSafetyPolicy();

    @Test
    void urgentSymptomsAreHandledWithoutCallingTheModel() {
        HealthAgentModelRequestDto request = new HealthAgentModelRequestDto();
        request.requestId = "urgent-1";
        request.message = "我现在胸痛而且呼吸困难";

        HealthAgentResponseDto response = policy.preflight(request).orElseThrow();

        assertEquals("safety_refusal", response.status);
        assertTrue(response.answer.contains("急救"));
        assertTrue(response.medicalDisclaimer.contains("不能替代医疗诊断"));
    }

    @Test
    void unsafeDiagnosisInProviderOutputIsReplaced() {
        HealthAgentResponseDto providerResponse = new HealthAgentResponseDto();
        providerResponse.requestId = "output-1";
        providerResponse.status = "ok";
        providerResponse.answer = "根据数据你患有某种疾病";

        HealthAgentResponseDto response = policy.postflight(providerResponse);

        assertEquals("safety_refusal", response.status);
        assertTrue(response.answer.contains("不能"));
    }
}
