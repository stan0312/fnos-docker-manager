package com.fnos.dockermanager.ui

import kotlin.math.roundToLong

/** 跨平台数字格式化（不依赖 java.lang.String.format） */
fun formatDecimal(value: Double, decimals: Int = 1): String {
    if (decimals <= 0) return value.roundToLong().toString()
    val factor = pow10(decimals)
    val rounded = (value * factor).roundToLong()
    val intPart = rounded / factor
    val fracPart = (rounded % factor).toInt()
    return "$intPart.${fracPart.toString().padStart(decimals, '0')}"
}

/** 整数左侧补零 */
fun padZero(value: Int, width: Int): String =
    value.toString().padStart(width, '0')

private fun pow10(n: Int): Long {
    var v = 1L
    repeat(n) { v *= 10 }
    return v
}
