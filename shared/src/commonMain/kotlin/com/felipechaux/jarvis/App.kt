package com.felipechaux.jarvis

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.felipechaux.jarvis.core.model.ServerCredentials
import com.felipechaux.jarvis.core.storage.CredentialStore
import com.felipechaux.jarvis.feature.chat.presentation.ui.ChatRoot
import com.felipechaux.jarvis.feature.pairing.presentation.ui.PairingRoot
import com.felipechaux.jarvis.feature.sessions.presentation.ui.SessionsRoot
import com.felipechaux.jarvis.presentation.components.JarvisIcon
import com.felipechaux.jarvis.presentation.components.JarvisIconKind
import com.felipechaux.jarvis.presentation.platform.AppPermission
import com.felipechaux.jarvis.presentation.platform.rememberPermissionRequest
import com.felipechaux.jarvis.presentation.theme.JarvisColors
import com.felipechaux.jarvis.presentation.theme.JarvisTheme
import com.felipechaux.jarvis.resources.Res
import com.felipechaux.jarvis.resources.tab_chat
import com.felipechaux.jarvis.resources.tab_sessions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private object Loading

@Composable
fun App() {
    JarvisTheme {
        val store = koinInject<CredentialStore>()
        val credentials by remember(store) { store.credentials }.collectAsState(initial = Loading)
        when (credentials) {
            Loading -> Box(Modifier.fillMaxSize())
            null -> Scaffold(
                containerColor = JarvisColors.Background,
            ) { padding ->
                Box(Modifier.padding(padding)) { PairingRoot(onPaired = {}) }
            }
            is ServerCredentials -> HomeTabs()
        }
    }
}

/** Chat and Sessions tabs styled according to OpenJarvis HUD dock. */
@Composable
private fun HomeTabs() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val requestNotifications = rememberPermissionRequest(AppPermission.NOTIFICATIONS) {}
    LaunchedEffect(Unit) { requestNotifications() }

    Scaffold(
        containerColor = JarvisColors.Background,
        bottomBar = {
            NavigationBar(
                containerColor = JarvisColors.BackgroundSecondary,
                tonalElevation = 0.dp,
                modifier = Modifier.border(width = 0.5.dp, color = JarvisColors.BorderSubtle),
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = {
                        JarvisIcon(
                            kind = JarvisIconKind.Chat,
                            tint = if (tab == 0) JarvisColors.Accent else JarvisColors.TextTertiary,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.tab_chat),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (tab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisColors.Accent,
                        unselectedIconColor = JarvisColors.TextTertiary,
                        selectedTextColor = JarvisColors.Accent,
                        unselectedTextColor = JarvisColors.TextTertiary,
                        indicatorColor = JarvisColors.Accent.copy(alpha = 0.15f),
                    ),
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        JarvisIcon(
                            kind = JarvisIconKind.Sessions,
                            tint = if (tab == 1) JarvisColors.Accent else JarvisColors.TextTertiary,
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(Res.string.tab_sessions),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (tab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp,
                            letterSpacing = 0.5.sp,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = JarvisColors.Accent,
                        unselectedIconColor = JarvisColors.TextTertiary,
                        selectedTextColor = JarvisColors.Accent,
                        unselectedTextColor = JarvisColors.TextTertiary,
                        indicatorColor = JarvisColors.Accent.copy(alpha = 0.15f),
                    ),
                )
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (tab == 0) ChatRoot() else SessionsRoot()
        }
    }
}
