package com.rehealth.genie.ui

import com.rehealth.genie.features.BaselineHealthProfile
import com.rehealth.genie.features.CvdFeatureVector

@Suppress("UNUSED_PARAMETER")
internal fun isRuntimeFactorContributionConfirmed(ruleVersion: String?): Boolean = false

internal object RuntimeFactor16Fallback {
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
