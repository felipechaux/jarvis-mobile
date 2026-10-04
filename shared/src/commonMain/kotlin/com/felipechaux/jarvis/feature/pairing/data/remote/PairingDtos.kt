package com.felipechaux.jarvis.feature.pairing.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PairingPayloadDto(
    val type: String = "",
    val v: Int = 0,
    val name: String = "",
    val urls: List<String> = emptyList(),
    val code: String = "",
    @SerialName("expires_at") val expiresAt: Long = 0,
)

@Serializable
data class HelloDto(
    val protocol: Int,
    @SerialName("min_protocol") val minProtocol: Int,
    val name: String = "",
    /** Push providers the Mac can send through ("fcm", "apns"). */
    val push: List<String> = emptyList(),
)

@Serializable
data class PairCompleteRequest(
    val code: String,
    val name: String,
    val platform: String,
    val protocol: Int,
)

@Serializable
data class DeviceDto(val id: String, val name: String = "", val platform: String = "")

@Serializable
data class PairCompleteResponse(
    val token: String,
    val device: DeviceDto,
    val name: String = "",
    val protocol: Int = 0,
)
