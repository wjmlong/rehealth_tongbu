package com.rehealth.genie.ring

import java.nio.ByteBuffer
import java.nio.ByteOrder

object SignalEncoding {
    fun int32LittleEndian(values: IntArray): ByteArray =
        ByteBuffer.allocate(values.size * Int.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply { values.forEach(::putInt) }
            .array()

    fun decodeInt32LittleEndian(payload: ByteArray): IntArray {
        require(payload.size % Int.SIZE_BYTES == 0)
        val buffer = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        return IntArray(payload.size / Int.SIZE_BYTES) { buffer.int }
    }
}
