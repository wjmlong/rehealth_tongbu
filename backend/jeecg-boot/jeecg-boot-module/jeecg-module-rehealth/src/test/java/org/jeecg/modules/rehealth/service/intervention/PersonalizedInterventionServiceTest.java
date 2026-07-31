package org.jeecg.modules.rehealth.service.intervention;

import org.jeecg.modules.rehealth.mobile.dto.HealthInterviewSubmitRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PersonalizedInterventionServiceTest {
    @Test
    void reloadsAllAuthorizedSourcesBeforeEveryGeneration() {
        ReHealthBusinessRepository repository = mock(ReHealthBusinessRepository.class);
        DeviceInterventionContextClient deviceClient = mock(DeviceInterventionContextClient.class);
        LangChain4jInterventionEngine engine = mock(LangChain4jInterventionEngine.class);
        PatientProfileDto profile = new PatientProfileDto();
        HealthInterviewSubmitRequestDto interview = new HealthInterviewSubmitRequestDto();
        RiskEvaluateResponseDto risk = new RiskEvaluateResponseDto();
        DeviceInterventionContext telemetry = new DeviceInterventionContext();
        InterventionGenerateResponseDto expected = new InterventionGenerateResponseDto();
        when(repository.findPatientProfile("user-a")).thenReturn(Optional.of(profile));
        when(repository.findLatestHealthInterview("user-a")).thenReturn(Optional.of(interview));
        when(repository.findLatestRiskResult("user-a")).thenReturn(Optional.of(risk));
        when(deviceClient.fetch("tenant-a", "user-a", ZoneId.of("Asia/Shanghai")))
                .thenReturn(telemetry);
        when(engine.generate(any(), any())).thenReturn(expected);
        PersonalizedInterventionService service =
                new PersonalizedInterventionService(repository, deviceClient, engine);

        var response = service.generate(
                "tenant-a",
                "user-a",
                "request-a",
                ZoneId.of("Asia/Shanghai")
        );
        var secondResponse = service.generate(
                "tenant-a",
                "user-a",
                "request-b",
                ZoneId.of("Asia/Shanghai")
        );

        assertSame(expected, response);
        assertSame(expected, secondResponse);
        verify(repository, times(2)).findPatientProfile("user-a");
        verify(repository, times(2)).findLatestHealthInterview("user-a");
        verify(repository, times(2)).findLatestRiskResult("user-a");
        verify(deviceClient, times(2))
                .fetch("tenant-a", "user-a", ZoneId.of("Asia/Shanghai"));
        verify(engine).generate(eq("request-a"), any(PersonalizedInterventionContext.class));
        verify(engine).generate(eq("request-b"), any(PersonalizedInterventionContext.class));
    }
}
