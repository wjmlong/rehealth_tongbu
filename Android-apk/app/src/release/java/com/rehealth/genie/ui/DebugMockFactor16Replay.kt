package com.rehealth.genie.ui

import com.rehealth.genie.features.CvdFeatureVector
import com.rehealth.genie.features.BaselineHealthProfile

internal object DebugMockFactor16Replay {
    @Suppress("UNUSED_PARAMETER")
    fun matchesRuleVersion(ruleVersion: String?): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun completeBaselineProfile(
        profile: BaselineHealthProfile?,
        enabled: Boolean,
        nowMillis: Long,
    ): BaselineHealthProfile? = null

    @Suppress("UNUSED_PARAMETER")
    fun evaluate(
        vector: CvdFeatureVector,
        enabled: Boolean,
        nowMillis: Long,
    ): Factor16ContributionSnapshot? = null
}
