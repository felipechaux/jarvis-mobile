package com.felipechaux.jarvis.core.model

/**
 * Where the paired JARVIS server lives and the device token that opens it.
 *
 * [urls] are every address from the pairing QR (public — Tailscale or
 * tunnel — first, then LAN); [baseUrl] is the one that answered last.
 */
data class ServerCredentials(
    val baseUrl: String,
    val token: String,
    val serverName: String,
    val deviceId: String,
    val urls: List<String> = listOf(baseUrl),
)
