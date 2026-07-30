package com.rehealth.genie.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "health_chat_conversations",
    primaryKeys = ["user_id", "conversation_id"],
    indices = [
        Index(value = ["user_id", "updated_at"]),
        Index(value = ["user_id", "is_active"]),
    ],
)
data class HealthChatConversationEntity(
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,
)

@Entity(
    tableName = "health_chat_messages",
    indices = [
        Index(value = ["user_id", "created_at"]),
        Index(value = ["user_id", "conversation_id", "created_at"]),
        Index(value = ["user_id", "request_id"]),
    ],
)
data class HealthChatMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "conversation_id")
    val conversationId: String,
    @ColumnInfo(name = "request_id")
    val requestId: String?,
    val role: String,
    val content: String,
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: String,
    val provider: String? = null,
    @ColumnInfo(name = "model_version")
    val modelVersion: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Dao
interface HealthChatDao {
    @Query(
        """
        SELECT * FROM health_chat_conversations
        WHERE user_id = :userId AND is_deleted = 0
        ORDER BY updated_at DESC, conversation_id DESC
        """,
    )
    fun observeConversations(userId: String): Flow<List<HealthChatConversationEntity>>

    @Query(
        """
        SELECT conversation_id FROM health_chat_conversations
        WHERE user_id = :userId AND is_active = 1 AND is_deleted = 0
        ORDER BY updated_at DESC, conversation_id DESC
        LIMIT 1
        """,
    )
    fun observeActiveConversationId(userId: String): Flow<String?>

    @Query(
        """
        SELECT * FROM health_chat_messages
        WHERE user_id = :userId
          AND conversation_id = (
              SELECT conversation_id FROM health_chat_conversations
              WHERE user_id = :userId
                AND is_active = 1
                AND is_deleted = 0
              ORDER BY updated_at DESC, conversation_id DESC
              LIMIT 1
          )
        ORDER BY created_at ASC, message_id ASC
        """,
    )
    fun observeActiveConversation(userId: String): Flow<List<HealthChatMessageEntity>>

    @Query(
        """
        SELECT conversation_id FROM health_chat_conversations
        WHERE user_id = :userId AND is_active = 1 AND is_deleted = 0
        ORDER BY updated_at DESC, conversation_id DESC
        LIMIT 1
        """,
    )
    suspend fun activeConversationId(userId: String): String?

    @Query(
        """
        SELECT * FROM health_chat_conversations
        WHERE user_id = :userId AND conversation_id = :conversationId
        LIMIT 1
        """,
    )
    suspend fun conversation(userId: String, conversationId: String): HealthChatConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(conversation: HealthChatConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: HealthChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(messages: List<HealthChatMessageEntity>)

    @Query(
        """
        UPDATE health_chat_messages
        SET delivery_status = :status
        WHERE message_id = :messageId AND user_id = :userId
        """,
    )
    suspend fun updateDeliveryStatus(userId: String, messageId: String, status: String)

    @Query(
        """
        UPDATE health_chat_conversations
        SET is_active = 0
        WHERE user_id = :userId
        """,
    )
    suspend fun deactivateAll(userId: String)

    @Query(
        """
        UPDATE health_chat_conversations
        SET is_active = 1
        WHERE user_id = :userId AND conversation_id = :conversationId AND is_deleted = 0
        """,
    )
    suspend fun activate(userId: String, conversationId: String)

    @Query(
        """
        UPDATE health_chat_conversations
        SET title = CASE WHEN title = :defaultTitle THEN :title ELSE title END,
            updated_at = :updatedAt
        WHERE user_id = :userId AND conversation_id = :conversationId AND is_deleted = 0
        """,
    )
    suspend fun touch(
        userId: String,
        conversationId: String,
        title: String,
        defaultTitle: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE health_chat_conversations
        SET is_deleted = 1, is_active = 0
        WHERE user_id = :userId AND conversation_id = :conversationId
        """,
    )
    suspend fun markDeleted(userId: String, conversationId: String)

    @Query(
        """
        UPDATE health_chat_conversations
        SET is_deleted = 1, is_active = 0
        WHERE user_id = :userId
        """,
    )
    suspend fun markAllDeleted(userId: String)

    @Query(
        "DELETE FROM health_chat_messages WHERE user_id = :userId AND conversation_id = :conversationId",
    )
    suspend fun deleteMessages(userId: String, conversationId: String)

    @Query("DELETE FROM health_chat_messages WHERE user_id = :userId")
    suspend fun deleteAllMessages(userId: String)

    @Query(
        """
        UPDATE health_chat_conversations
        SET is_active = 1
        WHERE user_id = :userId
          AND conversation_id = (
              SELECT conversation_id FROM health_chat_conversations
              WHERE user_id = :userId AND is_deleted = 0
              ORDER BY updated_at DESC, conversation_id DESC
              LIMIT 1
          )
        """,
    )
    suspend fun activateLatest(userId: String)

    @androidx.room.Transaction
    suspend fun activateConversation(userId: String, conversationId: String) {
        deactivateAll(userId)
        activate(userId, conversationId)
    }

    @androidx.room.Transaction
    suspend fun deleteConversation(userId: String, conversationId: String) {
        markDeleted(userId, conversationId)
        deleteMessages(userId, conversationId)
        deactivateAll(userId)
        activateLatest(userId)
    }

    @androidx.room.Transaction
    suspend fun clearConversations(userId: String) {
        markAllDeleted(userId)
        deleteAllMessages(userId)
    }
}
