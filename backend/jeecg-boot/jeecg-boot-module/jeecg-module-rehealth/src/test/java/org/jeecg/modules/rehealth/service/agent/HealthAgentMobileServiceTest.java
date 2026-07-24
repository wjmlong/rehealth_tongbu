package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.PatientProfileDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.jeecg.modules.rehealth.model.HealthAgentModelClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthAgentMobileServiceTest {
    @Test
    void assemblesOnlyAuthenticatedUsersPersistedMinimalContextAndAuditsMetadata() {
        StubReHealthBusinessRepository repository = populatedRepository();
        RecordingModelClient modelClient = new RecordingModelClient();
        HealthAgentMobileService service = service(
                repository,
                modelClient,
                HealthAgentRateLimitDecision.allowedDecision()
        );
        HealthAgentMessageRequestDto message = message();

        HealthAgentResponseDto response = service.respond("tenant-a", "user-a", message);

        assertEquals("ok", response.status);
        assertEquals("50-59", modelClient.request.context.ageBand);
        assertEquals("moderate", modelClient.request.context.riskLevel);
        assertEquals(31.0, modelClient.request.context.riskScorePercent);
        assertEquals("Walk gradually", modelClient.request.context.recommendedAction);
        assertTrue(repository.queriedUsers.stream().allMatch("user-a"::equals));
        assertEquals("HEALTH_AGENT_RESPOND", repository.audit.operation());
        assertEquals("agent-provider-v1", repository.audit.modelVersion());
    }

    @Test
    void rateLimitExceedStopsBeforeContextOrProvider() {
        StubReHealthBusinessRepository repository = populatedRepository();
        RecordingModelClient modelClient = new RecordingModelClient();
        HealthAgentMobileService service = service(
                repository,
                modelClient,
                HealthAgentRateLimitDecision.exceeded()
        );

        HealthAgentRequestException failure = assertThrows(
                HealthAgentRequestException.class,
                () -> service.respond("tenant-a", "user-a", message())
        );

        assertEquals(429, failure.statusCode());
        assertFalse(modelClient.called);
        assertTrue(repository.queriedUsers.isEmpty());
    }

    @Test
    void unavailableRedisFailsClosedBeforeProvider() {
        StubReHealthBusinessRepository repository = populatedRepository();
        RecordingModelClient modelClient = new RecordingModelClient();
        HealthAgentMobileService service = service(
                repository,
                modelClient,
                HealthAgentRateLimitDecision.unavailable()
        );

        HealthAgentRequestException failure = assertThrows(
                HealthAgentRequestException.class,
                () -> service.respond("tenant-a", "user-a", message())
        );

        assertEquals(503, failure.statusCode());
        assertFalse(modelClient.called);
    }

    private HealthAgentMobileService service(
            StubReHealthBusinessRepository repository,
            RecordingModelClient modelClient,
            HealthAgentRateLimitDecision decision
    ) {
        return new HealthAgentMobileService(
                new HealthAgentContextAssembler(repository),
                (tenantId, userId) -> decision,
                modelClient,
                repository
        );
    }

    private StubReHealthBusinessRepository populatedRepository() {
        StubReHealthBusinessRepository repository = new StubReHealthBusinessRepository();
        repository.profile = new PatientProfileDto();
        repository.profile.age = 54;
        repository.profile.diagnoses = java.util.List.of("must-not-leave-jeecg");
        repository.risk = new RiskEvaluateResponseDto();
        repository.risk.riskLevel = "moderate";
        repository.risk.riskScore = 0.31;
        repository.intervention = new InterventionGenerateResponseDto();
        repository.intervention.priorityIntervention = "Walk gradually";
        return repository;
    }

    private HealthAgentMessageRequestDto message() {
        HealthAgentMessageRequestDto message = new HealthAgentMessageRequestDto();
        message.requestId = "agent-request-1";
        message.message = "How can I improve my habits?";
        message.locale = "en-US";
        return message;
    }

    private static class RecordingModelClient implements HealthAgentModelClient {
        boolean called;
        HealthAgentModelRequestDto request;

        @Override
        public HealthAgentResponseDto respond(HealthAgentModelRequestDto request) {
            called = true;
            this.request = request;
            HealthAgentResponseDto response = new HealthAgentResponseDto();
            response.requestId = request.requestId;
            response.status = "ok";
            response.modelVersion = "agent-provider-v1";
            return response;
        }
    }
}
