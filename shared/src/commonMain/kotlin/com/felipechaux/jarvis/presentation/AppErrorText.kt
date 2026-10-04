package com.felipechaux.jarvis.presentation

import com.felipechaux.jarvis.core.model.AppError
import com.felipechaux.jarvis.resources.Res
import com.felipechaux.jarvis.resources.error_incompatible
import com.felipechaux.jarvis.resources.error_invalid_code
import com.felipechaux.jarvis.resources.error_server
import com.felipechaux.jarvis.resources.error_unauthorized
import com.felipechaux.jarvis.resources.error_unknown
import com.felipechaux.jarvis.resources.error_unreachable
import org.jetbrains.compose.resources.StringResource

fun AppError.toStringResource(): StringResource = when (this) {
    AppError.Unreachable -> Res.string.error_unreachable
    AppError.Unauthorized -> Res.string.error_unauthorized
    is AppError.IncompatibleProtocol -> Res.string.error_incompatible
    AppError.InvalidPairingCode -> Res.string.error_invalid_code
    is AppError.Server -> Res.string.error_server
    is AppError.Unknown -> Res.string.error_unknown
}
