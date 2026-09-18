package com.fnos.dockermanager.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 自定义图标（容器 / 镜像 / 仓库 / 网络 / 编排 / 服务器 / 卷 / 日志 / 统计）。
 * 均为 24x24 viewport 的简单几何图形。
 */
object AppIcons {

    val Container: ImageVector by lazy {
        ImageVector.Builder(
            name = "Container", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 2f)
                verticalLineTo(19f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                horizontalLineTo(4f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                verticalLineTo(5f)
                arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, -2f)
                close()
                moveTo(4f, 9f)
                horizontalLineTo(20f)
                verticalLineTo(5f)
                horizontalLineTo(4f)
                close()
                moveTo(6f, 12f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 8f, 12f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 12f)
                moveTo(10f, 12f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 12f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = false, 10f, 12f)
            }
        }.build()
    }

    val Image: ImageVector by lazy {
        ImageVector.Builder(
            name = "Image", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 4f)
                horizontalLineTo(21f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 1f)
                verticalLineTo(19f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1f, 1f)
                horizontalLineTo(3f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1f, -1f)
                verticalLineTo(5f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, -1f)
                close()
                moveTo(4f, 18f)
                lineTo(10f, 11f)
                lineTo(14f, 15f)
                lineTo(17f, 12f)
                lineTo(20f, 15f)
                verticalLineTo(6f)
                horizontalLineTo(4f)
                close()
                moveTo(9f, 8f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 12f, 8f)
                arcTo(1.5f, 1.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 8f)
            }
        }.build()
    }

    val Registry: ImageVector by lazy {
        ImageVector.Builder(
            name = "Registry", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 5f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 5f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 5f)
                close()
                moveTo(4f, 8f)
                verticalLineTo(16f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 16f)
                verticalLineTo(8f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 8f)
                close()
                moveTo(4f, 18.5f)
                verticalLineTo(19f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16f, 19f)
                verticalLineTo(18.5f)
                arcTo(8f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 4f, 18.5f)
                close()
            }
        }.build()
    }

    val Network: ImageVector by lazy {
        ImageVector.Builder(
            name = "Network", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 15f)
                close()
                moveTo(19f, 15f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 12f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 15f)
                close()
                moveTo(12f, 6f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 9f, 3f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 15f, 3f)
                arcTo(3f, 3f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 6f)
                close()
                moveTo(12f, 9f)
                lineTo(5f, 14f)
                moveTo(12f, 9f)
                lineTo(19f, 14f)
                moveTo(8f, 16f)
                lineTo(12f, 8f)
                moveTo(16f, 16f)
                lineTo(12f, 8f)
            }
        }.build()
    }

    val Compose: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compose", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.99f, 18.54f)
                lineTo(4.62f, 12.81f)
                lineTo(3f, 14.07f)
                lineTo(12f, 21f)
                lineTo(21f, 14.07f)
                lineTo(19.38f, 12.81f)
                lineTo(11.99f, 18.54f)
                close()
                moveTo(12f, 16f)
                lineTo(19.36f, 10.27f)
                lineTo(21f, 9f)
                lineTo(12f, 2f)
                lineTo(3f, 9f)
                lineTo(4.64f, 10.27f)
                lineTo(12f, 16f)
                close()
                moveTo(12f, 4.53f)
                lineTo(17.74f, 9f)
                lineTo(12f, 13.47f)
                lineTo(6.26f, 9f)
                lineTo(12f, 4.53f)
                close()
            }
        }.build()
    }

    val Server: ImageVector by lazy {
        ImageVector.Builder(
            name = "Server", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 4f)
                verticalLineTo(10f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 11f)
                horizontalLineTo(4f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 10f)
                verticalLineTo(4f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 3f)
                close()
                moveTo(4f, 13f)
                horizontalLineTo(20f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 14f)
                verticalLineTo(20f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 21f)
                horizontalLineTo(4f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 20f)
                verticalLineTo(14f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 13f)
                close()
                moveTo(6f, 5.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 5.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 5.5f)
                moveTo(9f, 5.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 10f, 5.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 5.5f)
                moveTo(6f, 15.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 7f, 15.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 6f, 15.5f)
                moveTo(9f, 15.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 10f, 15.5f)
                arcTo(0.5f, 0.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 9f, 15.5f)
            }
        }.build()
    }

    val Volume: ImageVector by lazy {
        ImageVector.Builder(
            name = "Volume", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                lineTo(2f, 9f)
                lineTo(12f, 15f)
                lineTo(22f, 9f)
                lineTo(12f, 3f)
                close()
                moveTo(2f, 15f)
                lineTo(12f, 21f)
                lineTo(22f, 15f)
                lineTo(19f, 13.5f)
                lineTo(12f, 17.5f)
                lineTo(5f, 13.5f)
                lineTo(2f, 15f)
                close()
            }
        }.build()
    }

    val Log: ImageVector by lazy {
        ImageVector.Builder(
            name = "Log", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 4f)
                horizontalLineTo(20f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 5f)
                verticalLineTo(19f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 20f)
                horizontalLineTo(4f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 19f)
                verticalLineTo(5f)
                arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 4f)
                close()
                moveTo(7f, 8f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 12f)
                horizontalLineTo(17f)
                verticalLineTo(14f)
                horizontalLineTo(7f)
                close()
                moveTo(7f, 16f)
                horizontalLineTo(13f)
                verticalLineTo(18f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    val Stats: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stats", defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 3f)
                verticalLineTo(21f)
                horizontalLineTo(21f)
                verticalLineTo(19f)
                horizontalLineTo(5f)
                verticalLineTo(3f)
                close()
                moveTo(7f, 17f)
                horizontalLineTo(9f)
                verticalLineTo(10f)
                horizontalLineTo(7f)
                close()
                moveTo(11f, 17f)
                horizontalLineTo(13f)
                verticalLineTo(6f)
                horizontalLineTo(11f)
                close()
                moveTo(15f, 17f)
                horizontalLineTo(17f)
                verticalLineTo(12f)
                horizontalLineTo(15f)
                close()
            }
        }.build()
    }
}
