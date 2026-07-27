package com.rehealth.genie.ring

import android.content.Context
import com.rehealth.genie.ring.data.RingDataDao
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter

/** Release builds always use the real MRD implementation and contain no mock repository. */
internal fun createRuntimeRingRepository(
    context: Context,
    dao: RingDataDao,
    protocolAdapter: MrdProtocolAdapter,
): RingRepository = MrdBleRingRepository(context, dao, protocolAdapter)
