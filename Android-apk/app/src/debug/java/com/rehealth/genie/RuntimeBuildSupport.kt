package com.rehealth.genie

internal fun runtimeJeecgSignSecret(): String? =
    BuildConfig.JEECG_SIGN_SECRET.takeIf { it.isNotBlank() }

internal fun runRuntimeStartupHooks(application: ReHealthApplication) {
    if (BuildConfig.SEED_FAKE_HEALTH_DATA) {
        application.launchRuntimeStartupSync()
    }
}
