@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.fnos.dockermanager.ui

import com.fnos.dockermanager.currentTimeMillis
import com.fnos.dockermanager.docker.ContainerStats
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

/** 字节数人类可读格式化 */
fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "-"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB", "PB")
    var value = bytes.toDouble()
    var unit = -1
    while (value >= 1024 && unit < units.size - 1) {
        value /= 1024
        unit++
    }
    val v = if (value >= 100) value.roundToLong().toString()
    else formatDecimal(value, 1)
    return "$v ${units[unit]}"
}

/** 相对时间：刚刚 / N分钟前 / N小时前 / 昨天 / N天前 / 日期 */
fun formatRelativeTime(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "-"
    val diffSec = (currentTimeMillis() / 1000) - epochSeconds
    return when {
        diffSec < 60 -> "刚刚"
        diffSec < 3600 -> "${diffSec / 60} 分钟前"
        diffSec < 86400 -> "${diffSec / 3600} 小时前"
        diffSec < 172800 -> "昨天"
        diffSec < 604800 -> "${diffSec / 86400} 天前"
        else -> formatDate(epochSeconds)
    }
}

/** epoch 秒 → "yyyy-MM-dd HH:mm"（本地时区） */
fun formatDate(epochSeconds: Long): String {
    if (epochSeconds <= 0) return "-"
    val dt = Instant.fromEpochSeconds(epochSeconds)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return "${dt.year}-${padZero(dt.monthNumber, 2)}-${padZero(dt.dayOfMonth, 2)} " +
        "${padZero(dt.hour, 2)}:${padZero(dt.minute, 2)}"
}

/** Docker 时间字符串（RFC3339）→ 相对时间 */
fun formatRfc3339Relative(value: String): String {
    if (value.isBlank()) return "-"
    return runCatching {
        formatRelativeTime(Instant.parse(value).epochSeconds)
    }.getOrDefault(value)
}

/** CPU 百分比（基于两次 stats 快照） */
fun cpuPercent(current: ContainerStats, previous: ContainerStats?): Double {
    val c = current.cpuStats
    val p = previous?.cpuStats
    if (p == null || p.systemCpuUsage <= 0) return 0.0
    val cpuDelta = (c.cpuUsage.totalUsage - p.cpuUsage.totalUsage).toDouble()
    val sysDelta = (c.systemCpuUsage - p.systemCpuUsage).toDouble()
    if (sysDelta <= 0) return 0.0
    val online = c.onlineCpus.takeIf { it > 0 } ?: 1
    return (cpuDelta / sysDelta * online * 100).coerceIn(0.0, 999.0)
}
