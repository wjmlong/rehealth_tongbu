package com.rehealth.genie.ui

import com.rehealth.genie.BuildConfig
import com.rehealth.genie.phm.PiasAttributionCacheDao
import com.rehealth.genie.phm.PiasAttributionCacheEntity
import com.rehealth.genie.phm.PiasAttributionCacheRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class RuntimeAttributionPiasPreviewTest {
    @Test
    fun `pias preview follows explicit debug seed switch and persists when enabled`() = runTest {
        var cached: PiasAttributionCacheEntity? = null
        val dao = object : PiasAttributionCacheDao {
            override suspend fun upsert(entity: PiasAttributionCacheEntity) {
                cached = entity
            }

            override suspend fun get(userId: String): PiasAttributionCacheEntity? =
                cached?.takeIf { it.userId == userId }
        }
        val repository = PiasAttributionCacheRepository(
            dao = dao,
            userIdProvider = { "debug-user" },
            nowProvider = { 123L },
        )
        val preview = runtimeAttributionPiasResult(repository, historyDays = 0)
        if (!BuildConfig.SEED_FAKE_HEALTH_DATA) {
            assertNull(preview)
            return@runTest
        }

        val seededPreview = assertNotNull(preview)

        assertEquals("ready", seededPreview.status)
        assertEquals(90, seededPreview.historyDays)
        assertEquals(31, seededPreview.forecastNoAction.size)
        assertEquals(31, seededPreview.forecastWithPlan.size)
        assertTrue(seededPreview.headline.orEmpty().contains("Debug 模拟"))
        assertEquals("debug-user", cached?.userId)
        assertEquals(true, cached?.isMock)
        assertEquals("debug-pias-preview-1.0.0", cached?.modelVersion)
    }
}
