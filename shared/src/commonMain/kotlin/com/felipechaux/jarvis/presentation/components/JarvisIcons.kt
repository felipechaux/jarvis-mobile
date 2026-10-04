package com.felipechaux.jarvis.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.felipechaux.jarvis.presentation.theme.JarvisColors

sealed class JarvisIconKind {
    data object Chat : JarvisIconKind()
    data object Sessions : JarvisIconKind()
    data object Mic : JarvisIconKind()
    data object Send : JarvisIconKind()
    data object Stop : JarvisIconKind()
    data object History : JarvisIconKind()
    data object Plus : JarvisIconKind()
    data object Trash : JarvisIconKind()
    data object ChevronDown : JarvisIconKind()
    data object ChevronRight : JarvisIconKind()
    data object Check : JarvisIconKind()
    data object Close : JarvisIconKind()
    data object Terminal : JarvisIconKind()
    data object Copy : JarvisIconKind()
    data object QrCode : JarvisIconKind()
}

@Composable
fun JarvisIcon(
    kind: JarvisIconKind,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = JarvisColors.TextPrimary,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (w * 0.08f).coerceAtLeast(1.5f)

        when (kind) {
            JarvisIconKind.Chat -> drawChatIcon(tint, stroke)
            JarvisIconKind.Sessions -> drawSessionsIcon(tint, stroke)
            JarvisIconKind.Mic -> drawMicIcon(tint, stroke)
            JarvisIconKind.Send -> drawSendIcon(tint, stroke)
            JarvisIconKind.Stop -> drawStopIcon(tint, stroke)
            JarvisIconKind.History -> drawHistoryIcon(tint, stroke)
            JarvisIconKind.Plus -> drawPlusIcon(tint, stroke)
            JarvisIconKind.Trash -> drawTrashIcon(tint, stroke)
            JarvisIconKind.ChevronDown -> drawChevronDownIcon(tint, stroke)
            JarvisIconKind.ChevronRight -> drawChevronRightIcon(tint, stroke)
            JarvisIconKind.Check -> drawCheckIcon(tint, stroke)
            JarvisIconKind.Close -> drawCloseIcon(tint, stroke)
            JarvisIconKind.Terminal -> drawTerminalIcon(tint, stroke)
            JarvisIconKind.Copy -> drawCopyIcon(tint, stroke)
            JarvisIconKind.QrCode -> drawQrCodeIcon(tint, stroke)
        }
    }
}

