package com.felipechaux.jarvis.feature.pairing.data.mapper

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.network.JarvisJson
import com.felipechaux.jarvis.core.network.PAIRING_PAYLOAD_TYPE
import com.felipechaux.jarvis.core.network.PROTOCOL_VERSION
import com.felipechaux.jarvis.feature.pairing.data.remote.PairingPayloadDto
import com.felipechaux.jarvis.feature.pairing.domain.model.PairingPayload

/** Parses the scanned QR text; rejects foreign QRs, expired codes and newer protocols. */
object PairingPayloadParser {

    fun parse(raw: String, nowEpochS: Long): Resource<PairingPayload> {
        val dto = runCatching { JarvisJson.decodeFromString<PairingPayloadDto>(raw.trim()) }.getOrNull()
            ?: return Resource.Error(AppError.InvalidPairingCode)
        if (dto.type != PAIRING_PAYLOAD_TYPE || dto.code.isBlank() || dto.urls.isEmpty()) {
            return Resource.Error(AppError.InvalidPairingCode)
        }
        if (dto.v > PROTOCOL_VERSION) {
            return Resource.Error(AppError.IncompatibleProtocol("server protocol ${dto.v}, app $PROTOCOL_VERSION"))
        }
        if (dto.expiresAt in 1..nowEpochS) return Resource.Error(AppError.InvalidPairingCode)
        return Resource.Success(
            PairingPayload(
                protocol = dto.v,
                serverName = dto.name,
                urls = dto.urls,
                code = dto.code,
                expiresAtEpochS = dto.expiresAt,
            ),
        )
    }
}
