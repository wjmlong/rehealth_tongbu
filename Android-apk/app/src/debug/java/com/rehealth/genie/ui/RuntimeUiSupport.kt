package com.rehealth.genie.ui

import com.rehealth.genie.BuildConfig

internal fun runtimeSmsTestCode(): String? =
    BuildConfig.SMS_TEST_CODE.takeIf { it.isNotBlank() }
