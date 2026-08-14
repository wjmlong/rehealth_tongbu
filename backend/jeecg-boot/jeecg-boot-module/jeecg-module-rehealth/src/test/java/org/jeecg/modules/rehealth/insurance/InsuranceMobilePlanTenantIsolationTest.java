package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePlanBindingEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceConsentRecordMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceInterventionFeedbackMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePlanBindingMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSubjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InsuranceMobilePlanTenantIsolationTest {
    private final InsuranceSubjectMapper subjects = mock(InsuranceSubjectMapper.class);
    private final InsurancePolicyMapper policies = mock(InsurancePolicyMapper.class);
    private final InsuranceConsentRecordMapper consents = mock(InsuranceConsentRecordMapper.class);
    private final InsurancePlanBindingMapper bindings = mock(InsurancePlanBindingMapper.class);
    private final InsuranceInterventionFeedbackMapper feedback = mock(InsuranceInterventionFeedbackMapper.class);
    private final InsuranceMobilePlanService service = new InsuranceMobilePlanService(
            subjects, policies, consents, bindings, feedback, new ObjectMapper()
    );

    @Test
    void revokedMembershipCannotBindAPlanUsingAStaleSubjectMapping() {
        InsuranceMobilePlanRequest.Bind request = new InsuranceMobilePlanRequest.Bind(
                "1001", "POLICY-1", "PLAN-1", "v1", null, null, null, "source-1", Map.of()
        );

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.bind("shared-user", request)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveMember(1001, "shared-user");
        verifyNoInteractions(policies, consents, bindings, feedback);
    }

    @Test
    void revokedMembershipCannotReadTheCurrentPlan() {
        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.current("shared-user", "1001")
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveMember(1001, "shared-user");
        verifyNoInteractions(policies, consents, bindings, feedback);
    }

    @Test
    void revokedMembershipCannotSubmitFeedbackToAnExistingBinding() {
        InsurancePlanBindingEntity binding = new InsurancePlanBindingEntity();
        binding.setId("binding-1");
        binding.setTenantId(1001);
        binding.setStatus("active");
        when(bindings.selectById("binding-1")).thenReturn(binding);
        InsuranceMobilePlanRequest.Feedback request = new InsuranceMobilePlanRequest.Feedback(
                "completion", LocalDateTime.now(), null, null, "feedback-1", null, Map.of()
        );

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.feedback("shared-user", "binding-1", request)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveMember(1001, "shared-user");
        verifyNoInteractions(policies, consents, feedback);
    }
}
