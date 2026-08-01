package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.BehaviorRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcBehaviorRecordRepositoryTest {
    private JdbcBehaviorRecordRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:behavior-records;MODE=MySQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE rehealth_behavior_record (
                    id VARCHAR(64) PRIMARY KEY, tenant_id VARCHAR(64) NOT NULL,
                    user_id VARCHAR(64) NOT NULL, request_id VARCHAR(128) NOT NULL,
                    category VARCHAR(32) NOT NULL, title VARCHAR(255) NOT NULL,
                    summary VARCHAR(2000), items_json CLOB, calories_kcal DECIMAL(10,2),
                    protein_grams DECIMAL(10,2), carbohydrate_grams DECIMAL(10,2),
                    fat_grams DECIMAL(10,2), ocr_text CLOB, confidence DOUBLE,
                    model_version VARCHAR(128) NOT NULL, occurred_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL, UNIQUE (tenant_id, user_id, request_id)
                )
                """);
        repository = new JdbcBehaviorRecordRepository(jdbc, new ObjectMapper());
    }

    @Test
    void persistsAndQueriesOnlyAuthorizedOwnerWindow() {
        long timestamp = Instant.parse("2026-07-31T04:00:00Z").toEpochMilli();
        BehaviorRecordDto record = new BehaviorRecordDto();
        record.id = "record-1";
        record.requestId = "request-1";
        record.category = "FOOD";
        record.title = "早餐";
        record.summary = "图像估算";
        record.items = List.of("鸡蛋", "牛奶");
        record.caloriesKcal = 320.0;
        record.modelVersion = "gpt-5.6-luna";
        record.occurredAt = timestamp;
        record.createdAt = timestamp + 1000;

        repository.save("tenant-a", "user-a", record);

        assertEquals("早餐", repository.findByRequestId("tenant-a", "user-a", "request-1").orElseThrow().title);
        assertEquals(1, repository.findInWindow(
                "tenant-a", "user-a", Instant.parse("2026-07-31T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z")
        ).size());
        assertTrue(repository.findInWindow(
                "tenant-a", "user-b", Instant.parse("2026-07-31T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z")
        ).isEmpty());
    }
}
