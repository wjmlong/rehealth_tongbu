package com.rehealth.device.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class TimescaleUserHealthScopeTest {
    @Test
    void everyAdminHealthQueryScopesTenantAndUser() {
        assertFalse(TimescaleTelemetryReader.USER_HEALTH_SQL.isEmpty());
        for (String sql : TimescaleTelemetryReader.USER_HEALTH_SQL) {
            String normalized = sql.toLowerCase();
            assertTrue(normalized.contains("tenant_id = ?"), sql);
            assertTrue(normalized.contains("user_id = ?"), sql);
        }
    }

    @Test
    void provenanceQueryUsesOnlyDistinctServerSideSources() {
        String sql = TimescaleTelemetryReader.USER_HEALTH_SQL.get(7).toLowerCase();
        assertTrue(sql.contains("select distinct source"));
        assertFalse(sql.contains("primary_value"));
    }

    @Test
    void knownSimulationAndSampleSourcesAreAlwaysMarkedSynthetic() {
        for (String source : List.of(
                "LOCAL_TEST_SEED", "ring_sim", "mock-provider",
                "demo-import", "sample_batch", "synthetic-generator")) {
            assertTrue(TimescaleTelemetryReader.isSyntheticProvenance(List.of(source)), source);
        }
        assertFalse(TimescaleTelemetryReader.isSyntheticProvenance(List.of("hband")));
    }
}
