package com.felipechaux.jarvis.core.push

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.felipechaux.jarvis.core.storage.CredentialStore
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable

@Serializable
private data class PushRegisterDto(val provider: String, val token: String)

/**
 * Hands the OS push token (FCM on Android, APNs on iOS) to the Mac.
 *
 * The token can arrive before the phone is paired, so it is kept and sent
 * as soon as credentials exist — and again whenever the token or the pairing
 * changes.
 */
class PushRegistrar(
    private val client: HttpClient,
    private val credentials: CredentialStore,
    private val prefs: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val lock = Mutex()

    /** Call from the platform push service when the OS issues a token. */
    fun onNewToken(provider: String, token: String) {
        scope.launch {
            prefs.edit {
                it[PROVIDER] = provider
                it[TOKEN] = token
            }
            registerIfNeeded()
        }
    }

    /** Re-register whenever the pairing changes (new server or device id). */
    fun start() {
        scope.launch {
            credentials.credentials
                .map { it?.deviceId }
                .distinctUntilChanged()
                .collect { if (it != null) registerIfNeeded() }
        }
    }

    private suspend fun registerIfNeeded() = lock.withLock {
        val creds = credentials.current() ?: return@withLock
        val stored = prefs.data.first()
        val provider = stored[PROVIDER] ?: return@withLock
        val token = stored[TOKEN] ?: return@withLock
        val marker = "${creds.deviceId}:$provider:$token"
        if (stored[REGISTERED] == marker) return@withLock
        try {
            val response = client.post("${creds.baseUrl}/v1/mobile/push/register") {
                bearerAuth(creds.token)
                contentType(ContentType.Application.Json)
                setBody(PushRegisterDto(provider, token))
            }
            if (response.status.value == 200) prefs.edit { it[REGISTERED] = marker }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: retried on the next token or pairing change, or app start.
        }
    }

    private companion object {
        val PROVIDER = stringPreferencesKey("push_provider")
        val TOKEN = stringPreferencesKey("push_token")
        val REGISTERED = stringPreferencesKey("push_registered")
    }
}
