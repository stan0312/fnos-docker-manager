package com.fnos.dockermanager.app

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppState = staticCompositionLocalOf<AppState> {
    error("AppState 未提供")
}
