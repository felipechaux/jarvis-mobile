package com.felipechaux.jarvis.feature.pairing.domain.repository

import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.feature.pairing.domain.model.PairingPayload

interface PairingRepository {
    /** Decode scanned QR text; rejects foreign QRs, expired codes, newer protocols. */
    fun parsePayload(raw: String, nowEpochS: Long): Resource<PairingPayload>

    /** Pair through the first reachable URL in [payload] and store the credentials. */
    suspend fun pair(payload: PairingPayload, deviceName: String, platform: String): Resource<ServerCredentials>

    suspend fun unpair()
}
