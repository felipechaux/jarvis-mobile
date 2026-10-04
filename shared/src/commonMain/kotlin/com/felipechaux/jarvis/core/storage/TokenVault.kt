package com.felipechaux.jarvis.core.storage

/**
 * Where the device token lives: Android Keystore-encrypted on Android, the
 * Keychain on iOS.  Everything else about the server stays in DataStore.
 */
interface TokenVault {
    suspend fun read(): String?

    /** Stores [token], or deletes it when null. */
    suspend fun write(token: String?)
}
