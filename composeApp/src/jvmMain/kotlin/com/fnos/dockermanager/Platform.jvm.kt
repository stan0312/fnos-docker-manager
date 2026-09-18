package com.fnos.dockermanager

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import java.util.prefs.Preferences

actual fun createPlatformHttpClientEngine(): HttpClientEngine = CIO.create { }

actual fun createAppSettings(): Settings =
    PreferencesSettings(Preferences.userRoot().node("fnos-dockermanager"))

actual fun platformName(): String = "JVM"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
