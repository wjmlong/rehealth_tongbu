package com.rehealth.genie.ring.hband

import com.rehealth.genie.ring.RingMetricType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class HBandCapabilityReportsTest {
    @Test
    fun `HRV separates direct detection from history and mini checkup`() {
        assertTrue(supportsHBandHrvHistory(deviceFeature = true, hrvType = 0))
        assertTrue(supportsHBandHrvHistory(deviceFeature = false, hrvType = 1))
        assertFalse(supportsHBandHrvHistory(deviceFeature = false, hrvType = 0))

        val historyOnly = HBandCapabilities(hrvHistory = true)
        assertTrue(RingMetricType.HRV in historyOnly.supportedMetrics)
        assertFalse(historyOnly.supportsDirectMeasurement(RingMetricType.HRV))

        val miniCheckup = HBandCapabilities(miniCheckup = true)
        assertTrue(RingMetricType.HRV in miniCheckup.supportedMetrics)
        assertTrue(RingMetricType.STRESS in miniCheckup.supportedMetrics)
    }

    @Test
    fun `MET protocol type enables history without claiming direct measurement`() {
        assertTrue(supportsHBandMetricHistory(protocolType = 1))
        assertFalse(supportsHBandMetricHistory(protocolType = 0))

        val historyOnly = HBandCapabilities(metHistory = true)
        assertTrue(RingMetricType.MET in historyOnly.supportedMetrics)
        assertFalse(historyOnly.supportsDirectMeasurement(RingMetricType.MET))
    }

    @Test
    fun `measurement routing avoids unsupported direct SDK commands`() {
        assertEquals(
            HBandMeasurementRoute.MINI_CHECKUP,
            HBandCapabilities(hrv = true, miniCheckup = true).measurementRoute(
                RingMetricType.HRV,
                allowHistoryFallback = true,
            ),
        )
        assertEquals(
            HBandMeasurementRoute.MINI_CHECKUP,
            HBandCapabilities(stress = true, miniCheckup = true).measurementRoute(
                RingMetricType.STRESS,
                allowHistoryFallback = true,
            ),
        )
        assertEquals(
            HBandMeasurementRoute.HISTORY,
            HBandCapabilities(met = true, metHistory = true).measurementRoute(
                RingMetricType.MET,
                allowHistoryFallback = true,
            ),
        )
        assertEquals(
            HBandMeasurementRoute.HISTORY,
            HBandCapabilities(hrv = true).measurementRoute(RingMetricType.HRV, allowHistoryFallback = true),
        )
        assertEquals(
            HBandMeasurementRoute.DIRECT,
            HBandCapabilities(hrv = true).measurementRoute(RingMetricType.HRV, allowHistoryFallback = false),
        )
        assertEquals(
            HBandMeasurementRoute.UNSUPPORTED,
            HBandCapabilities().measurementRoute(RingMetricType.MET, allowHistoryFallback = false),
        )
    }

    @Test
    fun `mini checkup keeps only valid requested HRV or stress result`() {
        val measuredAt = 1_700_000_000_000L
        val hrv = hBandMiniCheckupPayload(
            RingMetricType.HRV,
            measuredAt,
            hrv = 48,
            stress = 62,
        )
        val stress = hBandMiniCheckupPayload(
            RingMetricType.STRESS,
            measuredAt,
            hrv = 48,
            stress = 62,
        )
        val invalid = hBandMiniCheckupPayload(
            RingMetricType.HRV,
            measuredAt,
            hrv = 0,
            stress = 62,
        )
        val zeroStress = hBandMiniCheckupPayload(
            RingMetricType.STRESS,
            measuredAt,
            hrv = 48,
            stress = 0,
        )

        assertEquals(48.0, hrv.measurements.single().value)
        assertEquals("ms", hrv.measurements.single().unit)
        assertEquals(62.0, stress.measurements.single().value)
        assertEquals("score", stress.measurements.single().unit)
        assertTrue(invalid.measurements.isEmpty())
        assertTrue(zeroStress.measurements.isEmpty())
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
