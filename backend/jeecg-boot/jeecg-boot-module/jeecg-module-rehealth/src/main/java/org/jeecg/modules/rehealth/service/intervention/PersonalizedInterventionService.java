package org.jeecg.modules.rehealth.service.intervention;

import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;

@Service
public class PersonalizedInterventionService {
    public static final String CONTEXT_VERSION = "personalized-intervention-context-v1";

    private final ReHealthBusinessRepository repository;
    private final DeviceInterventionContextClient deviceContextClient;
    private final LangChain4jInterventionEngine interventionEngine;

    public PersonalizedInterventionService(
            ReHealthBusinessRepository repository,
            DeviceInterventionContextClient deviceContextClient,
            LangChain4jInterventionEngine interventionEngine
    ) {
        this.repository = repository;
        this.deviceContextClient = deviceContextClient;
        this.interventionEngine = interventionEngine;
    }

    public InterventionGenerateResponseDto generate(
            String tenantId,
            String userId,
            String requestId,
            ZoneId timeZone
    ) {
        PatientProfileDto profile = repository.findPatientProfile(userId).orElse(null);
        HealthInterviewSubmitRequestDto interview =
                repository.findLatestHealthInterview(userId).orElse(null);
        RiskEvaluateResponseDto risk = repository.findLatestRiskResult(userId).orElse(null);
        DeviceInterventionContext telemetry =
                deviceContextClient.fetch(tenantId, userId, timeZone);
        PersonalizedInterventionContext context = new PersonalizedInterventionContext(
                CONTEXT_VERSION,
                tenantId,
                userId,
                profile,
                interview,
                risk,
                telemetry
        );
        return interventionEngine.generate(requestId, context);
    }
}