private fun DrawScope.drawChatIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.2f)
        lineTo(size.width * 0.85f, size.height * 0.2f)
        quadraticTo(size.width * 0.95f, size.height * 0.2f, size.width * 0.95f, size.height * 0.35f)
        lineTo(size.width * 0.95f, size.height * 0.65f)
        quadraticTo(size.width * 0.95f, size.height * 0.8f, size.width * 0.85f, size.height * 0.8f)
        lineTo(size.width * 0.45f, size.height * 0.8f)
        lineTo(size.width * 0.25f, size.height * 0.95f)
        lineTo(size.width * 0.25f, size.height * 0.8f)
        lineTo(size.width * 0.15f, size.height * 0.8f)
        quadraticTo(size.width * 0.05f, size.height * 0.8f, size.width * 0.05f, size.height * 0.65f)
        lineTo(size.width * 0.05f, size.height * 0.35f)
        quadraticTo(size.width * 0.05f, size.height * 0.2f, size.width * 0.15f, size.height * 0.2f)
        close()
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Small inner accent line
    drawLine(
        color = tint,
        start = Offset(size.width * 0.28f, size.height * 0.5f),
        end = Offset(size.width * 0.72f, size.height * 0.5f),
        strokeWidth = stroke * 0.9f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSessionsIcon(tint: Color, stroke: Float) {
    // Terminal window frame
    val rectPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = size.width * 0.08f,
                top = size.height * 0.12f,
                right = size.width * 0.92f,
                bottom = size.height * 0.88f,
                cornerRadius = CornerRadius(size.width * 0.12f),
            )
        )
    }
    drawPath(rectPath, color = tint, style = Stroke(width = stroke))
    // Activity pulse / telemetry wave inside
    val pulse = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.52f)
        lineTo(size.width * 0.35f, size.height * 0.52f)
        lineTo(size.width * 0.45f, size.height * 0.32f)
        lineTo(size.width * 0.55f, size.height * 0.72f)
        lineTo(size.width * 0.65f, size.height * 0.42f)
        lineTo(size.width * 0.72f, size.height * 0.52f)
        lineTo(size.width * 0.82f, size.height * 0.52f)
    }
    drawPath(pulse, color = tint, style = Stroke(width = stroke * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawMicIcon(tint: Color, stroke: Float) {
    // Mic capsule
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.35f, size.height * 0.12f),
        size = Size(size.width * 0.3f, size.height * 0.48f),
        cornerRadius = CornerRadius(size.width * 0.15f),
        style = Stroke(width = stroke),
    )
    // Cradle arc
    val arcPath = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.44f)
        quadraticTo(size.width * 0.22f, size.height * 0.72f, size.width * 0.5f, size.height * 0.72f)
        quadraticTo(size.width * 0.78f, size.height * 0.72f, size.width * 0.78f, size.height * 0.44f)
    }
    drawPath(arcPath, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round))
    // Mic stand stem
    drawLine(
        color = tint,
        start = Offset(size.width * 0.5f, size.height * 0.72f),
        end = Offset(size.width * 0.5f, size.height * 0.88f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Base
    drawLine(
        color = tint,
        start = Offset(size.width * 0.32f, size.height * 0.88f),
        end = Offset(size.width * 0.68f, size.height * 0.88f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawSendIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.85f)
        lineTo(size.width * 0.88f, size.height * 0.5f)
        lineTo(size.width * 0.15f, size.height * 0.15f)
        lineTo(size.width * 0.32f, size.height * 0.5f)
        close()
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(
        color = tint,
        start = Offset(size.width * 0.32f, size.height * 0.5f),
        end = Offset(size.width * 0.88f, size.height * 0.5f),
        strokeWidth = stroke * 0.8f,
    )
}

private fun DrawScope.drawStopIcon(tint: Color, stroke: Float) {
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.25f, size.height * 0.25f),
        size = Size(size.width * 0.5f, size.height * 0.5f),
        cornerRadius = CornerRadius(size.width * 0.08f),
    )
}

