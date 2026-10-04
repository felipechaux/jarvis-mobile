package com.felipechaux.jarvis.feature.pairing.presentation.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingAction
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingEvent
import com.felipechaux.jarvis.feature.pairing.presentation.state.PairingState
import com.felipechaux.jarvis.feature.pairing.presentation.viewmodel.PairingViewModel
import com.felipechaux.jarvis.presentation.components.ArcReactor
import com.felipechaux.jarvis.presentation.components.JarvisIcon
import com.felipechaux.jarvis.presentation.components.JarvisIconKind
import com.felipechaux.jarvis.presentation.platform.AppPermission
import com.felipechaux.jarvis.presentation.platform.QrScannerView
import com.felipechaux.jarvis.presentation.platform.rememberPermissionRequest
import com.felipechaux.jarvis.presentation.theme.JarvisColors
import com.felipechaux.jarvis.presentation.toStringResource
import com.felipechaux.jarvis.resources.Res
import com.felipechaux.jarvis.resources.chat_stop
import com.felipechaux.jarvis.resources.error_camera_denied
import com.felipechaux.jarvis.resources.pairing_action
import com.felipechaux.jarvis.resources.pairing_input_label
import com.felipechaux.jarvis.resources.pairing_mode_paste
import com.felipechaux.jarvis.resources.pairing_mode_scan
import com.felipechaux.jarvis.resources.pairing_scan
import com.felipechaux.jarvis.resources.pairing_scan_hint
import com.felipechaux.jarvis.resources.pairing_step_1
import com.felipechaux.jarvis.resources.pairing_step_2
import com.felipechaux.jarvis.resources.pairing_subtitle
import com.felipechaux.jarvis.resources.pairing_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PairingRoot(onPaired: () -> Unit, viewModel: PairingViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var cameraDenied by remember { mutableStateOf(false) }
    val requestCamera = rememberPermissionRequest(AppPermission.CAMERA) { granted ->
        cameraDenied = !granted
        if (granted) viewModel.onAction(PairingAction.OnScanClick)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                PairingEvent.Paired -> onPaired()
            }
        }
    }
    PairingScreen(
        state = state,
        cameraDenied = cameraDenied,
        onAction = { if (it == PairingAction.OnScanClick) requestCamera() else viewModel.onAction(it) },
    )
}

