package org.jeecg.modules.rehealth.repository.impl;

import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.HealthAgentResponseDto;
import org.jeecg.modules.rehealth.service.agent.HealthAgentRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JdbcHealthAgentConversationRepositoryTest {
    private JdbcTemplate jdbc;
    private JdbcHealthAgentConversationRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:health-agent;MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE rehealth_ai_conversation (
                    id VARCHAR(64) PRIMARY KEY,
                    tenant_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    title VARCHAR(128) NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    summary_text CLOB,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE rehealth_ai_message (
                    id VARCHAR(64) PRIMARY KEY,
                    conversation_id VARCHAR(64) NOT NULL,
                    tenant_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL,
                    request_id VARCHAR(128) NOT NULL,
                    role VARCHAR(16) NOT NULL,
                    content CLOB NOT NULL,
                    status VARCHAR(32) NOT NULL,
                    provider VARCHAR(128),
                    model_version VARCHAR(128),
                    retryable BOOLEAN NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    UNIQUE (conversation_id, request_id, role),
                    FOREIGN KEY (conversation_id) REFERENCES rehealth_ai_conversation(id)
                )
                """);
        repository = new JdbcHealthAgentConversationRepository(jdbc);
    }

    @Test
    void persistsRestoresAndIsolatesConversationByTenantAndUser() {
        String conversationId = repository.resolveConversation(
                "tenant-a", "user-a", "conversation-1", "我的健康问题"
        );
        repository.saveUserMessage(
                "tenant-a", "user-a", conversationId, "message-1", "request-1", "如何改善睡眠"
        );
        HealthAgentResponseDto response = new HealthAgentResponseDto();
        response.status = "ok";
        response.answer = "保持规律作息";
        response.provider = "test-provider";
        response.modelVersion = "test-model";
        response.retryable = false;
        repository.saveAssistantMessage("tenant-a", "user-a", conversationId, "request-1", response);

        JdbcHealthAgentConversationRepository restarted = new JdbcHealthAgentConversationRepository(jdbc);
        var restored = restarted.findLatestConversation("tenant-a", "user-a", 100).orElseThrow();

        assertEquals(conversationId, restored.conversationId);
        assertEquals(2, restored.messages.size());
        assertEquals("如何改善睡眠", restored.messages.get(0).content);
        assertEquals("保持规律作息", restored.messages.get(1).content);
        assertEquals(0, restarted.findLatestConversation("tenant-a", "user-b", 100).stream().count());
        assertThrows(
                HealthAgentRequestException.class,
                () -> restarted.resolveConversation("tenant-a", "user-b", conversationId, "越权")
        );
    }
}
