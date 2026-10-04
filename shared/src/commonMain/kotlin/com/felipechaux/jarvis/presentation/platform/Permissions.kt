package com.felipechaux.jarvis.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class AppPermission { MICROPHONE, CAMERA, NOTIFICATIONS }

/**
 * Returns a function that asks for [permission] (or answers at once when it
 * is already granted) and reports the outcome to [onResult].  On iOS,
 * granting NOTIFICATIONS also registers for remote notifications.
 */
@Composable
expect fun rememberPermissionRequest(permission: AppPermission, onResult: (granted: Boolean) -> Unit): () -> Unit

/** Live camera preview that reports the first QR code it reads. */
@Composable
expect fun QrScannerView(onScanned: (String) -> Unit, modifier: Modifier = Modifier)
