package com.felipechaux.jarvis.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipechaux.jarvis.feature.chat.domain.model.ToolCall
import com.felipechaux.jarvis.feature.chat.domain.model.ToolStatus
import com.felipechaux.jarvis.presentation.theme.JarvisColors

@Composable
fun ToolCallCard(
    toolCall: ToolCall,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (toolCall.status) {
        ToolStatus.RUNNING -> JarvisColors.Accent
        ToolStatus.SUCCEEDED -> JarvisColors.Success
        ToolStatus.FAILED -> JarvisColors.Error
    }

    val statusText = when (toolCall.status) {
        ToolStatus.RUNNING -> "RUNNING"
        ToolStatus.SUCCEEDED -> "DONE"
        ToolStatus.FAILED -> "FAILED"
    }

    val shape = RoundedCornerShape(8.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, JarvisColors.BorderSubtle, shape)
            .background(JarvisColors.BackgroundTertiary, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JarvisIcon(
                kind = if (expanded) JarvisIconKind.ChevronDown else JarvisIconKind.ChevronRight,
                size = 12.dp,
                tint = JarvisColors.TextTertiary,
            )

            // Status indicator
            ToolStatusIndicator(toolCall.status, statusColor)

            // Terminal tool badge
            Text(
                text = ">_ ${toolCall.tool}",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = JarvisColors.TextPrimary,
            )

            val latency = toolCall.latencyMs?.let { "${it}ms" }
            if (latency != null) {
                Text(
                    text = latency,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = JarvisColors.TextTertiary,
                )
            }

            Box(modifier = Modifier.weight(1f))

            // Status chip tag
            Text(
                text = statusText,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                color = statusColor,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(JarvisColors.CodeBackground)
                    .border(1.dp, JarvisColors.BorderSubtle)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "TOOL CALL EXECUTION",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = JarvisColors.TextTertiary,
                    letterSpacing = 1.sp,
                )
                Text(
                    text = "tool: ${toolCall.tool}\nstatus: ${toolCall.status}" +
                        if (toolCall.arguments.isNotBlank()) "\nargs: ${toolCall.arguments}" else "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = JarvisColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun ToolStatusIndicator(status: ToolStatus, color: Color) {
    if (status == ToolStatus.RUNNING) {
        val infiniteTransition = rememberInfiniteTransition(label = "toolSpinner")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "toolRotation",
        )
        JarvisIcon(
            kind = JarvisIconKind.Sessions,
            size = 12.dp,
            tint = color,
            modifier = Modifier.rotate(rotation),
        )
    } else {
        JarvisIcon(
            kind = if (status == ToolStatus.SUCCEEDED) JarvisIconKind.Check else JarvisIconKind.Close,
            size = 12.dp,
            tint = color,
        )
    }
}
