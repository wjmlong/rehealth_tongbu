package com.rehealth.genie.ring.hband

import kotlin.test.Test
import kotlin.test.assertNotNull

class HBandSdkRuntimeDependencyTest {
    @Test
    fun `packages vendor classes required through HBand connection callbacks`() {
        val classLoader = javaClass.classLoader

        assertNotNull(Class.forName("com.inuker.bluetooth.library.jieli.ota.JLOTAManager", false, classLoader))
        assertNotNull(Class.forName("com.jieli.jl_rcsp.impl.WatchOpImpl", false, classLoader))
        assertNotNull(Class.forName("com.jieli.jl_rcsp.interfaces.watch.OnWatchCallback", false, classLoader))
        assertNotNull(Class.forName("com.veepoo.protocol.listener.data.IBPDetectDataListener", false, classLoader))
        assertNotNull(Class.forName("com.veepoo.protocol.listener.data.IECGDetectListener", false, classLoader))
        assertNotNull(Class.forName("com.veepoo.protocol.model.datas.EcgDetectResult", false, classLoader))
        assertNotNull(Class.forName("io.runtime.mcumgr.McuMgrTransport", false, classLoader))
        assertNotNull(Class.forName("io.runtime.mcumgr.ble.McuMgrBleTransport", false, classLoader))
        assertNotNull(
            Class.forName(
                "no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat",
                false,
                classLoader,
            ),
        )
    }
}
