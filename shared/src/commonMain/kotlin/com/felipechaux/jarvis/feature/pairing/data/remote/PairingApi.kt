package com.felipechaux.jarvis.feature.pairing.data.remote

import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.network.safeApiCall
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PairingApi(private val client: HttpClient) {

    /** Public version handshake; a short timeout so dead URLs are skipped fast. */
    suspend fun hello(baseUrl: String): Resource<HelloDto> = safeApiCall {
        client.get("$baseUrl/v1/mobile/hello") {
            timeout { requestTimeoutMillis = 3_000 }
        }
    }

    suspend fun complete(baseUrl: String, request: PairCompleteRequest): Resource<PairCompleteResponse> =
        safeApiCall {
            client.post("$baseUrl/v1/mobile/pair/complete") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
}
