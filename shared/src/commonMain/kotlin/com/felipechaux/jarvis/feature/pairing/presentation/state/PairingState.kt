package com.felipechaux.jarvis.feature.pairing.presentation.state

import com.felipechaux.jarvis.core.model.AppError

data class PairingState(
    val scannedText: String = "",
    val isScanning: Boolean = false,
    val isPairing: Boolean = false,
    val error: AppError? = null,
)

sealed interface PairingAction {
    data class OnScannedTextChange(val text: String) : PairingAction
    data object OnPairClick : PairingAction

    /** Camera permission granted: open the scanner. */
    data object OnScanClick : PairingAction
    data object OnCancelScan : PairingAction
    data class OnQrScanned(val text: String) : PairingAction
}

sealed interface PairingEvent {
    data object Paired : PairingEvent
}
