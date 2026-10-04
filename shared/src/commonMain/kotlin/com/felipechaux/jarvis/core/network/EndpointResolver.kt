package com.felipechaux.jarvis.core.network

import com.felipechaux.jarvis.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Picks whichever of the paired server's addresses answers right now, so
 * the app keeps working when the phone leaves the home Wi-Fi (LAN address
 * dead, Tailscale/tunnel address alive) and comes back.
 */
class EndpointResolver(
    private val client: HttpClient,
    private val store: CredentialStore,
) {
    private val lock = Mutex()

    /**
     * Probe the addresses (public first, as the QR ordered them) and make
     * the first live one the active base URL.  Returns it, or null when
     * none answers.
     */
    suspend fun failover(): String? = lock.withLock {
        val creds = store.current() ?: return null
        val live = creds.urls.firstOrNull { reachable(it) } ?: return null
        if (live != creds.baseUrl) store.save(creds.copy(baseUrl = live))
        live
    }

    private suspend fun reachable(baseUrl: String): Boolean = try {
        client.get("$baseUrl/v1/mobile/hello") { timeout { requestTimeoutMillis = PROBE_TIMEOUT_MS } }
            .status.value == 200
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val PROBE_TIMEOUT_MS = 3_000L
    }
}
