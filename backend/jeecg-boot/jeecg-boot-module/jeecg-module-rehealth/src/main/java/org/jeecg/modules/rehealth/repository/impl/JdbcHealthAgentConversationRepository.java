package org.jeecg.modules.rehealth.repository.impl;

import org.jeecg.modules.rehealth.mobile.dto.HealthAgentConversationDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentHistoryMessageDto;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.repository.HealthAgentConversationRepository;
import org.jeecg.modules.rehealth.service.agent.HealthAgentResponseDefaults;
import org.jeecg.modules.rehealth.service.agent.HealthAgentRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "rehealth.software-db.enabled", havingValue = "true")
public class JdbcHealthAgentConversationRepository implements HealthAgentConversationRepository {
    private final JdbcTemplate jdbc;

    public JdbcHealthAgentConversationRepository(
            @Qualifier("rehealthSoftwareJdbcTemplate") JdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public String resolveConversation(
            String tenantId,
            String userId,
            String requestedConversationId,
            String title
    ) {
        requireIdentity(tenantId, userId);
        if (requestedConversationId != null && !requestedConversationId.isBlank()) {
            List<String> owned = jdbc.query(
                    "SELECT id FROM rehealth_ai_conversation WHERE id = ? AND tenant_id = ? AND user_id = ?",
                    (rs, rowNum) -> rs.getString(1),
                    requestedConversationId,
                    tenantId,
                    userId
            );
            if (!owned.isEmpty()) {
                return owned.get(0);
            }
            Integer existing = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM rehealth_ai_conversation WHERE id = ?",
                    Integer.class,
                    requestedConversationId
            );
            if (existing != null && existing > 0) {
                throw new HealthAgentRequestException(404, "health-agent conversation not found");
            }
        }
        String conversationId = requestedConversationId == null || requestedConversationId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedConversationId;
        Timestamp now = Timestamp.from(Instant.now());
        try {
            jdbc.update("""
                    INSERT INTO rehealth_ai_conversation (
                        id, tenant_id, user_id, title, status, summary_text, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, 'ACTIVE', NULL, ?, ?)
                    """, conversationId, tenantId, userId, bounded(title, 128), now, now);
        } catch (DuplicateKeyException duplicate) {
            throw new HealthAgentRequestException(409, "health-agent conversation already exists");
        }
        return conversationId;
    }

    @Override
    public Optional<HealthAgentRequestState> findRequestState(
            String tenantId,
            String userId,
            String conversationId,
            String requestId
    ) {
        requireOwnedConversation(tenantId, userId, conversationId);
        List<String> userMessages = jdbc.query("""
                        SELECT content FROM rehealth_ai_message
                        WHERE conversation_id = ? AND tenant_id = ? AND user_id = ?
                          AND request_id = ? AND role = 'USER'
                        LIMIT 1
                        """, (rs, rowNum) -> rs.getString("content"),
                conversationId, tenantId, userId, requestId);
        if (userMessages.isEmpty()) {
            return Optional.empty();
        }
        List<HealthAgentResponseDto> responses = jdbc.query("""
                        SELECT id, request_id, content, status, provider, model_version, retryable, created_at
                        FROM rehealth_ai_message
                        WHERE conversation_id = ? AND tenant_id = ? AND user_id = ?
                          AND request_id = ? AND role = 'ASSISTANT'
                        LIMIT 1
                        """, (rs, rowNum) -> mapResponse(conversationId, rs),
                conversationId, tenantId, userId, requestId);
        return Optional.of(new HealthAgentRequestState(
                userMessages.get(0),
                responses.isEmpty() ? null : responses.get(0)
        ));
    }

