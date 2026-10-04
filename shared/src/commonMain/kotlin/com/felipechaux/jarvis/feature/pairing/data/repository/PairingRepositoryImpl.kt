package com.felipechaux.jarvis.feature.pairing.data.repository

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.network.PROTOCOL_VERSION
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.pairing.data.mapper.PairingPayloadParser
import com.felipechaux.jarvis.feature.pairing.data.remote.PairCompleteRequest
import com.felipechaux.jarvis.feature.pairing.data.remote.PairingApi
import com.felipechaux.jarvis.feature.pairing.domain.model.PairingPayload
import com.felipechaux.jarvis.feature.pairing.domain.repository.PairingRepository

class PairingRepositoryImpl(
    private val api: PairingApi,
    private val store: CredentialStore,
) : PairingRepository {

    override fun parsePayload(raw: String, nowEpochS: Long): Resource<PairingPayload> =
        PairingPayloadParser.parse(raw, nowEpochS)

    override suspend fun pair(
        payload: PairingPayload,
        deviceName: String,
        platform: String,
    ): Resource<ServerCredentials> {
        // URLs come ordered public (Tailscale/tunnel) first, then LAN; the
        // first one that answers the handshake is the one we keep.
        var lastError: AppError = AppError.Unreachable
        for (url in payload.urls.map { it.trimEnd('/') }) {
            when (val hello = api.hello(url)) {
                is Resource.Error -> {
                    lastError = hello.error
                    continue
                }
                is Resource.Success -> if (PROTOCOL_VERSION < hello.data.minProtocol) {
                    return Resource.Error(AppError.IncompatibleProtocol("update the app (min ${hello.data.minProtocol})"))
                }
            }
            val request = PairCompleteRequest(payload.code, deviceName, platform, PROTOCOL_VERSION)
            return when (val result = api.complete(url, request)) {
                is Resource.Error -> Resource.Error(
                    if (result.error == AppError.Unauthorized) AppError.InvalidPairingCode else result.error,
                )
                is Resource.Success -> {
                    val creds = ServerCredentials(
                        baseUrl = url,
                        token = result.data.token,
                        serverName = result.data.name.ifBlank { payload.serverName },
                        deviceId = result.data.device.id,
                        // Keep every address: at home the LAN answers, away
                        // from it the public one (see EndpointResolver).
                        urls = payload.urls.map { it.trimEnd('/') },
                    )
                    store.save(creds)
                    Resource.Success(creds)
                }
            }
        }
        return Resource.Error(lastError)
    }

    override suspend fun unpair() = store.clear()
}
