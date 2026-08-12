package org.jeecg.modules.rehealth.website;

import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;

public record WebsiteRecord(
        String id,
        String tenantId,
        String resourceType,
        String status,
        JsonNode payload,
        String createdBy,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
