package org.jeecg.modules.rehealth.insurance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class InsuranceMobileCarePlanResponse {
    private InsuranceMobileCarePlanResponse() {
    }

    public record Plan(
            Integer tenantId,
            String organizationName,
            String planId,
            String revisionId,
            Integer revisionNo,
            String title,
            String summary,
            LocalDateTime effectiveFrom,
            LocalDateTime effectiveTo,
            Adherence adherence28d,
            List<Item> items
    ) {
    }

    public record Item(
            String itemId,
            String logicalItemId,
            String category,
            String title,
            String instructions,
            BigDecimal scoringWeight,
            Boolean allowNotApplicable,
            String scheduleType,
            Boolean scheduleSupported,
            Occurrence todayOccurrence
    ) {
    }

    public record Occurrence(
            String occurrenceId,
            LocalDateTime scheduledAt,
            LocalDateTime dueAt,
            String feedbackType,
            BigDecimal scoreValue
    ) {
    }

    public record Adherence(
            Integer windowDays,
            BigDecimal scorePercent,
            Integer expectedCount,
            Integer scoredCount,
            Integer excludedCount,
            String calculationVersion
    ) {
    }
}
