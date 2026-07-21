package com.rehealth.genie

import android.app.Application
import com.rehealth.genie.data.AppDatabase
import com.rehealth.genie.ring.RingRepository
import com.rehealth.genie.ring.mrd.MrdBleRingRepository
import com.rehealth.genie.ring.mrd.MrdProtocolAdapter

class ReHealthApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val mrdProtocolAdapter by lazy { MrdProtocolAdapter(this) }
    val ringRepository: RingRepository by lazy {
        MrdBleRingRepository(this, database.ringDataDao(), mrdProtocolAdapter)
    }
}
