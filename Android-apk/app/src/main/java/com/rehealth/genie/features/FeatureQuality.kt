package com.rehealth.genie.features

enum class FeatureSource {
    REAL_DEVICE,
    USER_REPORTED,
    CLINICAL_REPORT,
    DERIVED,
    UNKNOWN,
}

enum class FeatureQualityStatus {
    VALID,
    MISSING,
    STALE,
    LOW_CONFIDENCE,
}

data class FeatureQuality(
    val status: FeatureQualityStatus,
    val source: FeatureSource,
    val observedAt: Long? = null,
    val reason: String,
) {
    companion object {
        fun valid(source: FeatureSource, observedAt: Long? = null, reason: String): FeatureQuality =
            FeatureQuality(
                status = FeatureQualityStatus.VALID,
                source = source,
                observedAt = observedAt,
                reason = reason,
            )

        fun missing(reason: String): FeatureQuality =
            FeatureQuality(
                status = FeatureQualityStatus.MISSING,
                source = FeatureSource.UNKNOWN,
                reason = reason,
            )

        fun stale(source: FeatureSource, observedAt: Long? = null, reason: String): FeatureQuality =
            FeatureQuality(
                status = FeatureQualityStatus.STALE,
                source = source,
                observedAt = observedAt,
                reason = reason,
            )

        fun lowConfidence(source: FeatureSource, observedAt: Long? = null, reason: String): FeatureQuality =
            FeatureQuality(
                status = FeatureQualityStatus.LOW_CONFIDENCE,
                source = source,
                observedAt = observedAt,
                reason = reason,
            )
    }
}
