package com.rehealth.genie.ring.viomi

import com.rehealth.genie.network.dto.ViomiMeasurementDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViomiMeasurementMappingTest {
    @Test
    fun `blood pressure maps to user and device scoped Room row`() {
        val row = ViomiMeasurementDto(
            id = "bp-1",
            metricType = "BLOOD_PRESSURE",
            measuredAt = 1_000L,
            primaryValue = 128.0,
            secondaryValue = 79.0,
            unit = "vendor-unit",
        ).toScopedEntityOrNull("user-1", "viomi-device-1", now = 2_000L)

        requireNotNull(row)
        assertEquals(128.0, row.primaryValue)
        assertEquals(79.0, row.secondaryValue)
        assertEquals("mmHg", row.unit)
        assertEquals("user-1", row.ownerUserId)
        assertEquals("viomi-device-1", row.deviceId)
        assertEquals(VIOMI_SOURCE, row.source)

        val otherOwnerRow = ViomiMeasurementDto(
            id = "bp-1",
            metricType = "BLOOD_PRESSURE",
            measuredAt = 1_000L,
            primaryValue = 128.0,
            secondaryValue = 79.0,
            unit = "vendor-unit",
        ).toScopedEntityOrNull("user-2", "viomi-device-1", now = 2_000L)
        requireNotNull(otherOwnerRow)
        assertNotEquals(row.id, otherOwnerRow.id)
    }

    @Test
    fun `invalid or unsupported cloud measurement is rejected`() {
        val invalidPressure = ViomiMeasurementDto(
            id = "bp-invalid",
            metricType = "BLOOD_PRESSURE",
            measuredAt = 1_000L,
            primaryValue = 70.0,
            secondaryValue = 120.0,
            unit = "mmHg",
        )
        val unsupported = ViomiMeasurementDto(
            id = "temp-1",
            metricType = "TEMPERATURE",
            measuredAt = 1_000L,
            primaryValue = 36.5,
            unit = "C",
        )

        assertNull(invalidPressure.toScopedEntityOrNull("user-1", "device-1", now = 2_000L))
        assertNull(unsupported.toScopedEntityOrNull("user-1", "device-1", now = 2_000L))
    }

    @Test
    fun `Viomi device id is stable and does not expose imei`() {
        val id = viomiDeviceId("123456789012345")

        assertEquals(id, viomiDeviceId("123456789012345"))
        assertTrue(id.startsWith("viomi-"))
        assertTrue("123456789012345" !in id)
    }
}
