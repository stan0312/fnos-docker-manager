package com.fnos.dockermanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6AC4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E5FF),
    onPrimaryContainer = Color(0xFF0B3E78),
    secondary = Color(0xFF00B4A0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBDF4EC),
    onSecondaryContainer = Color(0xFF005B51),
    tertiary = Color(0xFF2E7D32),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF3F8),
    error = Color(0xFFD93025),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB8FF),
    onPrimary = Color(0xFF0B3E78),
    primaryContainer = Color(0xFF14518F),
    onPrimaryContainer = Color(0xFFD6E5FF),
    secondary = Color(0xFF5FD4C4),
    onSecondary = Color(0xFF005B51),
    secondaryContainer = Color(0xFF007A6C),
    onSecondaryContainer = Color(0xFFBDF4EC),
    tertiary = Color(0xFF8FD694),
    background = Color(0xFF101418),
    surface = Color(0xFF161B21),
    surfaceVariant = Color(0xFF1E242C),
    error = Color(0xFFFF8A80),
)

@Composable
fun AppTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
