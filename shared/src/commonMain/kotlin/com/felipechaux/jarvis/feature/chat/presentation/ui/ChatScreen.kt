package com.felipechaux.jarvis.feature.chat.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.felipechaux.jarvis.core.presence.Presence
import com.felipechaux.jarvis.feature.chat.domain.model.ChatMessage
import com.felipechaux.jarvis.feature.chat.domain.model.ChatRole
import com.felipechaux.jarvis.feature.chat.domain.model.Conversation
import com.felipechaux.jarvis.feature.chat.presentation.state.ChatAction
import com.felipechaux.jarvis.feature.chat.presentation.state.ChatState
import com.felipechaux.jarvis.feature.chat.presentation.viewmodel.ChatViewModel
import com.felipechaux.jarvis.presentation.components.ArcReactor
import com.felipechaux.jarvis.presentation.components.JarvisIcon
import com.felipechaux.jarvis.presentation.components.JarvisIconKind
import com.felipechaux.jarvis.presentation.components.ToolCallCard
import com.felipechaux.jarvis.presentation.platform.AppPermission
import com.felipechaux.jarvis.presentation.platform.rememberPermissionRequest
import com.felipechaux.jarvis.presentation.theme.JarvisColors
import com.felipechaux.jarvis.presentation.toStringResource
import com.felipechaux.jarvis.resources.Res
import com.felipechaux.jarvis.resources.chat_delete
import com.felipechaux.jarvis.resources.chat_hero_subtitle
import com.felipechaux.jarvis.resources.chat_hero_title
import com.felipechaux.jarvis.resources.chat_history
import com.felipechaux.jarvis.resources.chat_history_empty
import com.felipechaux.jarvis.resources.chat_input_hint
import com.felipechaux.jarvis.resources.chat_new
import com.felipechaux.jarvis.resources.chat_suggest_1
import com.felipechaux.jarvis.resources.chat_suggest_2
import com.felipechaux.jarvis.resources.chat_suggest_3
import com.felipechaux.jarvis.resources.error_mic_denied
import com.felipechaux.jarvis.resources.presence_idle
import com.felipechaux.jarvis.resources.presence_listening
import com.felipechaux.jarvis.resources.presence_speaking
import com.felipechaux.jarvis.resources.presence_thinking
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoot(viewModel: ChatViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var micDenied by remember { mutableStateOf(false) }
    val requestMic = rememberPermissionRequest(AppPermission.MICROPHONE) { granted ->
        micDenied = !granted
        if (granted) viewModel.onAction(ChatAction.OnMicClick)
    }
    ChatScreen(
        state = state,
        micDenied = micDenied,
        onAction = { action ->
            if (action == ChatAction.OnMicClick && !state.isRecording) requestMic() else viewModel.onAction(action)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(state: ChatState, micDenied: Boolean, onAction: (ChatAction) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.content?.length) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(JarvisColors.Background)
            .imePadding(),
    ) {
        // Top HUD Navigation Bar
        ChatTopBar(state, onAction)

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (state.messages.isEmpty()) {
                ChatHeroEmptyState(state, onAction)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }

        // Error message if present
        val errorText = when {
            micDenied -> Res.string.error_mic_denied
            else -> state.error?.toStringResource()
        }
        if (errorText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(JarvisColors.ErrorSubtle, RoundedCornerShape(8.dp))
                    .border(1.dp, JarvisColors.Error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                JarvisIcon(JarvisIconKind.Close, size = 14.dp, tint = JarvisColors.Error)
                Text(
                    text = stringResource(errorText),
                    color = JarvisColors.Error,
                    fontSize = 12.sp,
                )
            }
        }

        // Floating Bottom Input Dock
        ChatInputDock(state, onAction)
    }

    if (state.showHistory) {
        ModalBottomSheet(
            onDismissRequest = { onAction(ChatAction.OnShowHistory(false)) },
            containerColor = JarvisColors.BackgroundSecondary,
            scrimColor = Color.Black.copy(alpha = 0.7f),
        ) {
            HistorySheetContent(state.conversations, state.conversationId, onAction)
        }
    }
}

@Composable
private fun ChatTopBar(state: ChatState, onAction: (ChatAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisColors.BackgroundSecondary)
            .border(width = 0.5.dp, color = JarvisColors.BorderSubtle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Mini Arc Reactor Core
        ArcReactor(
            size = 32.dp,
            presence = state.presence,
            micLevel = state.micLevel,
        )

        Spacer(Modifier.width(10.dp))

        // System Presence Status
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "J.A.R.V.I.S",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = JarvisColors.Accent,
                )
                // Status dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when (state.presence) {
                                Presence.THINKING -> JarvisColors.AccentAmber
                                Presence.LISTENING -> JarvisColors.Accent
                                Presence.SPEAKING -> JarvisColors.Success
                                Presence.IDLE -> JarvisColors.Accent.copy(alpha = 0.5f)
                            }
                        ),
                )
            }
            Text(
                text = stringResource(state.presence.label()),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = JarvisColors.TextSecondary,
                letterSpacing = 0.5.sp,
            )
        }

        // Action Buttons: History and New
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // History
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction(ChatAction.OnShowHistory(true)) }
                    .background(JarvisColors.BackgroundTertiary)
                    .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                JarvisIcon(JarvisIconKind.History, size = 14.dp, tint = JarvisColors.TextSecondary)
                Text(
                    text = stringResource(Res.string.chat_history),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = JarvisColors.TextPrimary,
                )
            }

            // New Conversation
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAction(ChatAction.OnNewConversation) }
                    .background(JarvisColors.Accent.copy(alpha = 0.12f))
                    .border(1.dp, JarvisColors.BorderHighlight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                JarvisIcon(JarvisIconKind.Plus, size = 14.dp, tint = JarvisColors.Accent)
                Text(
                    text = stringResource(Res.string.chat_new),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = JarvisColors.Accent,
                )
            }
        }
    }
}

