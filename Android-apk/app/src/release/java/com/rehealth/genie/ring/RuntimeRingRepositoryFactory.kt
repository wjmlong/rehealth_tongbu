package com.rehealth.genie.ring

import android.content.Context
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter
import com.rehealth.genie.ring.provider.ActiveWearableBindingStore
import com.rehealth.genie.ring.provider.DEFAULT_MRD_PRODUCT_CODE
import com.rehealth.genie.ring.provider.WearableVendor

/** Release builds register only the real MRD implementation and contain no mock repository. */
internal fun createRuntimeRingProviderFactories(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
    activeWearableStore: ActiveWearableBindingStore,
): Map<WearableVendor, () -> RingRepository> = mapOf(
    WearableVendor.MRD to {
        MrdBleRingRepository(context, dao, protocolAdapter, activeWearableStore)
    },
)

internal fun runtimeDefaultWearableSelection(): Pair<String, WearableVendor> =
    DEFAULT_MRD_PRODUCT_CODE to WearableVendor.MRD

internal fun shouldForceRuntimeWearableSelection(): Boolean = false