/** High-tech HUD Pairing screen matching OpenJarvis design system. */
@Composable
fun PairingScreen(state: PairingState, cameraDenied: Boolean, onAction: (PairingAction) -> Unit) {
    var modeScan by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // Hero Arc Reactor Core
        ArcReactor(size = 72.dp)

        // Header Title & Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.pairing_title),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 2.sp,
                color = JarvisColors.Accent,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.pairing_subtitle),
                fontSize = 12.sp,
                color = JarvisColors.TextSecondary,
                lineHeight = 18.sp,
            )
        }

        // Instructions Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(JarvisColors.BackgroundSecondary)
                .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JarvisIcon(JarvisIconKind.Terminal, size = 14.dp, tint = JarvisColors.Accent)
                Text(
                    text = "PASOS PARA VINCULAR",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = JarvisColors.Accent,
                )
            }
            Text(
                text = stringResource(Res.string.pairing_step_1),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = JarvisColors.TextPrimary,
            )
            Text(
                text = stringResource(Res.string.pairing_step_2),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = JarvisColors.TextPrimary,
            )
        }

        // Mode Selector: Scan QR vs Manual JSON Code
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(JarvisColors.BackgroundSecondary)
                .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(10.dp))
                .padding(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (modeScan) JarvisColors.Accent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (modeScan) JarvisColors.Accent else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { modeScan = true }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    JarvisIcon(
                        JarvisIconKind.QrCode,
                        size = 14.dp,
                        tint = if (modeScan) JarvisColors.Accent else JarvisColors.TextTertiary,
                    )
                    Text(
                        text = stringResource(Res.string.pairing_mode_scan),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (modeScan) JarvisColors.Accent else JarvisColors.TextTertiary,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!modeScan) JarvisColors.Accent.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (!modeScan) JarvisColors.Accent else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { modeScan = false }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    JarvisIcon(
                        JarvisIconKind.Terminal,
                        size = 14.dp,
                        tint = if (!modeScan) JarvisColors.Accent else JarvisColors.TextTertiary,
                    )
                    Text(
                        text = stringResource(Res.string.pairing_mode_paste),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!modeScan) JarvisColors.Accent else JarvisColors.TextTertiary,
                    )
                }
            }
        }

        if (modeScan) {
            // Scanner Viewport
            if (state.isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, JarvisColors.Accent, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    QrScannerView(
                        onScanned = { onAction(PairingAction.OnQrScanned(it)) },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // High-tech laser sweep overlay
                    HolographicScannerOverlay()
                }

                // Stop scanning button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(JarvisColors.BackgroundTertiary)
                        .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onAction(PairingAction.OnCancelScan) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.chat_stop),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = JarvisColors.TextSecondary,
                    )
                }
            } else {
                // Button to launch scanner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisColors.Accent.copy(alpha = 0.15f))
                        .border(1.dp, JarvisColors.Accent, RoundedCornerShape(12.dp))
                        .clickable(enabled = !state.isPairing) { onAction(PairingAction.OnScanClick) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isPairing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = JarvisColors.Accent)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            JarvisIcon(JarvisIconKind.QrCode, size = 18.dp, tint = JarvisColors.Accent)
                            Text(
                                text = stringResource(Res.string.pairing_scan),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = JarvisColors.Accent,
                            )
                        }
                    }
                }
            }
        } else {
            // Manual Code Input
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = state.scannedText,
                    onValueChange = { onAction(PairingAction.OnScannedTextChange(it)) },
                    label = {
                        Text(
                            stringResource(Res.string.pairing_input_label),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = JarvisColors.CodeBackground,
                        unfocusedContainerColor = JarvisColors.CodeBackground,
                        focusedBorderColor = JarvisColors.Accent,
                        unfocusedBorderColor = JarvisColors.BorderSubtle,
                        focusedTextColor = JarvisColors.TextPrimary,
                        unfocusedTextColor = JarvisColors.TextPrimary,
                    ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (state.scannedText.isNotBlank()) JarvisColors.Accent.copy(alpha = 0.2f)
                            else JarvisColors.BackgroundTertiary
                        )
                        .border(
                            1.dp,
                            if (state.scannedText.isNotBlank()) JarvisColors.Accent
                            else JarvisColors.BorderSubtle,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable(
                            enabled = !state.isPairing && state.scannedText.isNotBlank(),
                            onClick = { onAction(PairingAction.OnPairClick) },
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.isPairing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = JarvisColors.Accent)
                    } else {
                        Text(
                            text = stringResource(Res.string.pairing_action),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.scannedText.isNotBlank()) JarvisColors.Accent else JarvisColors.TextTertiary,
                        )
                    }
                }
            }
        }

        // Error Feedback
        if (cameraDenied) {
            Text(
                stringResource(Res.string.error_camera_denied),
                color = JarvisColors.Error,
                fontSize = 12.sp,
            )
        }
        state.error?.let {
            Text(
                stringResource(it.toStringResource()),
                color = JarvisColors.Error,
                fontSize = 12.sp,
            )
        }
    }
}

/** Animated laser scan line overlay with corner crosshairs */
@Composable
private fun HolographicScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scannerLaser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "laserY",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Corner target brackets
        val bracketLen = w * 0.12f
        val stroke = 3.dp.toPx()
        val c = JarvisColors.Accent

        // Top Left
        drawLine(c, Offset(0f, 0f), Offset(bracketLen, 0f), stroke)
        drawLine(c, Offset(0f, 0f), Offset(0f, bracketLen), stroke)
        // Top Right
        drawLine(c, Offset(w, 0f), Offset(w - bracketLen, 0f), stroke)
        drawLine(c, Offset(w, 0f), Offset(w, bracketLen), stroke)
        // Bottom Left
        drawLine(c, Offset(0f, h), Offset(bracketLen, h), stroke)
        drawLine(c, Offset(0f, h), Offset(0f, h - bracketLen), stroke)
        // Bottom Right
        drawLine(c, Offset(w, h), Offset(w - bracketLen, h), stroke)
        drawLine(c, Offset(w, h), Offset(w, h - bracketLen), stroke)

        // Sweeping Laser Beam
        val beamY = h * laserY
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    JarvisColors.Accent.copy(alpha = 0.8f),
                    Color.White,
                    JarvisColors.Accent.copy(alpha = 0.8f),
                    Color.Transparent,
                ),
            ),
            start = Offset(0f, beamY),
            end = Offset(w, beamY),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
