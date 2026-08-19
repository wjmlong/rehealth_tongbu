package org.jeecg.modules.rehealth.careplan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

public final class CarePlanVersionResponse {
    private CarePlanVersionResponse() {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Plan(
            @JsonProperty("plan_id") String planId,
            @JsonProperty("subject_id") String subjectId,
            @JsonProperty("owner_type") String ownerType,
            String status,
            @JsonProperty("lock_version") long lockVersion,
            @JsonProperty("current_revision_id") String currentRevisionId,
            @JsonProperty("draft_revision_id") String draftRevisionId,
            List<Revision> revisions,
            @JsonProperty("updated_at") String updatedAt
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Revision(
            @JsonProperty("revision_id") String revisionId,
            @JsonProperty("revision_no") int revisionNo,
            String status,
            String title,
            String summary,
            @JsonProperty("change_reason") String changeReason,
            @JsonProperty("content_hash") String contentHash,
            @JsonProperty("effective_from") String effectiveFrom,
            @JsonProperty("effective_to") String effectiveTo,
            @JsonProperty("published_at") String publishedAt,
            List<Item> items
    ) {
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Item(
            @JsonProperty("item_id") String itemId,
            @JsonProperty("logical_item_id") String logicalItemId,
            String category,
            String title,
            String instructions,
            JsonNode schedule,
            @JsonProperty("scoring_weight") BigDecimal scoringWeight,
            @JsonProperty("allow_not_applicable") boolean allowNotApplicable,
            @JsonProperty("display_order") int displayOrder
    ) {
    }
}
