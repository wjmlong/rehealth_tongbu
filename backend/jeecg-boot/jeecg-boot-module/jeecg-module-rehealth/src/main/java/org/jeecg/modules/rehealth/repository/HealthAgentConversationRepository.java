package org.jeecg.modules.rehealth.repository;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentConversationDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;

import java.util.List;
import java.util.Optional;

public interface HealthAgentConversationRepository {
    String resolveConversation(String tenantId, String userId, String requestedConversationId, String title);

    Optional<HealthAgentRequestState> findRequestState(
            String tenantId,
            String userId,
            String conversationId,
            String requestId
    );

    void saveUserMessage(
            String tenantId,
            String userId,
            String conversationId,
            String messageId,
            String requestId,
            String content
    );

    HealthAgentHistoryMessageDto saveAssistantMessage(
            String tenantId,
            String userId,
            String conversationId,
            String requestId,
            HealthAgentResponseDto response
    );

    List<HealthAgentHistoryMessageDto> findRecentMessages(
            String tenantId,
            String userId,
            String conversationId,
            int limit
    );

    Optional<HealthAgentConversationDto> findLatestConversation(
            String tenantId,
            String userId,
            int messageLimit
    );

    record HealthAgentRequestState(String userContent, HealthAgentResponseDto response) {
    }
}
