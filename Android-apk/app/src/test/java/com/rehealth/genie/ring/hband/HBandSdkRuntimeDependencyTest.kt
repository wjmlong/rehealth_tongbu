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
        val hrvListener = Class.forName("com.veepoo.protocol.listener.data.IHrvDetectListener", false, classLoader)
        val metListener = Class.forName("com.veepoo.protocol.listener.data.IMetDetectListener", false, classLoader)
        val writeResponse = Class.forName(
            "com.inuker.bluetooth.library.connect.response.BleWriteResponse",
            false,
            classLoader,
        )
        val manager = Class.forName("com.veepoo.protocol.VPOperateManager", false, classLoader)
        val capabilityStore = Class.forName("com.veepoo.protocol.shareprence.VpSpGetUtil", false, classLoader)
        assertNotNull(manager.getMethod("startDetectHrv", writeResponse, hrvListener))
        assertNotNull(manager.getMethod("stopDetectHrv", writeResponse, hrvListener))
        assertNotNull(manager.getMethod("startDetectMet", writeResponse, metListener))
        assertNotNull(manager.getMethod("stopDetectMet", writeResponse))
        assertNotNull(capabilityStore.getMethod("isSupportHrvAppDetect"))
        assertNotNull(capabilityStore.getMethod("isSupportMetAppDetect"))
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
