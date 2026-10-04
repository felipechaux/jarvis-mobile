package com.felipechaux.jarvis.feature.pairing.domain.usecase

import com.felipechaux.jarvis.core.model.Resource
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.platform.PlatformInfo
import com.felipechaux.jarvis.feature.pairing.domain.repository.PairingRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PairWithServerUseCase(
    private val repository: PairingRepository,
    private val platform: PlatformInfo,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(scannedText: String): Resource<ServerCredentials> {
        val now = Clock.System.now().epochSeconds
        return when (val parsed = repository.parsePayload(scannedText, now)) {
            is Resource.Error -> parsed
            is Resource.Success -> repository.pair(parsed.data, platform.deviceName, platform.platform)
        }
    }
}
