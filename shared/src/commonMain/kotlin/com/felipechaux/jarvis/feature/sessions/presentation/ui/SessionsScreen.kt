package com.felipechaux.jarvis.feature.sessions.presentation.ui

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipechaux.jarvis.core.network.RpcConnection
import com.felipechaux.jarvis.feature.sessions.domain.model.LiveSession
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionEvent
import com.felipechaux.jarvis.feature.sessions.domain.model.SessionStatus
import com.felipechaux.jarvis.feature.sessions.presentation.state.SessionsAction
import com.felipechaux.jarvis.feature.sessions.presentation.state.SessionsState
import com.felipechaux.jarvis.feature.sessions.presentation.viewmodel.SessionsViewModel
import com.felipechaux.jarvis.presentation.components.JarvisIcon
import com.felipechaux.jarvis.presentation.components.JarvisIconKind
import com.felipechaux.jarvis.presentation.theme.JarvisColors
import com.felipechaux.jarvis.resources.Res
import com.felipechaux.jarvis.resources.connection_connected
import com.felipechaux.jarvis.resources.connection_connecting
import com.felipechaux.jarvis.resources.connection_disconnected
import com.felipechaux.jarvis.resources.connection_incompatible
import com.felipechaux.jarvis.resources.connection_unauthorized
import com.felipechaux.jarvis.resources.session_status_idle
import com.felipechaux.jarvis.resources.session_status_prompt
import com.felipechaux.jarvis.resources.session_status_working
import com.felipechaux.jarvis.resources.sessions_attention_required
import com.felipechaux.jarvis.resources.sessions_empty
import com.felipechaux.jarvis.resources.sessions_telemetry
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SessionsRoot(viewModel: SessionsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SessionsScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun SessionsScreen(state: SessionsState, onAction: (SessionsAction) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisColors.Background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Status & Connection Header HUD
        item {
            ConnectionHudHeader(state, onAction)
        }

        // Section Title: Live Sessions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    JarvisIcon(JarvisIconKind.Sessions, size = 16.dp, tint = JarvisColors.Accent)
                    Text(
                        text = "SESIONES DEL MAC",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = JarvisColors.TextPrimary,
                    )
                }

                if (state.sessions.isNotEmpty()) {
                    Text(
                        text = "${state.sessions.size} ACTIVAS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = JarvisColors.Accent,
                        modifier = Modifier
                            .background(JarvisColors.Accent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }

        if (state.sessions.isEmpty()) {
            item {
                SessionsEmptyCard()
            }
        } else {
            items(state.sessions, key = { it.name }) { session ->
                SessionCard(session, onAction)
            }
        }

        // Section: Telemetry & Live Event Console
        if (state.feed.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    JarvisIcon(JarvisIconKind.Terminal, size = 16.dp, tint = JarvisColors.Accent)
                    Text(
                        text = stringResource(Res.string.sessions_telemetry),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = JarvisColors.TextPrimary,
                    )
                }
            }

            item {
                TelemetryFeedConsole(state.feed)
            }
        }
    }
}

@Composable
private fun ConnectionHudHeader(state: SessionsState, onAction: (SessionsAction) -> Unit) {
    val isConnected = state.connection == RpcConnection.CONNECTED
    val isConnecting = state.connection == RpcConnection.CONNECTING
    val statusColor = when {
        isConnected -> JarvisColors.Success
        isConnecting -> JarvisColors.AccentAmber
        else -> JarvisColors.Error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisColors.BackgroundSecondary)
            .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status dot with breathing pulse
        val infiniteTransition = rememberInfiniteTransition(label = "connPulse")
        val dotAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "dotAlpha",
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = if (isConnected) 1f else dotAlpha)),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(state.connection.label()),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = JarvisColors.TextPrimary,
            )
            Text(
                text = "RPC WebSocket · Protocolo v1",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = JarvisColors.TextSecondary,
            )
        }

        // Refresh button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(JarvisColors.BackgroundTertiary)
                .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(8.dp))
                .clickable { onAction(SessionsAction.OnRefresh) },
            contentAlignment = Alignment.Center,
        ) {
            JarvisIcon(JarvisIconKind.History, size = 14.dp, tint = JarvisColors.TextSecondary)
        }
    }
}

