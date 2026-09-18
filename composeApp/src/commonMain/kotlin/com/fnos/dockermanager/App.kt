package com.fnos.dockermanager

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.fnos.dockermanager.app.AppState
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.ui.MainScreen
import com.fnos.dockermanager.ui.theme.AppTheme

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val appState = remember { AppState(scope) }
    val darkTheme = when (appState.serverStore.loadThemeMode()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    AppTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(LocalAppState provides appState) {
            MainScreen()
        }
    }
}
