package com.rehealth.genie.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SignInterceptorTest {
    private val server = MockWebServer()

    @AfterTest
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `signs sms request with sorted body and timestamp`() {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(SignInterceptor("dev-secret"))
            .build()
        val request = Request.Builder()
            .url(server.url("/jeecg-boot/sys/sms"))
            .post(
                """{"smsmode":"1","mobile":"13800138000"}"""
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
        }

        val recorded = server.takeRequest()
        assertEquals("2FFB66646DB9471758B91854E255860C", recorded.getHeader("X-Sign"))
        assertNotNull(recorded.getHeader("X-Timestamp")?.toLongOrNull())
    }

    @Test
    fun `does not sign unrelated request`() {
        server.start()
        server.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(SignInterceptor("dev-secret"))
            .build()
        val request = Request.Builder()
            .url(server.url("/jeecg-boot/sys/user/register"))
            .post("{}".toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().close()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("X-Sign"))
        assertNull(recorded.getHeader("X-Timestamp"))
    }
}
