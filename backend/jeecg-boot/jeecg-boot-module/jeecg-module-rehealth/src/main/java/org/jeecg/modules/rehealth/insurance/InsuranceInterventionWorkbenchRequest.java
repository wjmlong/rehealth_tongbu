package org.jeecg.modules.rehealth.insurance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public final class InsuranceInterventionWorkbenchRequest {
    private InsuranceInterventionWorkbenchRequest() {
    }

    public record CreateAction(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("action_type") String actionType,
            String title,
            String content,
            @JsonProperty("assignee_user_id") String assigneeUserId,
            @JsonProperty("due_at") LocalDateTime dueAt,
            @JsonProperty("request_id") String requestId
    ) {
    }

    public record UpdateAction(
            String status,
            @JsonProperty("assignee_user_id") String assigneeUserId,
            @JsonProperty("due_at") LocalDateTime dueAt,
            Map<String, Object> result
    ) {
    }
}
