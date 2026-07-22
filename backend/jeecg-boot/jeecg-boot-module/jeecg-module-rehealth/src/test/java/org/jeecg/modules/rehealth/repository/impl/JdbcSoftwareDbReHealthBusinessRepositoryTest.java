package org.jeecg.modules.rehealth.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.jeecg.modules.rehealth.mobile.dto.DeviceBindRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.InterventionGenerateResponseDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateRequestDto;
import org.jeecg.modules.rehealth.mobile.dto.RiskEvaluateResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcSoftwareDbReHealthBusinessRepositoryTest {
    private JdbcTemplate jdbcTemplate;
    private JdbcSoftwareDbReHealthBusinessRepository repository;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:software-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        new ResourceDatabasePopulator(
                new ClassPathResource("db/software/mysql/V1__create_rehealth_software_tables.sql")
        ).execute(dataSource);
        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new JdbcSoftwareDbReHealthBusinessRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void persistsDeviceBindingForAuthenticatedUser() {
        DeviceBindRequestDto request = new DeviceBindRequestDto();
        request.deviceId = "ring-001";
        request.model = "MR11";

        assertTrue(repository.recordDeviceBinding("user-a", request).persisted);
        assertEquals(1, count("rehealth_device_binding"));
        assertEquals("user-a", jdbcTemplate.queryForObject(
                "SELECT user_id FROM rehealth_device_binding WHERE device_id = ?",
                String.class,
                "ring-001"
        ));
    }

    @Test
    void latestRiskAndInterventionAreIsolatedByUser() {
        saveRisk("user-a", "request-a", 0.21);
        saveRisk("user-b", "request-b", 0.73);
        InterventionGenerateResponseDto plan = new InterventionGenerateResponseDto();
        plan.planId = "plan-a";
        repository.saveInterventionPlan("user-a", plan);

        assertEquals(0.21, repository.findLatestRiskResult("user-a").orElseThrow().riskScore);
        assertEquals(0.73, repository.findLatestRiskResult("user-b").orElseThrow().riskScore);
        assertEquals("plan-a", repository.findLatestInterventionPlan("user-a").orElseThrow().planId);
        assertTrue(repository.findLatestInterventionPlan("user-b").isEmpty());
    }

    private void saveRisk(String userId, String requestId, double score) {
        RiskEvaluateRequestDto request = new RiskEvaluateRequestDto();
        request.requestId = requestId;
        RiskEvaluateResponseDto response = new RiskEvaluateResponseDto();
        response.riskScore = score;
        response.riskLevel = "moderate";
        response.modelVersion = "test-v1";
        repository.saveRiskResult(userId, requestId, request, response);
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return count == null ? 0 : count;
    }
}
