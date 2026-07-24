package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentModelRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.model.HealthAgentModelClient;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

@Service
public class HealthAgentMobileService {
    private final HealthAgentContextAssembler contextAssembler;
    private final HealthAgentRateLimiter rateLimiter;
    private final HealthAgentModelClient modelServiceClient;
    private final ReHealthBusinessRepository repository;

    public HealthAgentMobileService(
            HealthAgentContextAssembler contextAssembler,
            HealthAgentRateLimiter rateLimiter,
            HealthAgentModelClient modelServiceClient,
            ReHealthBusinessRepository repository
    ) {
        this.contextAssembler = contextAssembler;
        this.rateLimiter = rateLimiter;
        this.modelServiceClient = modelServiceClient;
        this.repository = repository;
    }

    public HealthAgentResponseDto respond(
            String tenantId,
            String userId,
            HealthAgentMessageRequestDto message
    ) {
        HealthAgentRateLimitDecision decision = rateLimiter.acquire(tenantId, userId);
        if (!decision.available()) {
            throw new HealthAgentRequestException(503, "health-agent rate limiter unavailable");
        }
        if (!decision.allowed()) {
            throw new HealthAgentRequestException(429, "health-agent rate limit exceeded");
        }
        HealthAgentModelRequestDto request = contextAssembler.assemble(userId, message);
        long startedNanos = System.nanoTime();
        HealthAgentResponseDto response;
        try {
            response = modelServiceClient.respond(request);
        } catch (RuntimeException failure) {
            recordAudit(userId, request.requestId, null, "FAILED", "MODEL_SERVICE_FAILURE", startedNanos);
            throw failure;
        }
        String outcome = response == null || response.status == null
                ? "FAILED"
                : response.status.toUpperCase();
        recordAudit(
                userId,
                request.requestId,
                response == null ? null : response.modelVersion,
                outcome,
                response == null ? "EMPTY_RESPONSE" : null,
                startedNanos
        );
        return response;
    }

    private void recordAudit(
            String userId,
            String requestId,
            String modelVersion,
            String outcome,
            String errorCode,
            long startedNanos
    ) {
        repository.recordModelRequest(userId, new ModelCallAudit(
                requestId,
                "HEALTH_AGENT_RESPOND",
                modelVersion,
                outcome,
                errorCode,
                Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000)
        ));
    }
}
