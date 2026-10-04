package com.felipechaux.jarvis.feature.voice.data.remote

import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.network.errorFor
import com.felipechaux.jarvis.core.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionDto(val text: String = "", val language: String? = null)

@Serializable
data class SynthesizeRequestDto(val text: String)

class SpeechApi(private val client: HttpClient) {

    suspend fun transcribe(creds: ServerCredentials, wav: ByteArray, language: String): Resource<TranscriptionDto> =
        safeApiCall {
            client.submitFormWithBinaryData(
                url = "${creds.baseUrl}/v1/speech/transcribe",
                formData = formData {
                    append(
                        "file",
                        wav,
                        Headers.build {
                            append(HttpHeaders.ContentType, "audio/wav")
                            append(HttpHeaders.ContentDisposition, "filename=\"speech.wav\"")
                        },
                    )
                    if (language.isNotBlank()) append("language", language)
                },
            ) {
                bearerAuth(creds.token)
                timeout { requestTimeoutMillis = LONG_TIMEOUT_MS }
            }
        }

    suspend fun synthesize(creds: ServerCredentials, text: String): Resource<ByteArray> = try {
        val response = client.post("${creds.baseUrl}/v1/speech/synthesize") {
            bearerAuth(creds.token)
            contentType(ContentType.Application.Json)
            setBody(SynthesizeRequestDto(text))
            timeout { requestTimeoutMillis = LONG_TIMEOUT_MS }
        }
        if (response.status.value == 200) {
            Resource.Success(response.readRawBytes())
        } else {
            Resource.Error(errorFor(response.status.value, response.bodyAsText()))
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(com.felipechaux.jarvis.core.model.AppError.Unreachable)
    }

    private companion object {
        const val LONG_TIMEOUT_MS = 60_000L
    }
}
