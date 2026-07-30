package org.jeecg.modules.rehealth.service.agent;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentMessageRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentConversationDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.model.ModelCallAudit;
import org.jeecg.modules.rehealth.repository.HealthAgentConversationRepository;
import org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class HealthAgentMobileService {
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");
    private static final int PROMPT_HISTORY_LIMIT = 16;
    private final HealthAgentContextAssembler contextAssembler;
    private final HealthAgentRateLimiter rateLimiter;
    private final HealthAgentEngine engine;
    private final HealthAgentSafetyPolicy safetyPolicy;
    private final HealthAgentConversationRepository conversationRepository;
    private final ReHealthBusinessRepository repository;
    private final HealthAgentProfileUpdateService profileUpdateService;

    public HealthAgentMobileService(
            HealthAgentContextAssembler contextAssembler,
            HealthAgentRateLimiter rateLimiter,
            HealthAgentEngine engine,
            HealthAgentSafetyPolicy safetyPolicy,
            HealthAgentConversationRepository conversationRepository,
            ReHealthBusinessRepository repository,
            HealthAgentProfileUpdateService profileUpdateService
    ) {
        this.contextAssembler = contextAssembler;
        this.rateLimiter = rateLimiter;
        this.engine = engine;
        this.safetyPolicy = safetyPolicy;
        this.conversationRepository = conversationRepository;
        this.repository = repository;
        this.profileUpdateService = profileUpdateService;
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
        validateIdentifiers(message);
        HealthAgentPromptContext promptContext = contextAssembler.assemblePrompt(userId, message);
        String conversationId = conversationRepository.resolveConversation(
                tenantId,
                userId,
                message.conversationId,
                promptContext.legacyRequest().message
        );
        Optional<HealthAgentConversationRepository.HealthAgentRequestState> existing =
                conversationRepository.findRequestState(
                        tenantId, userId, conversationId, promptContext.legacyRequest().requestId
                );
        HealthAgentProfileUpdateResult profileUpdate = HealthAgentProfileUpdateResult.none();
        if (existing.isPresent()) {
            if (!existing.get().userContent().equals(promptContext.legacyRequest().message)) {
                throw new HealthAgentRequestException(409, "requestId was already used for another message");
            }
            if (existing.get().response() != null
                    && !Boolean.TRUE.equals(existing.get().response().retryable)) {
                return existing.get().response();
            }
        } else {
            profileUpdate = profileUpdateService.updateFromMessage(userId, message.message);
            if (profileUpdate.changed()) {
                message.requestId = promptContext.legacyRequest().requestId;
                promptContext = contextAssembler.assemblePrompt(userId, message);
            }
            conversationRepository.saveUserMessage(
                    tenantId,
                    userId,
                    conversationId,
                    clientMessageId(message.clientMessageId),
                    promptContext.legacyRequest().requestId,
                    promptContext.legacyRequest().message
            );
        }
        HealthAgentPromptContext enginePromptContext = promptContext;
        List<HealthAgentHistoryMessageDto> history = conversationRepository.findRecentMessages(
                        tenantId, userId, conversationId, PROMPT_HISTORY_LIMIT + 1
                ).stream()
                .filter(item -> !enginePromptContext.legacyRequest().requestId.equals(item.requestId))
                .toList();
        long startedNanos = System.nanoTime();
        HealthAgentResponseDto response;
        try {
            response = safetyPolicy.preflight(enginePromptContext.legacyRequest())
                    .orElseGet(() -> engine.respond(new HealthAgentEngineRequest(enginePromptContext, history)));
            response = safetyPolicy.postflight(response);
            appendProfileUpdateConfirmation(response, profileUpdate);
        } catch (RuntimeException failure) {
            recordAudit(
                    userId,
                    enginePromptContext.legacyRequest().requestId,
                    null,
                    "FAILED",
                    "HEALTH_AGENT_ENGINE_FAILURE",
                    startedNanos
            );
            throw failure;
        }
        conversationRepository.saveAssistantMessage(
                tenantId,
                userId,
                conversationId,
                enginePromptContext.legacyRequest().requestId,
                response
        );
        String outcome = response == null || response.status == null
                ? "FAILED"
                : response.status.toUpperCase();
        recordAudit(
                userId,
                enginePromptContext.legacyRequest().requestId,
                response == null ? null : response.modelVersion,
                outcome,
                response == null ? "EMPTY_RESPONSE" : null,
                startedNanos
        );
        return response;
    }

    private void appendProfileUpdateConfirmation(
            HealthAgentResponseDto response,
            HealthAgentProfileUpdateResult profileUpdate
    ) {
        if (response == null || !profileUpdate.changed()) {
            return;
        }
        String confirmation = "已更新个人资料：" + String.join("、", profileUpdate.changedFields()) + "。";
        response.answer = response.answer == null || response.answer.isBlank()
                ? confirmation
                : response.answer.stripTrailing() + "\n\n" + confirmation;
    }

    public Optional<HealthAgentConversationDto> latestConversation(
            String tenantId,
            String userId,
            int messageLimit
    ) {
        return conversationRepository.findLatestConversation(
                tenantId, userId, Math.max(1, Math.min(messageLimit, 200))
        );
    }

    private void validateIdentifiers(HealthAgentMessageRequestDto message) {
        if (message == null) {
            throw new HealthAgentRequestException(400, "health-agent request is required");
        }
        if (message.conversationId != null && !message.conversationId.isBlank()
                && !IDENTIFIER.matcher(message.conversationId).matches()) {
            throw new HealthAgentRequestException(400, "invalid health-agent conversationId");
        }
        if (message.clientMessageId != null && !message.clientMessageId.isBlank()
                && !IDENTIFIER.matcher(message.clientMessageId).matches()) {
            throw new HealthAgentRequestException(400, "invalid health-agent clientMessageId");
        }
    }

    private String clientMessageId(String candidate) {
        return candidate == null || candidate.isBlank() ? UUID.randomUUID().toString() : candidate;
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
