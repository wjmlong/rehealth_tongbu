package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePlanBindingEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceInterventionFeedbackEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceConsentRecordEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsurancePolicyEntity;
import org.jeecg.modules.rehealth.insurance.entity.InsuranceSubjectEntity;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceConsentRecordMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceInterventionFeedbackMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePlanBindingMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsurancePolicyMapper;
import org.jeecg.modules.rehealth.insurance.mapper.InsuranceSubjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
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
    void inactiveTenantCannotBindAPlanUsingAStaleSubjectMapping() {
        InsuranceMobilePlanRequest.Bind request = new InsuranceMobilePlanRequest.Bind(
                "1001", "POLICY-1", "PLAN-1", "v1", null, null, null, "source-1", Map.of()
        );

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.bind("shared-user", request)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveTenant(1001);
        verifyNoInteractions(policies, consents, bindings, feedback);
    }

    @Test
    void inactiveTenantCannotReadTheCurrentPlan() {
        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.current("shared-user", "1001")
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveTenant(1001);
        verifyNoInteractions(policies, consents, bindings, feedback);
    }

    @Test
    void inactiveTenantCannotSubmitFeedbackToAnExistingBinding() {
        InsurancePlanBindingEntity binding = new InsurancePlanBindingEntity();
        binding.setId("binding-1");
        binding.setTenantId(1001);
        binding.setStatus("active");
        when(bindings.selectById("binding-1")).thenReturn(binding);
        InsuranceMobilePlanRequest.Feedback request = new InsuranceMobilePlanRequest.Feedback(
                "completed", LocalDateTime.now(), null, null, "feedback-1", null,
                "plan-item-1", BigDecimal.ONE, BigDecimal.ONE, "self_report", Map.of()
        );

        InsuranceApiException error = assertThrows(
                InsuranceApiException.class,
                () -> service.feedback("shared-user", "binding-1", request)
        );

        assertEquals(HttpStatus.FORBIDDEN, error.status());
        verify(subjects).countActiveTenant(1001);
        verifyNoInteractions(policies, consents, feedback);
    }

    @Test
    void serviceRecipientCanSubmitBoundPlanFactsAndServerCalculatesAdherence() {
        InsurancePlanBindingEntity binding = new InsurancePlanBindingEntity();
        binding.setId("binding-1");
        binding.setTenantId(1001);
        binding.setSubjectRef("subject-1");
        binding.setPolicyId("policy-1");
        binding.setConsentId("consent-1");
        binding.setStatus("active");
        InsuranceSubjectEntity subject = new InsuranceSubjectEntity();
        subject.setTenantId(1001);
        subject.setSubjectRef("subject-1");
        subject.setRehealthUserId("app-user");
        subject.setEnrollmentStatus("active");
        InsurancePolicyEntity policy = new InsurancePolicyEntity();
        policy.setId("policy-1");
        policy.setTenantId(1001);
        policy.setStatus("active");
        InsuranceConsentRecordEntity consent = new InsuranceConsentRecordEntity();
        consent.setId("consent-1");
        consent.setTenantId(1001);
        consent.setStatus("granted");
        when(bindings.selectById("binding-1")).thenReturn(binding);
        when(subjects.countActiveTenant(1001)).thenReturn(1);
        when(subjects.selectOne(any())).thenReturn(subject);
        when(policies.selectOne(any())).thenReturn(policy);
        when(consents.selectOne(any())).thenReturn(consent);
        when(feedback.selectOne(any())).thenReturn(null);

        Map<String, Object> result = service.feedback("app-user", "binding-1",
                new InsuranceMobilePlanRequest.Feedback(
                        "partially_completed", LocalDateTime.now(), new BigDecimal("0.9"), BigDecimal.ONE,
                        "feedback-1", "plan-1", "plan-item-1", BigDecimal.ONE,
                        null, "self_report", Map.of()
                ));

        var captor = org.mockito.ArgumentCaptor.forClass(InsuranceInterventionFeedbackEntity.class);
        verify(feedback).insert(captor.capture());
        assertEquals(0, captor.getValue().getAdherenceScore().compareTo(new BigDecimal("0.500000")));
        assertEquals(0, captor.getValue().getExpectedCount().compareTo(BigDecimal.ONE));
        assertEquals("insurance-adherence-event-v1", captor.getValue().getCalculationVersion());
        assertEquals(new BigDecimal("0.500000"), result.get("adherenceScore"));
    }

    @Test
    void appUserCanReadActivePlansFromMultipleInsuranceTenants() {
        InsuranceSubjectEntity firstSubject = subject(1001, "subject-1");
        InsuranceSubjectEntity secondSubject = subject(1002, "subject-2");
        InsurancePlanBindingEntity firstBinding = binding(1001, "binding-1", "subject-1", "policy-1", "consent-1");
        InsurancePlanBindingEntity secondBinding = binding(1002, "binding-2", "subject-2", "policy-2", "consent-2");
        when(subjects.selectList(any())).thenReturn(List.of(firstSubject, secondSubject));
        when(subjects.countActiveTenant(1001)).thenReturn(1);
        when(subjects.countActiveTenant(1002)).thenReturn(1);
        when(bindings.selectList(any())).thenReturn(List.of(firstBinding), List.of(secondBinding));
        when(policies.selectOne(any())).thenAnswer(invocation -> {
            InsurancePolicyEntity policy = new InsurancePolicyEntity();
            policy.setStatus("active");
            policy.setPolicyNo("POL-ACTIVE");
            return policy;
        });
        when(consents.selectOne(any())).thenAnswer(invocation -> {
            InsuranceConsentRecordEntity consent = new InsuranceConsentRecordEntity();
            consent.setStatus("granted");
            consent.setConsentVersion("v1");
            return consent;
        });

        List<InsuranceMobilePlanResponse> result = service.active("app-user");

        assertEquals(2, result.size());
        assertEquals(List.of(1001, 1002), result.stream().map(InsuranceMobilePlanResponse::tenantId).toList());
    }

    private static InsuranceSubjectEntity subject(int tenantId, String subjectRef) {
        InsuranceSubjectEntity value = new InsuranceSubjectEntity();
        value.setTenantId(tenantId);
        value.setSubjectRef(subjectRef);
        value.setRehealthUserId("app-user");
        value.setEnrollmentStatus("active");
        return value;
    }

    private static InsurancePlanBindingEntity binding(
            int tenantId, String id, String subjectRef, String policyId, String consentId
    ) {
        InsurancePlanBindingEntity value = new InsurancePlanBindingEntity();
        value.setTenantId(tenantId);
        value.setId(id);
        value.setSubjectRef(subjectRef);
        value.setPolicyId(policyId);
        value.setConsentId(consentId);
        value.setPlanId("plan-1");
        value.setStatus("active");
        return value;
    }
}