private fun DrawScope.drawHistoryIcon(tint: Color, stroke: Float) {
    // Clock circle
    drawCircle(
        color = tint,
        radius = size.width * 0.38f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        style = Stroke(width = stroke),
    )
    // Clock hands
    drawLine(
        color = tint,
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.5f, size.height * 0.26f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.68f, size.height * 0.5f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPlusIcon(tint: Color, stroke: Float) {
    drawLine(
        color = tint,
        start = Offset(size.width * 0.5f, size.height * 0.18f),
        end = Offset(size.width * 0.5f, size.height * 0.82f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.18f, size.height * 0.5f),
        end = Offset(size.width * 0.82f, size.height * 0.5f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTrashIcon(tint: Color, stroke: Float) {
    // Bin body
    val body = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.35f)
        lineTo(size.width * 0.30f, size.height * 0.85f)
        quadraticTo(size.width * 0.32f, size.height * 0.90f, size.width * 0.40f, size.height * 0.90f)
        lineTo(size.width * 0.60f, size.height * 0.90f)
        quadraticTo(size.width * 0.68f, size.height * 0.90f, size.width * 0.70f, size.height * 0.85f)
        lineTo(size.width * 0.75f, size.height * 0.35f)
    }
    drawPath(body, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Bin lid
    drawLine(
        color = tint,
        start = Offset(size.width * 0.18f, size.height * 0.35f),
        end = Offset(size.width * 0.82f, size.height * 0.35f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Lid handle
    val handle = Path().apply {
        moveTo(size.width * 0.40f, size.height * 0.35f)
        lineTo(size.width * 0.40f, size.height * 0.22f)
        lineTo(size.width * 0.60f, size.height * 0.22f)
        lineTo(size.width * 0.60f, size.height * 0.35f)
    }
    drawPath(handle, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawChevronDownIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.38f)
        lineTo(size.width * 0.5f, size.height * 0.62f)
        lineTo(size.width * 0.75f, size.height * 0.38f)
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawChevronRightIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.38f, size.height * 0.25f)
        lineTo(size.width * 0.62f, size.height * 0.5f)
        lineTo(size.width * 0.38f, size.height * 0.75f)
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawCheckIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.52f)
        lineTo(size.width * 0.42f, size.height * 0.75f)
        lineTo(size.width * 0.82f, size.height * 0.28f)
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun DrawScope.drawCloseIcon(tint: Color, stroke: Float) {
    drawLine(
        color = tint,
        start = Offset(size.width * 0.25f, size.height * 0.25f),
        end = Offset(size.width * 0.75f, size.height * 0.75f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = tint,
        start = Offset(size.width * 0.75f, size.height * 0.25f),
        end = Offset(size.width * 0.25f, size.height * 0.75f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTerminalIcon(tint: Color, stroke: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.22f, size.height * 0.32f)
        lineTo(size.width * 0.48f, size.height * 0.5f)
        lineTo(size.width * 0.22f, size.height * 0.68f)
    }
    drawPath(path, color = tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(
        color = tint,
        start = Offset(size.width * 0.52f, size.height * 0.68f),
        end = Offset(size.width * 0.78f, size.height * 0.68f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawCopyIcon(tint: Color, stroke: Float) {
    // Back square
    drawRoundRect(
        color = tint.copy(alpha = 0.6f),
        topLeft = Offset(size.width * 0.32f, size.height * 0.15f),
        size = Size(size.width * 0.52f, size.height * 0.52f),
        cornerRadius = CornerRadius(size.width * 0.08f),
        style = Stroke(width = stroke),
    )
    // Front square
    drawRoundRect(
        color = tint,
        topLeft = Offset(size.width * 0.16f, size.height * 0.32f),
        size = Size(size.width * 0.52f, size.height * 0.52f),
        cornerRadius = CornerRadius(size.width * 0.08f),
        style = Stroke(width = stroke),
    )
}

private fun DrawScope.drawQrCodeIcon(tint: Color, stroke: Float) {
    val cornerLen = size.width * 0.26f
    // Top-left
    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.15f), Offset(size.width * 0.15f + cornerLen, size.height * 0.15f), stroke, StrokeCap.Round)
    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.15f), Offset(size.width * 0.15f, size.height * 0.15f + cornerLen), stroke, StrokeCap.Round)
    // Top-right
    drawLine(tint, Offset(size.width * 0.85f, size.height * 0.15f), Offset(size.width * 0.85f - cornerLen, size.height * 0.15f), stroke, StrokeCap.Round)
    drawLine(tint, Offset(size.width * 0.85f, size.height * 0.15f), Offset(size.width * 0.85f, size.height * 0.15f + cornerLen), stroke, StrokeCap.Round)
    // Bottom-left
    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.85f), Offset(size.width * 0.15f + cornerLen, size.height * 0.85f), stroke, StrokeCap.Round)
    drawLine(tint, Offset(size.width * 0.15f, size.height * 0.85f), Offset(size.width * 0.15f, size.height * 0.85f - cornerLen), stroke, StrokeCap.Round)
    // Bottom-right
    drawLine(tint, Offset(size.width * 0.85f, size.height * 0.85f), Offset(size.width * 0.85f - cornerLen, size.height * 0.85f), stroke, StrokeCap.Round)
    drawLine(tint, Offset(size.width * 0.85f, size.height * 0.85f), Offset(size.width * 0.85f, size.height * 0.85f - cornerLen), stroke, StrokeCap.Round)
    // Center dot
    drawCircle(tint, radius = stroke * 1.2f, center = Offset(size.width * 0.5f, size.height * 0.5f))
}
