package com.felipechaux.jarvis.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipechaux.jarvis.core.presence.Presence
import com.felipechaux.jarvis.presentation.theme.JarvisColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * OpenJarvis signature Arc Reactor HUD core.
 * Features rotating tick bezels, concentric geometric rings, orbital sweep scanner,
 * voice reactive ripple halo, and breathing core glow.
 */
@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    presence: Presence = Presence.IDLE,
    micLevel: Float = 0f,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ArcReactorTransitions")

    // Slow continuous base rotation (clockwise)
    val baseRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "baseRotation",
    )

    // Counter rotation for inner ring (counter-clockwise)
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "counterRotation",
    )

    // Fast orbital scanner sweep (active during THINKING)
    val scannerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (presence == Presence.THINKING) 1200 else 6000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scannerRotation",
    )

    // Breathing pulse for idle and thinking
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (presence == Presence.THINKING) 800 else 2400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    // Voice speaking waves
    val waveStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveStep",
    )

    val isCompact = size < 60.dp
    val primaryCyan = when (presence) {
        Presence.THINKING -> JarvisColors.AccentAmber
        Presence.LISTENING -> JarvisColors.Accent
        Presence.SPEAKING -> JarvisColors.Success
        Presence.IDLE -> JarvisColors.Accent
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f
            val hair = if (isCompact) 1.2f else 1.0f

            // 1. Ambient Background Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan.copy(alpha = if (presence == Presence.IDLE) 0.12f else 0.25f),
                        primaryCyan.copy(alpha = 0.03f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )

            // 2. Listening audio ripple / halo
            if (presence == Presence.LISTENING) {
                val voiceSwell = (1f + micLevel * 0.4f).coerceIn(1f, 1.5f)
                drawCircle(
                    color = JarvisColors.Accent.copy(alpha = 0.25f * (micLevel + 0.3f)),
                    radius = (radius * 0.85f) * voiceSwell,
                    center = center,
                    style = Stroke(width = hair * 2f),
                )
                drawCircle(
                    color = JarvisColors.Accent.copy(alpha = 0.15f * (micLevel + 0.2f)),
                    radius = (radius * 0.95f) * voiceSwell,
                    center = center,
                    style = Stroke(width = hair * 1.5f),
                )
            }

            // 3. Speaking Equalizer / radial sound bars
            if (presence == Presence.SPEAKING) {
                val waveCount = 16
                for (i in 0 until waveCount) {
                    val angle = (360f / waveCount) * i
                    val rad = (angle * PI / 180f).toFloat()
                    val waveHeight = (sin((rad * 4f) + (waveStep * 2f * PI)).toFloat() + 1f) * 0.5f
                    val barLen = radius * (0.12f + waveHeight * 0.15f)
                    val r1 = radius * 0.72f
                    val r2 = r1 + barLen
                    drawLine(
                        color = JarvisColors.Success.copy(alpha = 0.7f + waveHeight * 0.3f),
                        start = Offset(center.x + r1 * cos(rad), center.y + r1 * sin(rad)),
                        end = Offset(center.x + r2 * cos(rad), center.y + r2 * sin(rad)),
                        strokeWidth = hair * 2f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            // 4. Outer Bezel & Ticks (Clockwise rotation)
            rotate(degrees = baseRotation, pivot = center) {
                // Outer perimeter ring
                drawCircle(
                    color = primaryCyan.copy(alpha = 0.3f),
                    radius = radius * 0.96f,
                    center = center,
                    style = Stroke(width = hair),
                )

                if (!isCompact) {
                    // Outer tick marks
                    val tickCount = 36
                    for (i in 0 until tickCount) {
                        val angle = (360f / tickCount) * i
                        val rad = (angle * PI / 180f).toFloat()
                        val isMajor = i % 4 == 0
                        val r1 = if (isMajor) radius * 0.88f else radius * 0.91f
                        val r2 = radius * 0.95f
                        val p1 = Offset(center.x + r1 * cos(rad), center.y + r1 * sin(rad))
                        val p2 = Offset(center.x + r2 * cos(rad), center.y + r2 * sin(rad))
                        drawLine(
                            color = primaryCyan.copy(alpha = if (isMajor) 0.65f else 0.35f),
                            start = p1,
                            end = p2,
                            strokeWidth = if (isMajor) hair * 1.5f else hair,
                        )
                    }
                }
            }

            // 5. Middle Segmented Arcs (Counter-clockwise rotation)
            rotate(degrees = counterRotation, pivot = center) {
                val midR = radius * 0.75f
                val sweepAngle = 70f
                // 3 segmented arcs
                for (i in 0 until 3) {
                    val startAngle = i * 120f + 10f
                    drawArc(
                        color = primaryCyan.copy(alpha = 0.5f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - midR, center.y - midR),
                        size = Size(midR * 2, midR * 2),
                        style = Stroke(width = hair * 1.6f, cap = StrokeCap.Round),
                    )
                }

                if (!isCompact) {
                    // Small internal tick ring
                    val innerMidR = radius * 0.62f
                    drawCircle(
                        color = primaryCyan.copy(alpha = 0.25f),
                        radius = innerMidR,
                        center = center,
                        style = Stroke(width = hair),
                    )
                }
            }

            // 6. Orbital Scanner Sweep (Active during thinking or gentle sweep)
            rotate(degrees = scannerRotation, pivot = center) {
                val scanR = radius * 0.75f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryCyan.copy(alpha = if (presence == Presence.THINKING) 0.85f else 0.4f),
                        ),
                        center = center,
                    ),
                    startAngle = 0f,
                    sweepAngle = if (presence == Presence.THINKING) 90f else 45f,
                    useCenter = false,
                    topLeft = Offset(center.x - scanR, center.y - scanR),
                    size = Size(scanR * 2, scanR * 2),
                    style = Stroke(width = hair * 2.5f, cap = StrokeCap.Round),
                )
            }

            // 7. Core Inner Ring and Pulsing Arc Core
            val coreR = (radius * (if (isCompact) 0.38f else 0.32f)) * (if (presence == Presence.IDLE) pulse else 1f)
            drawCircle(
                color = primaryCyan.copy(alpha = 0.4f),
                radius = coreR * 1.35f,
                center = center,
                style = Stroke(width = hair),
            )

            // Inner glowing Core Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryCyan,
                        primaryCyan.copy(alpha = 0.8f),
                        primaryCyan.copy(alpha = 0.2f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = coreR,
                ),
                radius = coreR,
                center = center,
            )

            // Central pinpoint star
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = (coreR * 0.35f).coerceAtLeast(1.5f),
                center = center,
            )
        }
    }
}
