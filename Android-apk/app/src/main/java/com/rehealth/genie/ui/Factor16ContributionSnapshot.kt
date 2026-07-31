package com.rehealth.genie.ui

internal const val FACTOR16_RULE_VERSION = "factor16-rule-v1.0.0"
internal const val DEBUG_MOCK_FACTOR16_RULE_VERSION = "factor16-rule-v1.0.0-debug-mock"

internal data class Factor16ContributionSnapshot(
    val contributions: Map<String, Double>,
    val measuredComponents: Map<String, Double>,
    val controlSupportComponents: Map<String, Double>,
    val ruleVersion: String,
)
