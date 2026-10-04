package com.felipechaux.jarvis.feature.chat.data.remote

import com.felipechaux.jarvis.core.model.ServerCredentials
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class IndexedConversationDto(
    val id: String,
    val title: String,
    /** Epoch milliseconds, as the server expects from the desktop app. */
    val updatedAt: Long,
    val messages: List<ChatMessageDto>,
)

@Serializable
data class IndexRequestDto(val conversations: List<IndexedConversationDto>)

class ChatHistoryApi(private val client: HttpClient) {
    /** POST /v1/chat-history/index — lets JARVIS recall this conversation later. */
    suspend fun index(creds: ServerCredentials, conversation: IndexedConversationDto) {
        client.post("${creds.baseUrl}/v1/chat-history/index") {
            bearerAuth(creds.token)
            contentType(ContentType.Application.Json)
            setBody(IndexRequestDto(listOf(conversation)))
        }
    }
}
