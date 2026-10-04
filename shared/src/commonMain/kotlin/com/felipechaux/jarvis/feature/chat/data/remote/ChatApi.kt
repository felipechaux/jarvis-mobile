package com.felipechaux.jarvis.feature.chat.data.remote

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.network.SseEvent
import com.felipechaux.jarvis.core.network.SseParser
import com.felipechaux.jarvis.core.network.errorFor
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatStreamException(val error: AppError) : Exception()

class ChatApi(private val client: HttpClient) {

    suspend fun serverInfo(creds: ServerCredentials): ServerInfoDto {
        val response = client.get("${creds.baseUrl}/v1/info") { bearerAuth(creds.token) }
        if (response.status.value != 200) throw ChatStreamException(errorFor(response.status.value, response.bodyAsText()))
        return response.body()
    }

    /** POST /v1/chat/completions with stream=true; emits each SSE event. */
    fun stream(creds: ServerCredentials, request: ChatRequestDto): Flow<SseEvent> = flow {
        client.preparePost("${creds.baseUrl}/v1/chat/completions") {
            bearerAuth(creds.token)
            contentType(ContentType.Application.Json)
            setBody(request)
            // Agent turns with tools can take minutes between bytes.
            timeout {
                requestTimeoutMillis = STREAM_TIMEOUT_MS
                socketTimeoutMillis = STREAM_TIMEOUT_MS
            }
        }.execute { response ->
            if (response.status.value != 200) {
                throw ChatStreamException(errorFor(response.status.value, response.bodyAsText()))
            }
            val channel = response.bodyAsChannel()
            val parser = SseParser()
            while (!parser.done) {
                val line = channel.readUTF8Line() ?: break
                parser.feed(line)?.let { emit(it) }
            }
        }
    }

    private companion object {
        const val STREAM_TIMEOUT_MS = 10 * 60 * 1000L
    }
}
