package com.felipechaux.jarvis.feature.pairing.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.felipechaux.jarvis.core.model.onError
import com.felipechaux.jarvis.core.model.onSuccess
import com.felipechaux.jarvis.feature.pairing.domain.usecase.PairWithServerUseCase
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingAction
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingEvent
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PairingViewModel(private val pairWithServer: PairWithServerUseCase) : ViewModel() {

    private val _state = MutableStateFlow(PairingState())
    val state: StateFlow<PairingState> = _state.asStateFlow()

    private val _events = Channel<PairingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: PairingAction) {
        when (action) {
            is PairingAction.OnScannedTextChange ->
                _state.update { it.copy(scannedText = action.text, error = null) }
            PairingAction.OnPairClick -> pair()
            PairingAction.OnScanClick -> _state.update { it.copy(isScanning = true, error = null) }
            PairingAction.OnCancelScan -> _state.update { it.copy(isScanning = false) }
            is PairingAction.OnQrScanned -> {
                _state.update { it.copy(isScanning = false, scannedText = action.text) }
                pair()
            }
        }
    }

    private fun pair() {
        val text = _state.value.scannedText
        if (text.isBlank() || _state.value.isPairing) return
        viewModelScope.launch {
            _state.update { it.copy(isPairing = true, error = null) }
            pairWithServer(text)
                .onSuccess {
                    _state.update { it.copy(isPairing = false) }
                    _events.send(PairingEvent.Paired)
                }
                .onError { error -> _state.update { it.copy(isPairing = false, error = error) } }
        }
    }
}
