package com.felipechaux.jarvis.feature.pairing.domain.model

/** What the Mac's pairing QR carries (see `openjarvis.mobile.pairing.build_payload`). */
data class PairingPayload(
    val protocol: Int,
    val serverName: String,
    val urls: List<String>,
    val code: String,
    val expiresAtEpochS: Long,
)
