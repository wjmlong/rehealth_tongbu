package org.jeecg.modules.rehealth.repository.impl;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentConversationDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.repository.HealthAgentConversationRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "false", matchIfMissing = true)
public class StatelessHealthAgentConversationRepository implements HealthAgentConversationRepository {
    @Override
    public String resolveConversation(String tenantId, String userId, String requestedConversationId, String title) {
        return requestedConversationId == null || requestedConversationId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedConversationId;
    }

    @Override
    public Optional<HealthAgentRequestState> findRequestState(
            String tenantId, String userId, String conversationId, String requestId
    ) {
        return Optional.empty();
    }

    @Override
    public void saveUserMessage(
            String tenantId, String userId, String conversationId, String messageId,
            String requestId, String content
    ) {
    }

    @Override
    public HealthAgentHistoryMessageDto saveAssistantMessage(
            String tenantId, String userId, String conversationId, String requestId,
            HealthAgentResponseDto response
    ) {
        response.conversationId = conversationId;
        response.messageId = UUID.randomUUID().toString();
        response.createdAt = System.currentTimeMillis();
        HealthAgentHistoryMessageDto message = new HealthAgentHistoryMessageDto();
        message.messageId = response.messageId;
        message.requestId = requestId;
        message.role = "ASSISTANT";
        message.content = response.answer;
        message.status = response.status;
        message.provider = response.provider;
        message.modelVersion = response.modelVersion;
        message.createdAt = response.createdAt;
        return message;
    }

    @Override
    public List<HealthAgentHistoryMessageDto> findRecentMessages(
            String tenantId, String userId, String conversationId, int limit
    ) {
        return List.of();
    }

    @Override
    public Optional<HealthAgentConversationDto> findLatestConversation(
            String tenantId, String userId, int messageLimit
    ) {
        return Optional.empty();
    }
}