    @Override
    public void saveUserMessage(
            String tenantId,
            String userId,
            String conversationId,
            String messageId,
            String requestId,
            String content
    ) {
        requireOwnedConversation(tenantId, userId, conversationId);
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                INSERT INTO rehealth_ai_message (
                    id, conversation_id, tenant_id, user_id, request_id, role, content,
                    status, provider, model_version, retryable, created_at
                ) VALUES (?, ?, ?, ?, ?, 'USER', ?, 'accepted', NULL, NULL, 0, ?)
                """, messageId, conversationId, tenantId, userId, requestId, content, now);
        touchConversation(conversationId, tenantId, userId, now);
    }

    @Override
    @Transactional
    public HealthAgentHistoryMessageDto saveAssistantMessage(
            String tenantId,
            String userId,
            String conversationId,
            String requestId,
            HealthAgentResponseDto response
    ) {
        requireOwnedConversation(tenantId, userId, conversationId);
        List<String> existingIds = jdbc.query("""
                        SELECT id FROM rehealth_ai_message
                        WHERE conversation_id = ? AND tenant_id = ? AND user_id = ?
                          AND request_id = ? AND role = 'ASSISTANT'
                        LIMIT 1
                        """, (rs, rowNum) -> rs.getString("id"),
                conversationId, tenantId, userId, requestId);
        String messageId = existingIds.isEmpty() ? UUID.randomUUID().toString() : existingIds.get(0);
        Timestamp now = Timestamp.from(Instant.now());
        if (existingIds.isEmpty()) {
            jdbc.update("""
                    INSERT INTO rehealth_ai_message (
                        id, conversation_id, tenant_id, user_id, request_id, role, content,
                        status, provider, model_version, retryable, created_at
                    ) VALUES (?, ?, ?, ?, ?, 'ASSISTANT', ?, ?, ?, ?, ?, ?)
                    """, messageId, conversationId, tenantId, userId, requestId,
                    response.answer, response.status, response.provider, response.modelVersion,
                    Boolean.TRUE.equals(response.retryable), now);
        } else {
            jdbc.update("""
                    UPDATE rehealth_ai_message
                    SET content = ?, status = ?, provider = ?, model_version = ?, retryable = ?, created_at = ?
                    WHERE id = ? AND conversation_id = ? AND tenant_id = ? AND user_id = ?
                    """, response.answer, response.status, response.provider, response.modelVersion,
                    Boolean.TRUE.equals(response.retryable), now, messageId, conversationId, tenantId, userId);
        }
        touchConversation(conversationId, tenantId, userId, now);
        response.conversationId = conversationId;
        response.messageId = messageId;
        response.createdAt = now.getTime();
        return mapMessage(
                messageId, requestId, "ASSISTANT", response.answer, response.status,
                response.provider, response.modelVersion, now
        );
    }

    @Override
    public List<HealthAgentHistoryMessageDto> findRecentMessages(
            String tenantId,
            String userId,
            String conversationId,
            int limit
    ) {
        requireOwnedConversation(tenantId, userId, conversationId);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        List<HealthAgentHistoryMessageDto> descending = jdbc.query("""
                        SELECT id, request_id, role, content, status, provider, model_version, created_at
                        FROM rehealth_ai_message
                        WHERE conversation_id = ? AND tenant_id = ? AND user_id = ?
                        ORDER BY created_at DESC, id DESC
                        LIMIT ?
                        """, (rs, rowNum) -> mapMessage(rs),
                conversationId, tenantId, userId, safeLimit);
        java.util.Collections.reverse(descending);
        return descending;
    }

    @Override
    public Optional<HealthAgentConversationDto> findLatestConversation(
            String tenantId,
            String userId,
            int messageLimit
    ) {
        requireIdentity(tenantId, userId);
        List<HealthAgentConversationDto> conversations = jdbc.query("""
                        SELECT id, title, status, created_at, updated_at
                        FROM rehealth_ai_conversation
                        WHERE tenant_id = ? AND user_id = ? AND status = 'ACTIVE'
                        ORDER BY updated_at DESC, id DESC
                        LIMIT 1
                        """, (rs, rowNum) -> {
                    HealthAgentConversationDto dto = new HealthAgentConversationDto();
                    dto.conversationId = rs.getString("id");
                    dto.title = rs.getString("title");
                    dto.status = rs.getString("status");
                    dto.createdAt = rs.getTimestamp("created_at").getTime();
                    dto.updatedAt = rs.getTimestamp("updated_at").getTime();
                    return dto;
                }, tenantId, userId);
        if (conversations.isEmpty()) {
            return Optional.empty();
        }
        HealthAgentConversationDto conversation = conversations.get(0);
        conversation.messages = findRecentMessages(
                tenantId, userId, conversation.conversationId, Math.max(1, Math.min(messageLimit, 200))
        );
        return Optional.of(conversation);
    }

    private void requireOwnedConversation(String tenantId, String userId, String conversationId) {
        requireIdentity(tenantId, userId);
        Integer count = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM rehealth_ai_conversation
                        WHERE id = ? AND tenant_id = ? AND user_id = ?
                        """, Integer.class, conversationId, tenantId, userId);
        if (count == null || count == 0) {
            throw new HealthAgentRequestException(404, "health-agent conversation not found");
        }
    }

    private void requireIdentity(String tenantId, String userId) {
        if (tenantId == null || tenantId.isBlank() || userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("authenticated tenant and user are required");
        }
    }

    private void touchConversation(String conversationId, String tenantId, String userId, Timestamp now) {
        jdbc.update("""
                UPDATE rehealth_ai_conversation SET updated_at = ?
                WHERE id = ? AND tenant_id = ? AND user_id = ?
                """, now, conversationId, tenantId, userId);
    }

    private HealthAgentHistoryMessageDto mapMessage(ResultSet rs) throws SQLException {
        return mapMessage(
                rs.getString("id"), rs.getString("request_id"), rs.getString("role"),
                rs.getString("content"), rs.getString("status"), rs.getString("provider"),
                rs.getString("model_version"), rs.getTimestamp("created_at")
        );
    }

    private HealthAgentHistoryMessageDto mapMessage(
            String id,
            String requestId,
            String role,
            String content,
            String status,
            String provider,
            String modelVersion,
            Timestamp createdAt
    ) {
        HealthAgentHistoryMessageDto dto = new HealthAgentHistoryMessageDto();
        dto.messageId = id;
        dto.requestId = requestId;
        dto.role = role;
        dto.content = content;
        dto.status = status;
        dto.provider = provider;
        dto.modelVersion = modelVersion;
        dto.createdAt = createdAt.getTime();
        return dto;
    }

    private HealthAgentResponseDto mapResponse(String conversationId, ResultSet rs) throws SQLException {
        HealthAgentResponseDto response = new HealthAgentResponseDto();
        response.requestId = rs.getString("request_id");
        response.conversationId = conversationId;
        response.messageId = rs.getString("id");
        response.answer = rs.getString("content");
        response.status = rs.getString("status");
        response.provider = rs.getString("provider");
        response.modelVersion = rs.getString("model_version");
        response.retryable = rs.getBoolean("retryable");
        response.isDemo = false;
        response.medicalDisclaimer = HealthAgentResponseDefaults.MEDICAL_DISCLAIMER_ZH;
        response.createdAt = rs.getTimestamp("created_at").getTime();
        return response;
    }

    private String bounded(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "健康问答";
        }
        String trimmed = value.strip();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
