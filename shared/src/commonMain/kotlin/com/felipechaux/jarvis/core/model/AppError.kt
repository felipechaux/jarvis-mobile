package com.felipechaux.jarvis.core.model

/** Typed failures shared by every feature; mapped to UI text in presentation. */
sealed interface AppError {
    /** The server could not be reached (offline, wrong address, Mac asleep). */
    data object Unreachable : AppError

    /** The device token was rejected: the phone was unpaired or never paired. */
    data object Unauthorized : AppError

    /** The app and server speak different protocol versions. */
    data class IncompatibleProtocol(val detail: String) : AppError

    /** The pairing QR is malformed, expired or already used. */
    data object InvalidPairingCode : AppError

    /** The server answered with an error the app does not handle specially. */
    data class Server(val status: Int, val detail: String) : AppError

    data class Unknown(val cause: Throwable? = null) : AppError
}