@Composable
private fun ChatHeroEmptyState(state: ChatState, onAction: (ChatAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Holographic Arc Reactor Core
        ArcReactor(
            size = 140.dp,
            presence = state.presence,
            micLevel = state.micLevel,
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.chat_hero_title),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
            letterSpacing = 4.sp,
            color = JarvisColors.Accent,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.chat_hero_subtitle),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = JarvisColors.TextSecondary,
            letterSpacing = 1.sp,
        )

        Spacer(Modifier.height(32.dp))

        // Quick Suggestion Chips
        val suggestions = listOf(
            stringResource(Res.string.chat_suggest_1),
            stringResource(Res.string.chat_suggest_2),
            stringResource(Res.string.chat_suggest_3),
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { prompt ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JarvisColors.BackgroundSecondary)
                        .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable {
                            onAction(ChatAction.OnInputChange(prompt))
                            onAction(ChatAction.OnSendClick)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    JarvisIcon(JarvisIconKind.Terminal, size = 14.dp, tint = JarvisColors.Accent)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        color = JarvisColors.TextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (isUser) {
            // User Message Bubble
            Column(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .background(JarvisColors.UserBubble)
                    .border(1.dp, JarvisColors.UserBubbleBorder, RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = message.content,
                    fontSize = 14.sp,
                    color = JarvisColors.UserBubbleText,
                    lineHeight = 20.sp,
                )
            }
        } else {
            // JARVIS Assistant Message
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // JARVIS Header Indicator with rotating dashed circle style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .border(1.dp, JarvisColors.Accent.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(JarvisColors.Accent),
                        )
                    }

                    Text(
                        text = "J.A.R.V.I.S",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = JarvisColors.Accent,
                    )

                    Spacer(Modifier.weight(1f))

                    // Quick copy action
                    if (message.content.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    clipboard.setText(AnnotatedString(message.content))
                                    copied = true
                                }
                                .padding(4.dp),
                        ) {
                            JarvisIcon(
                                kind = if (copied) JarvisIconKind.Check else JarvisIconKind.Copy,
                                size = 12.dp,
                                tint = if (copied) JarvisColors.Success else JarvisColors.TextTertiary,
                            )
                        }
                    }
                }

                // Tool Calls
                if (message.toolCalls.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        message.toolCalls.forEach { tc ->
                            ToolCallCard(tc)
                        }
                    }
                }

                // Message Text Content
                if (message.content.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                            .background(JarvisColors.Surface)
                            .border(1.dp, JarvisColors.BorderSubtle, RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = message.content,
                            fontSize = 14.sp,
                            color = JarvisColors.TextPrimary,
                            lineHeight = 22.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputDock(state: ChatState, onAction: (ChatAction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisColors.BackgroundSecondary)
            .border(width = 0.5.dp, color = JarvisColors.BorderSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // OpenJarvis Voice Mic Button with dynamic equalizer bars
        JarvisMicButton(
            state = state,
            onClick = { onAction(ChatAction.OnMicClick) },
        )

        // Text Field
        OutlinedTextField(
            value = state.input,
            onValueChange = { onAction(ChatAction.OnInputChange(it)) },
            placeholder = {
                Text(
                    text = stringResource(Res.string.chat_input_hint),
                    fontSize = 13.sp,
                    color = JarvisColors.TextTertiary,
                )
            },
            modifier = Modifier.weight(1f),
            maxLines = 4,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = JarvisColors.CodeBackground,
                unfocusedContainerColor = JarvisColors.CodeBackground,
                focusedBorderColor = JarvisColors.Accent,
                unfocusedBorderColor = JarvisColors.BorderSubtle,
                focusedTextColor = JarvisColors.TextPrimary,
                unfocusedTextColor = JarvisColors.TextPrimary,
                cursorColor = JarvisColors.Accent,
            ),
        )

        // Send / Stop Action Button
        if (state.isStreaming || state.isSpeaking) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(JarvisColors.Error.copy(alpha = 0.15f))
                    .border(1.dp, JarvisColors.Error, CircleShape)
                    .clickable { onAction(ChatAction.OnStopClick) },
                contentAlignment = Alignment.Center,
            ) {
                JarvisIcon(JarvisIconKind.Stop, size = 16.dp, tint = JarvisColors.Error)
            }
        } else {
            val enabled = state.input.isNotBlank()
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (enabled) JarvisColors.Accent.copy(alpha = 0.2f) else JarvisColors.BackgroundTertiary
                    )
                    .border(
                        1.dp,
                        if (enabled) JarvisColors.Accent else JarvisColors.BorderSubtle,
                        CircleShape,
                    )
                    .clickable(enabled = enabled) { onAction(ChatAction.OnSendClick) },
                contentAlignment = Alignment.Center,
            ) {
                JarvisIcon(
                    kind = JarvisIconKind.Send,
                    size = 18.dp,
                    tint = if (enabled) JarvisColors.Accent else JarvisColors.TextTertiary,
                )
            }
        }
    }
}

