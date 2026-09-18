package com.fnos.dockermanager

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.CoreFoundation.CFAbsoluteTimeGetCurrent
import platform.CoreFoundation.kCFAbsoluteTimeIntervalSince1970
import platform.Foundation.NSUserDefaults

actual fun createPlatformHttpClientEngine(): HttpClientEngine = Darwin.create { }

actual fun createAppSettings(): Settings =
    NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)

actual fun platformName(): String = "iOS"

actual fun currentTimeMillis(): Long =
    ((CFAbsoluteTimeGetCurrent() + kCFAbsoluteTimeIntervalSince1970) * 1000).toLong()
