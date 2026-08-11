package com.rehealth.genie.phm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class PiasAttributionCacheRepositoryTest {
    @Test
    fun `mock cache is persisted but hidden unless explicitly allowed`() = runTest {
        val dao = InMemoryPiasDao()
        val repository = PiasAttributionCacheRepository(
            dao = dao,
            userIdProvider = { "user-1" },
            nowProvider = { 99L },
        )
        val result = IndividualAttributionResult(
            status = "ready",
            historyDays = 90,
            headline = "Debug 模拟",
            forecastNoAction = listOf(0.2, 0.3),
            forecastWithPlan = listOf(0.2, 0.1),
        )

        assertTrue(repository.save(result, isMock = true, modelVersion = "debug-v1"))
        assertNull(repository.load())
        val restored = assertNotNull(repository.load(allowMock = true))

        assertEquals(result, restored)
        assertEquals(99L, dao.row?.updatedAt)
        assertEquals("debug-v1", dao.row?.modelVersion)
    }

    @Test
    fun `real cache remains readable without mock permission`() = runTest {
        val dao = InMemoryPiasDao()
        val repository = PiasAttributionCacheRepository(dao, { "user-2" })
        val result = IndividualAttributionResult(status = "ready", historyDays = 45)

        assertTrue(repository.save(result, isMock = false, modelVersion = "pias-real-v1"))
        assertFalse(dao.row?.isMock ?: true)
        assertEquals(result, repository.load())
    }

    @Test
    fun `cache is not written without authenticated user`() = runTest {
        val dao = InMemoryPiasDao()
        val repository = PiasAttributionCacheRepository(dao, { null })

        assertFalse(
            repository.save(
                IndividualAttributionResult(status = "ready"),
                isMock = true,
                modelVersion = "debug-v1",
            ),
        )
        assertNull(dao.row)
    }

    private class InMemoryPiasDao : PiasAttributionCacheDao {
        var row: PiasAttributionCacheEntity? = null

        override suspend fun upsert(entity: PiasAttributionCacheEntity) {
            row = entity
        }

        override suspend fun get(userId: String): PiasAttributionCacheEntity? =
            row?.takeIf { it.userId == userId }
    }
}