@Composable
private fun JarvisMicButton(state: ChatState, onClick: () -> Unit) {
    val isRecording = state.isRecording
    val scale = if (isRecording) 1f + state.micLevel * 0.25f else 1f

    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                if (isRecording) JarvisColors.Error.copy(alpha = 0.2f) else JarvisColors.BackgroundTertiary
            )
            .border(
                1.dp,
                if (isRecording) JarvisColors.Error else JarvisColors.Border,
                CircleShape,
            )
            .clickable(
                enabled = !state.isStreaming && !state.isTranscribing,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isRecording) {
            // Live 5 Equalizer Waveform Bars (animated)
            EqualizerBars()
        } else {
            JarvisIcon(
                kind = JarvisIconKind.Mic,
                size = 18.dp,
                tint = if (state.isTranscribing) JarvisColors.AccentAmber else JarvisColors.Accent,
            )
        }
    }
}

@Composable
private fun EqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "eqAnims")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1",
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2",
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 6f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3",
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 16f, targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4",
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 5f, targetValue = 15f,
        animationSpec = infiniteRepeatable(tween(330, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h5",
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(20.dp),
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(JarvisColors.Error),
            )
        }
    }
}

@Composable
private fun HistorySheetContent(
    conversations: List<Conversation>,
    activeId: String,
    onAction: (ChatAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            JarvisIcon(JarvisIconKind.History, size = 18.dp, tint = JarvisColors.Accent)
            Text(
                text = "HISTORIAL DE CONVERSACIONES",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = JarvisColors.TextPrimary,
            )
        }

        if (conversations.isEmpty()) {
            Text(
                text = stringResource(Res.string.chat_history_empty),
                fontSize = 13.sp,
                color = JarvisColors.TextSecondary,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(conversations, key = { it.id }) { conversation ->
                val isActive = conversation.id == activeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isActive) JarvisColors.BackgroundTertiary else JarvisColors.Surface)
                        .border(
                            1.dp,
                            if (isActive) JarvisColors.BorderHighlight else JarvisColors.BorderSubtle,
                            RoundedCornerShape(10.dp),
                        )
                        .clickable { onAction(ChatAction.OnOpenConversation(conversation.id)) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(JarvisColors.Accent),
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = conversation.title.ifBlank { "Conversación" },
                        style = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) JarvisColors.Accent else JarvisColors.TextPrimary,
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )

                    IconButton(
                        onClick = { onAction(ChatAction.OnDeleteConversation(conversation.id)) },
                        modifier = Modifier.size(28.dp),
                    ) {
                        JarvisIcon(JarvisIconKind.Trash, size = 14.dp, tint = JarvisColors.TextTertiary)
                    }
                }
            }
        }
    }
}

private fun Presence.label(): StringResource = when (this) {
    Presence.IDLE -> Res.string.presence_idle
    Presence.LISTENING -> Res.string.presence_listening
    Presence.THINKING -> Res.string.presence_thinking
    Presence.SPEAKING -> Res.string.presence_speaking
}
