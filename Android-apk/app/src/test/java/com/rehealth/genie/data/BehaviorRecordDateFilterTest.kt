package com.rehealth.genie.data

import com.rehealth.genie.network.dto.BehaviorRecordDto
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class BehaviorRecordDateFilterTest {
    @Test
    fun `today records exclude adjacent local calendar days and missing timestamps`() {
        val zoneId = ZoneId.of("Asia/Shanghai")
        val date = LocalDate.of(2026, 8, 7)
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val records = listOf(
            BehaviorRecordDto(id = "previous", occurredAt = start - 1),
            BehaviorRecordDto(id = "start", occurredAt = start),
            BehaviorRecordDto(id = "end-minus-one", occurredAt = end - 1),
            BehaviorRecordDto(id = "next", occurredAt = end),
            BehaviorRecordDto(id = "missing", occurredAt = null),
        )

        assertEquals(
            listOf("start", "end-minus-one"),
            records.recordsOn(date, zoneId).map(BehaviorRecordDto::id),
        )
    }
}
