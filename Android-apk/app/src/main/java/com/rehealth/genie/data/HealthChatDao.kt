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
        SELECT * FROM health_chat_messages
        WHERE user_id = :userId
          AND conversation_id = (
              SELECT conversation_id FROM health_chat_messages
              WHERE user_id = :userId
              ORDER BY created_at DESC, message_id DESC
              LIMIT 1
          )
        ORDER BY created_at ASC, message_id ASC
        """,
    )
    fun observeLatestConversation(userId: String): Flow<List<HealthChatMessageEntity>>

    @Query(
        """
        SELECT conversation_id FROM health_chat_messages
        WHERE user_id = :userId
        ORDER BY created_at DESC, message_id DESC
        LIMIT 1
        """,
    )
    suspend fun latestConversationId(userId: String): String?

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
}
