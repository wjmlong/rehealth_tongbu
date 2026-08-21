package com.rehealth.genie.data

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.HealthAgentConversation
import com.rehealth.genie.network.dto.HealthAgentMessageRequest
import com.rehealth.genie.network.dto.HealthAgentResponse
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class HealthChatRepository(
    private val dao: HealthChatDao,
    private val apiClient: AuthenticatedApiClient,
    private val userIdProvider: () -> String?,
) {
    fun observeActiveConversation(userId: String): Flow<List<HealthChatMessageEntity>> =
        dao.observeActiveConversation(userId)

    fun observeConversations(userId: String): Flow<List<HealthChatConversationEntity>> =
        dao.observeConversations(userId)

    fun observeActiveConversationId(userId: String): Flow<String?> =
        dao.observeActiveConversationId(userId)

    suspend fun refreshLatest(): ApiResult<HealthAgentConversation?> {
        val userId = currentUserId() ?: return ApiResult.Unauthorized("请重新登录后查看健康问答")
        val result = apiClient.getLatestHealthAgentConversation()
        if (result is ApiResult.Success) {
            result.data?.let { cacheConversation(userId, it) }
        }
        return result
    }

    suspend fun send(text: String): ApiResult<HealthAgentResponse> {
        val userId = currentUserId() ?: return ApiResult.Unauthorized("请重新登录后使用健康问答")
        val content = text.trim()
        if (content.isEmpty()) return ApiResult.InvalidRequest("请输入健康问题")
        val conversationId = dao.activeConversationId(userId) ?: createConversation(userId)
        val requestId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        dao.touch(userId, conversationId, titleFor(content), DEFAULT_TITLE, createdAt)
        dao.upsert(
            HealthChatMessageEntity(
                messageId = messageId,
                userId = userId,
                conversationId = conversationId,
                requestId = requestId,
                role = ROLE_USER,
                content = content,
                deliveryStatus = DELIVERY_PENDING,
                createdAt = createdAt,
            ),
        )
        val result = apiClient.sendHealthAgentMessage(
            HealthAgentMessageRequest(
                requestId = requestId,
                conversationId = conversationId,
                clientMessageId = messageId,
                message = content,
                timeZone = ZoneId.systemDefault().id,
            ),
        )
        when (result) {
            is ApiResult.Success -> {
                dao.updateDeliveryStatus(userId, messageId, DELIVERY_SYNCED)
                val response = result.data
                dao.touch(
                    userId,
                    conversationId,
                    titleFor(content),
                    DEFAULT_TITLE,
                    response.createdAt ?: System.currentTimeMillis(),
                )
                dao.upsert(
                    HealthChatMessageEntity(
                        messageId = response.messageId ?: UUID.randomUUID().toString(),
                        userId = userId,
                        conversationId = response.conversationId ?: conversationId,
                        requestId = response.request_id ?: response.requestId ?: requestId,
                        role = ROLE_ASSISTANT,
                        content = response.answer.orEmpty(),
                        deliveryStatus = DELIVERY_SYNCED,
                        provider = response.provider,
                        modelVersion = response.model_version,
                        createdAt = response.createdAt ?: System.currentTimeMillis(),
                    ),
                )
            }
            else -> dao.updateDeliveryStatus(userId, messageId, DELIVERY_FAILED)
        }
        return result
    }

    suspend fun createConversation(): String? {
        val userId = currentUserId() ?: return null
        return createConversation(userId)
    }

    suspend fun selectConversation(conversationId: String) {
        val userId = currentUserId() ?: return
        dao.activateConversation(userId, conversationId)
    }

    suspend fun deleteLocalConversation(conversationId: String) {
        val userId = currentUserId() ?: return
        dao.deleteConversation(userId, conversationId)
    }

    suspend fun clearLocalConversations() {
        val userId = currentUserId() ?: return
        dao.clearConversations(userId)
    }

    private suspend fun createConversation(userId: String): String {
        val conversationId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.deactivateAll(userId)
        dao.upsert(
            HealthChatConversationEntity(
                userId = userId,
                conversationId = conversationId,
                title = DEFAULT_TITLE,
                createdAt = now,
                updatedAt = now,
                isActive = true,
                isDeleted = false,
            ),
        )
        return conversationId
    }

    private suspend fun cacheConversation(userId: String, conversation: HealthAgentConversation) {
        val existing = dao.conversation(userId, conversation.conversationId)
        if (existing?.isDeleted == true) return
        val now = System.currentTimeMillis()
        val createdAt = conversation.createdAt
            ?: conversation.messages.minOfOrNull { it.createdAt }
            ?: now
        val updatedAt = conversation.updatedAt
            ?: conversation.messages.maxOfOrNull { it.createdAt }
            ?: createdAt
        val shouldActivate = existing?.isActive
            ?: (dao.activeConversationId(userId) == null)
        dao.upsert(
            HealthChatConversationEntity(
                userId = userId,
                conversationId = conversation.conversationId,
                title = conversation.title?.takeIf(String::isNotBlank)?.take(MAX_TITLE_LENGTH)
                    ?: conversation.messages.firstOrNull { it.role.equals(ROLE_USER, ignoreCase = true) }
                        ?.content
                        ?.let(::titleFor)
                    ?: DEFAULT_TITLE,
                createdAt = existing?.createdAt ?: createdAt,
                updatedAt = maxOf(existing?.updatedAt ?: 0L, updatedAt),
                isActive = shouldActivate,
                isDeleted = false,
            ),
        )
        val messages = conversation.messages.map { message ->
            HealthChatMessageEntity(
                messageId = message.messageId,
                userId = userId,
                conversationId = conversation.conversationId,
                requestId = message.requestId,
                role = message.role.uppercase(),
                content = message.content,
                deliveryStatus = DELIVERY_SYNCED,
                provider = message.provider,
                modelVersion = message.modelVersion,
                createdAt = message.createdAt,
            )
        }
        if (messages.isNotEmpty()) dao.upsert(messages)
    }

    private fun titleFor(content: String): String =
        content.lineSequence().firstOrNull(String::isNotBlank)
            ?.trim()
            ?.take(MAX_TITLE_LENGTH)
            ?.ifBlank { DEFAULT_TITLE }
            ?: DEFAULT_TITLE

    private fun currentUserId(): String? = userIdProvider()?.takeIf(String::isNotBlank)

    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ASSISTANT = "ASSISTANT"
        const val DELIVERY_PENDING = "PENDING"
        const val DELIVERY_SYNCED = "SYNCED"
        const val DELIVERY_FAILED = "FAILED"
        const val DEFAULT_TITLE = "新对话"
        const val MAX_TITLE_LENGTH = 32
    }
}
