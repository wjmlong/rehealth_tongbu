package com.rehealth.genie.ring.hband

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest

class HBandCommandQueueTest {
    @Test
    fun serializesSdkCommands() = runTest {
        val queue = HBandCommandQueue()
        val events = mutableListOf<String>()
        listOf("first", "second").map { name ->
            async {
                queue.execute(1_000) {
                    events += "$name-start"
                    delay(10)
                    events += "$name-end"
                }
            }
        }.awaitAll()

        assertEquals(listOf("first-start", "first-end", "second-start", "second-end"), events)
    }

    @Test
    fun timesOutAStalledCommand() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            HBandCommandQueue().execute(10) { delay(20) }
        }
    }
}
