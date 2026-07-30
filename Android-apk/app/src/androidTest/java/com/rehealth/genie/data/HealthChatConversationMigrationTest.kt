package com.rehealth.genie.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthChatConversationMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Before
    fun deleteExistingDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun cleanUpDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migration6To7PreservesMessagesAndCreatesConversationSummaries() {
        helper.createDatabase(TEST_DATABASE, 6).apply {
            execSQL(
                """
                INSERT INTO health_chat_messages (
                    message_id, user_id, conversation_id, request_id, role, content,
                    delivery_status, provider, model_version, created_at
                ) VALUES (
                    'message-old', 'user-a', 'conversation-old', 'request-old', 'USER',
                    '较早的问题', 'SYNCED', NULL, NULL, 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO health_chat_messages (
                    message_id, user_id, conversation_id, request_id, role, content,
                    delivery_status, provider, model_version, created_at
                ) VALUES (
                    'message-latest', 'user-a', 'conversation-latest', 'request-latest', 'USER',
                    '最新的健康问题', 'SYNCED', NULL, NULL, 2000
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            7,
            true,
            AppDatabase.Migration6To7,
        ).apply {
            query(
                """
                SELECT conversation_id, title, is_active, is_deleted
                FROM health_chat_conversations
                WHERE user_id = 'user-a'
                ORDER BY updated_at ASC
                """.trimIndent(),
            ).use { cursor ->
                assertEquals(2, cursor.count)
                assertTrue(cursor.moveToFirst())
                assertEquals("conversation-old", cursor.getString(0))
                assertEquals("较早的问题", cursor.getString(1))
                assertEquals(0, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
                assertTrue(cursor.moveToNext())
                assertEquals("conversation-latest", cursor.getString(0))
                assertEquals("最新的健康问题", cursor.getString(1))
                assertEquals(1, cursor.getInt(2))
                assertEquals(0, cursor.getInt(3))
            }
            query("SELECT COUNT(*) FROM health_chat_messages").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }
            close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "health-chat-migration-test"
    }
}
