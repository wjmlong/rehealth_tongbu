package com.rehealth.genie.ring

import android.content.Context
import android.os.Build
import com.rehealth.genie.BuildConfig
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter

internal fun createRuntimeRingRepository(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
): RingRepository =
    if (BuildConfig.USE_FAKE_RING || (BuildConfig.SEED_FAKE_HEALTH_DATA && isProbablyEmulator())) {
        MockRingRepository(dao)
    } else {
        MrdBleRingRepository(context, dao, protocolAdapter)
    }

private fun isProbablyEmulator(): Boolean {
    val fingerprint = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val product = Build.PRODUCT.lowercase()
    val brand = Build.BRAND.lowercase()
    return fingerprint.contains("generic") ||
        fingerprint.contains("emulator") ||
        model.contains("emulator") ||
        model.contains("android sdk built for") ||
        product.contains("sdk") ||
        product.contains("emulator") ||
        brand.contains("generic")
}
