package com.felipechaux.jarvis.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val JarvisJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}

/** One client for REST, SSE and the RPC WebSocket; the engine comes from the platform. */
fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(JarvisJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 5_000
        requestTimeoutMillis = 30_000
    }
    install(WebSockets) {
        pingIntervalMillis = 20_000
    }
    expectSuccess = false
}
