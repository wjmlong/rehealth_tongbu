package org.jeecg.modules.rehealth.careplan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CarePlanVersionRequest {
    private CarePlanVersionRequest() {
    }

    public record Item(
            @JsonProperty("logical_item_id") String logicalItemId,
            String category,
            String title,
            String instructions,
            Map<String, Object> schedule,
            @JsonProperty("scoring_weight") BigDecimal scoringWeight,
            @JsonProperty("allow_not_applicable") Boolean allowNotApplicable
    ) {
    }

    public record CreateDraft(
            String title,
            String summary,
            @JsonProperty("change_reason") String changeReason,
            List<Item> items
    ) {
    }

    public record UpdateDraft(
            @JsonProperty("expected_lock_version") Long expectedLockVersion,
            String title,
            String summary,
            @JsonProperty("change_reason") String changeReason,
            List<Item> items
    ) {
    }

    public record CreateRevision(
            @JsonProperty("expected_lock_version") Long expectedLockVersion,
            @JsonProperty("change_reason") String changeReason
    ) {
    }

    public record Publish(
            @JsonProperty("expected_lock_version") Long expectedLockVersion,
            @JsonProperty("effective_at") LocalDateTime effectiveAt
    ) {
    }

    public record DiscardDraft(
            @JsonProperty("expected_lock_version") Long expectedLockVersion,
            String reason
    ) {
    }

    public record Withdraw(
            @JsonProperty("expected_lock_version") Long expectedLockVersion,
            String reason
    ) {
    }
}
