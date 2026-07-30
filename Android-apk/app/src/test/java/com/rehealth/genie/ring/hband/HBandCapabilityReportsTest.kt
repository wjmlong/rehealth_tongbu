package com.rehealth.genie.ring.hband

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HBandCapabilityReportsTest {
    @Test
    fun `HRV accepts every explicit SDK capability signal`() {
        assertTrue(supportsHBandHrv(appDetection = true, deviceFeature = false, hrvType = 0))
        assertTrue(supportsHBandHrv(appDetection = false, deviceFeature = true, hrvType = 0))
        assertTrue(supportsHBandHrv(appDetection = false, deviceFeature = false, hrvType = 1))
        assertFalse(supportsHBandHrv(appDetection = false, deviceFeature = false, hrvType = 0))
    }

    @Test
    fun `MET accepts feature flag or non-zero protocol type`() {
        assertTrue(supportsHBandMet(feature = true, metType = 0))
        assertTrue(supportsHBandMet(feature = false, metType = 1))
        assertFalse(supportsHBandMet(feature = false, metType = 0))
    }

    @Test
    fun `numbered package overrides incomplete deprecated aggregate report`() = runTest {
        val reports = HBandCapabilityReports(quietPeriodMillis = 0)

        reports.reportAggregate(HBandCapabilities(heartRate = false, hrv = false, ecg = false))
        reports.reportPackage(
            1,
            HBandCapabilityPatch(heartRate = true),
        )
        reports.reportPackage(
            2,
            HBandCapabilityPatch(watchDataDays = 7, hrv = true, ecg = true),
        )

        val capabilities = reports.awaitSettled()

        assertTrue(capabilities.heartRate)
        assertTrue(capabilities.hrv)
        assertTrue(capabilities.ecg)
        assertEquals(7, capabilities.watchDataDays)
    }

    @Test
    fun `later aggregate callback cannot overwrite authoritative package fields`() = runTest {
        val reports = HBandCapabilityReports(quietPeriodMillis = 0)

        reports.reportPackage(2, HBandCapabilityPatch(ecg = true))
        reports.reportAggregate(HBandCapabilities(ecg = false, met = true))

        val capabilities = reports.awaitSettled()

        assertTrue(capabilities.ecg)
        assertTrue(capabilities.met)
    }

    @Test
    fun `legacy-only device retains explicit unsupported capability`() = runTest {
        val reports = HBandCapabilityReports(quietPeriodMillis = 0)
        reports.reportAggregate(HBandCapabilities(heartRate = true, ecg = false))

        val capabilities = reports.awaitSettled()

        assertTrue(capabilities.heartRate)
        assertFalse(capabilities.ecg)
    }
}