@Composable
private fun SessionsEmptyCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisColors.BackgroundSecondary)
            .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        JarvisIcon(JarvisIconKind.Sessions, size = 32.dp, tint = JarvisColors.TextTertiary)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.sessions_empty),
            fontSize = 13.sp,
            color = JarvisColors.TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Las tareas autónomas de Claude Code o Antigravity aparecerán aquí.",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = JarvisColors.TextTertiary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionCard(session: LiveSession, onAction: (SessionsAction) -> Unit) {
    val isPrompt = session.status == SessionStatus.PROMPT
    val isWorking = session.status == SessionStatus.WORKING

    val borderColor = when {
        isPrompt -> JarvisColors.AccentAmber
        isWorking -> JarvisColors.Accent
        else -> JarvisColors.BorderSubtle
    }

    val cardBg = when {
        isPrompt -> JarvisColors.Surface
        else -> JarvisColors.BackgroundSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(
                width = if (isPrompt) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Header: Project & CLI badges + Status pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Project badge
                Text(
                    text = session.project,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = JarvisColors.TextPrimary,
                )

                // CLI badge
                Text(
                    text = ">_ ${session.cli}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = JarvisColors.Accent,
                    modifier = Modifier
                        .background(JarvisColors.BackgroundTertiary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }

            // Elapsed time & Status tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.elapsedS?.let { seconds ->
                    Text(
                        text = "⏱ ${seconds}s",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = JarvisColors.TextSecondary,
                    )
                }

                val statusTagColor = when (session.status) {
                    SessionStatus.PROMPT -> JarvisColors.AccentAmber
                    SessionStatus.WORKING -> JarvisColors.Accent
                    SessionStatus.IDLE -> JarvisColors.TextTertiary
                }

                Text(
                    text = when (session.status) {
                        SessionStatus.PROMPT -> stringResource(Res.string.sessions_attention_required)
                        SessionStatus.WORKING -> stringResource(Res.string.session_status_working).uppercase()
                        SessionStatus.IDLE -> stringResource(Res.string.session_status_idle).uppercase()
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusTagColor,
                    modifier = Modifier
                        .background(statusTagColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, statusTagColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // Active Prompt Container (Claude Code permission request)
        session.prompt?.let { prompt ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisColors.BackgroundTertiary)
                    .border(1.dp, JarvisColors.AccentAmber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    JarvisIcon(JarvisIconKind.Sessions, size = 14.dp, tint = JarvisColors.AccentAmber)
                    Text(
                        text = prompt.question,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = JarvisColors.TextPrimary,
                    )
                }

                // Interactive Answer Options (cybernetic buttons)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    prompt.options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(JarvisColors.AccentAmber.copy(alpha = 0.15f))
                                .border(1.dp, JarvisColors.AccentAmber, RoundedCornerShape(6.dp))
                                .clickable {
                                    onAction(SessionsAction.OnAnswer(session.name, prompt.id, option.key))
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "[${option.key}]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = JarvisColors.AccentAmber,
                            )
                            Text(
                                text = option.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = JarvisColors.TextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryFeedConsole(feed: List<SessionEvent>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(JarvisColors.CodeBackground)
            .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        feed.forEach { event ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Event Tag
                Text(
                    text = "[${event.kind.uppercase()}]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (event.kind.lowercase()) {
                        "prompt" -> JarvisColors.AccentAmber
                        "agent_event" -> JarvisColors.Accent
                        "done" -> JarvisColors.Success
                        else -> JarvisColors.TextSecondary
                    },
                )

                // Project
                Text(
                    text = event.project,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = JarvisColors.TextTertiary,
                )

                // Text
                Text(
                    text = event.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = JarvisColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun RpcConnection.label(): StringResource = when (this) {
    RpcConnection.CONNECTED -> Res.string.connection_connected
    RpcConnection.CONNECTING -> Res.string.connection_connecting
    RpcConnection.DISCONNECTED -> Res.string.connection_disconnected
    RpcConnection.UNAUTHORIZED -> Res.string.connection_unauthorized
    RpcConnection.INCOMPATIBLE -> Res.string.connection_incompatible
}

private fun SessionStatus.label(): StringResource = when (this) {
    SessionStatus.PROMPT -> Res.string.session_status_prompt
    SessionStatus.WORKING -> Res.string.session_status_working
    SessionStatus.IDLE -> Res.string.session_status_idle
}
