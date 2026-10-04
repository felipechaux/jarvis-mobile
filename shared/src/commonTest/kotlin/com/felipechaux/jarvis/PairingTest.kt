package com.felipechaux.jarvis

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.network.JarvisJson
import com.felipechaux.jarvis.core.network.wsUrl
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.pairing.data.mapper.PairingPayloadParser
import com.felipechaux.jarvis.feature.pairing.data.remote.PairingApi
import com.felipechaux.jarvis.feature.pairing.data.repository.PairingRepositoryImpl
import com.felipechaux.jarvis.feature.pairing.domain.model.PairingPayload
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

// Real dispatcher for HTTP calls: runTest's virtual time would fire the
// client's request timeout instantly.
class PairingTest {
    private val validJson =
        """{"type":"openjarvis-pair","v":1,"name":"mac","urls":["http://10.0.0.2:8000"],"code":"abc","expires_at":2000}"""

    @Test
    fun parsesValidPayload() {
        val result = PairingPayloadParser.parse(validJson, nowEpochS = 1000)
        assertIs<Resource.Success<PairingPayload>>(result)
        assertEquals("abc", result.data.code)
    }

    @Test
    fun rejectsExpiredForeignAndNewer() {
        assertIs<Resource.Error>(PairingPayloadParser.parse(validJson, nowEpochS = 3000))
        assertIs<Resource.Error>(PairingPayloadParser.parse("""{"type":"wifi"}""", 0))
        assertIs<Resource.Error>(PairingPayloadParser.parse("hola", 0))
        val newer = PairingPayloadParser.parse(validJson.replace("\"v\":1", "\"v\":99"), 1000)
        assertIs<AppError.IncompatibleProtocol>((newer as Resource.Error).error)
    }

    @Test
    fun wsUrlFromBase() {
        assertEquals("ws://10.0.0.2:8000/v1/mobile/ws", wsUrl("http://10.0.0.2:8000/"))
        assertEquals("wss://jarvis.ts.net/v1/mobile/ws", wsUrl("https://jarvis.ts.net"))
    }

    @Test
    fun pairsThroughFirstReachableUrl() = runTest {
        val store = FakeCredentialStore()
        val engine = MockEngine { request ->
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                request.url.host == "dead.example" -> throw IOException("unreachable")
                request.url.encodedPath.endsWith("/hello") ->
                    respond(REAL_HELLO, HttpStatusCode.OK, json)
                request.url.encodedPath.endsWith("/pair/complete") ->
                    respond(REAL_PAIR_COMPLETE, HttpStatusCode.OK, json)
                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(JarvisJson) }
            install(HttpTimeout)
        }
        val repository = PairingRepositoryImpl(PairingApi(client), store)
        val payload = PairingPayload(1, "mac", listOf("http://dead.example", "http://10.0.0.2:8000"), "abc", 0)

        val result = withContext(Dispatchers.Default) { repository.pair(payload, "Pixel", "android") }

        assertIs<Resource.Success<ServerCredentials>>(result)
        assertEquals("http://10.0.0.2:8000", result.data.baseUrl)
        assertEquals("oj_dev_x", store.current()?.token)
    }

    @Test
    fun usedCodeMapsToInvalidPairingCode() = runTest {
        val engine = MockEngine { request ->
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            if (request.url.encodedPath.endsWith("/hello")) {
                respond(REAL_HELLO, HttpStatusCode.OK, json)
            } else {
                respond("""{"detail":"Invalid or expired pairing code"}""", HttpStatusCode.Unauthorized, json)
            }
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(JarvisJson) }
            install(HttpTimeout)
        }
        val repository = PairingRepositoryImpl(PairingApi(client), FakeCredentialStore())
        val payload = PairingPayload(1, "mac", listOf("http://10.0.0.2:8000"), "used", 0)
        val result = withContext(Dispatchers.Default) { repository.pair(payload, "Pixel", "android") }
        assertEquals(AppError.InvalidPairingCode, (result as Resource.Error).error)
    }
}

/** Verbatim from a live server, so DTO drift breaks the tests. */
private const val REAL_HELLO =
    """{"protocol":1,"min_protocol":1,"name":"felipes-MacBook-Pro","push":[],"time":1791071524.929613}"""

private const val REAL_PAIR_COMPLETE =
    """{"token":"oj_dev_x","device":{"id":"d1","name":"Pixel","platform":"android","created_at":1791071500.1,""" +
        """"last_seen":0.0,"app_protocol":1,"push":{}},"protocol":1,"min_protocol":1,""" +
        """"name":"felipes-MacBook-Pro","push":[],"time":1791071524.9}"""

class FakeCredentialStore : CredentialStore {
    override val credentials = MutableStateFlow<ServerCredentials?>(null)
    override suspend fun save(credentials: ServerCredentials) {
        this.credentials.value = credentials
    }
    override suspend fun clear() {
        credentials.value = null
    }
}
