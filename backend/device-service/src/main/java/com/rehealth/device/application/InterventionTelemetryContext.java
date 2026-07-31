package com.rehealth.device.application;

import java.util.List;

/**
 * Bounded, read-only telemetry context for personalized intervention generation.
 * Values are descriptive observations; a trend is not itself a clinical improvement.
 */
public record InterventionTelemetryContext(
        long generatedAt,
        String localDate,
        String timeZone,
        Long latestDataAt,
        TodayBehavior todayBehavior,
        List<RecentChange> recentChanges
) {
    public record TodayBehavior(
            int steps,
            int activeMinutes,
            double activityCaloriesKcal,
            Double averageActivityHeartRate,
            Integer sleepMinutes,
            Long sleepEndedAt,
            List<DietSnapshot> dietRecords,
            List<MetricSnapshot> measurements
    ) {
    }

    public record DietSnapshot(
            String mealType,
            String description,
            Long consumedAt,
            Double caloriesKcal,
            Double proteinGrams,
            Double carbohydrateGrams,
            Double fatGrams,
            Double fiberGrams,
            Double sodiumMilligrams,
            String source
    ) {
    }

    public record MetricSnapshot(
            String metricType,
            Double latestValue,
            Double averageValue,
            Double minimumValue,
            Double maximumValue,
            String unit,
            int sampleCount,
            Long latestObservedAt
    ) {
    }

    public record RecentChange(
            String metricType,
            String unit,
            Double recentAverage,
            Double previousAverage,
            Double delta,
            String trend,
            int recentSampleCount,
            int previousSampleCount
    ) {
    }
}
