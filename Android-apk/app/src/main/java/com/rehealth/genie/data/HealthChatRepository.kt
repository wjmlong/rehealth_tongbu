package com.rehealth.genie.data

import com.rehealth.genie.network.ApiResult
import com.rehealth.genie.network.AuthenticatedApiClient
import com.rehealth.genie.network.dto.HealthAgentConversation
import com.rehealth.genie.network.dto.HealthAgentMessageRequest
import com.rehealth.genie.network.dto.HealthAgentResponse
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class HealthChatRepository(
    private val dao: HealthChatDao,
    private val apiClient: AuthenticatedApiClient,
    private val userIdProvider: () -> String?,
) {
    fun observeLatestConversation(userId: String): Flow<List<HealthChatMessageEntity>> =
        dao.observeLatestConversation(userId)

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
        val conversationId = dao.latestConversationId(userId) ?: UUID.randomUUID().toString()
        val requestId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
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
            ),
        )
        when (result) {
            is ApiResult.Success -> {
                dao.updateDeliveryStatus(userId, messageId, DELIVERY_SYNCED)
                val response = result.data
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

    private suspend fun cacheConversation(userId: String, conversation: HealthAgentConversation) {
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

    private fun currentUserId(): String? = userIdProvider()?.takeIf(String::isNotBlank)

    companion object {
        const val ROLE_USER = "USER"
        const val ROLE_ASSISTANT = "ASSISTANT"
        const val DELIVERY_PENDING = "PENDING"
        const val DELIVERY_SYNCED = "SYNCED"
        const val DELIVERY_FAILED = "FAILED"
    }
}
