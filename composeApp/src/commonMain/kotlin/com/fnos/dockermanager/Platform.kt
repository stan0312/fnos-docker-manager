package com.fnos.dockermanager

import com.russhwolf.settings.Settings
import io.ktor.client.engine.HttpClientEngine

/** 创建当前平台默认的 Ktor 引擎（iOS: Darwin，JVM/测试: CIO） */
expect fun createPlatformHttpClientEngine(): HttpClientEngine

/** 创建当前平台的持久化设置存储 */
expect fun createAppSettings(): Settings

/** 平台信息展示 */
expect fun platformName(): String

/** 当前 Unix 毫秒时间戳 */
expect fun currentTimeMillis(): Long
