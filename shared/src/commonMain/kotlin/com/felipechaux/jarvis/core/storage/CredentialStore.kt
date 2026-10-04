package com.felipechaux.jarvis.core.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.felipechaux.jarvis.core.model.ServerCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface CredentialStore {
    val credentials: Flow<ServerCredentials?>
    suspend fun current(): ServerCredentials? = credentials.first()
    suspend fun save(credentials: ServerCredentials)
    suspend fun clear()
}

/**
 * Server address and names in DataStore; the device token in [vault]
 * (Keystore / Keychain).  The token is written before the DataStore edit,
 * so the emission that edit triggers already sees it.
 */
class DataStoreCredentialStore(
    private val dataStore: DataStore<Preferences>,
    private val vault: TokenVault,
) : CredentialStore {

    override val credentials: Flow<ServerCredentials?> = dataStore.data.map { prefs ->
        val url = prefs[BASE_URL] ?: return@map null
        val token = vault.read() ?: return@map null
        ServerCredentials(
            baseUrl = url,
            token = token,
            serverName = prefs[SERVER_NAME].orEmpty(),
            deviceId = prefs[DEVICE_ID].orEmpty(),
            urls = prefs[URLS]?.split(URL_SEPARATOR)?.filter { it.isNotBlank() }?.ifEmpty { null } ?: listOf(url),
        )
    }

    override suspend fun save(credentials: ServerCredentials) {
        vault.write(credentials.token)
        dataStore.edit { prefs ->
            prefs[BASE_URL] = credentials.baseUrl
            prefs[SERVER_NAME] = credentials.serverName
            prefs[DEVICE_ID] = credentials.deviceId
            prefs[URLS] = credentials.urls.joinToString(URL_SEPARATOR)
        }
    }

    override suspend fun clear() {
        vault.write(null)
        dataStore.edit { prefs ->
            prefs.remove(BASE_URL)
            prefs.remove(SERVER_NAME)
            prefs.remove(DEVICE_ID)
            prefs.remove(URLS)
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val SERVER_NAME = stringPreferencesKey("server_name")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val URLS = stringPreferencesKey("server_urls")
    }
}

private const val URL_SEPARATOR = "\n"

internal const val DATASTORE_FILE = "jarvis.preferences_pb"
