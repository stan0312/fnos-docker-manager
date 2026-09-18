package com.fnos.dockermanager.ui.screens.overlays

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.docker.ImageInspect
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.InfoRow
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.formatBytes
import com.fnos.dockermanager.ui.formatRfc3339Relative
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageDetailScreen(id: String, name: String) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var inspect by remember { mutableStateOf<ImageInspect?>(null) }
    var loading by remember { mutableStateOf(true) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        val client = app.client ?: return@LaunchedEffect
        loading = true
        runCatching { client.inspectImage(id) }
            .onSuccess { inspect = it }
            .onFailure { app.showMessage("加载失败：${it.message}") }
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        val c = inspect
        if (loading && c == null) {
            LoadingView(Modifier.fillMaxSize().padding(padding))
        } else if (c == null) {
            Text("无法加载镜像信息", modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AppCard {
                        Column {
                            Text(c.repoTags?.firstOrNull() ?: name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            InfoRow("镜像 ID", c.id.removePrefix("sha256:").take(12))
                            InfoRow("架构", "${c.os ?: "-"} / ${c.architecture ?: "-"}")
                            InfoRow("大小", formatBytes(c.size))
                            InfoRow("创建时间", formatRfc3339Relative(c.created))
                            InfoRow("Docker 版本", c.dockerVersion)
                            InfoRow("作者", c.author)
                            c.repoTags?.let { tags ->
                                if (tags.isNotEmpty()) {
                                    InfoRow("标签", tags.joinToString(", "))
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { app.showMessage("请在「容器」页点右下角运行容器，或到编排页使用此镜像") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("运行此镜像")
                        }
                        Button(
                            onClick = { deleteConfirm = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (c.config.env != null && c.config.env.isNotEmpty()) {
                    item {
                        AppCard {
                            Text("默认环境变量", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            c.config.env.take(40).forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 1) }
                            if (c.config.env.size > 40) {
                                Text("… 共 ${c.config.env.size} 项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
                if (c.config.exposedPorts.isNotEmpty()) {
                    item {
                        AppCard {
                            Text("暴露端口", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            c.config.exposedPorts.keys.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace) }
                        }
                    }
                }
                if (c.config.labels.isNotEmpty()) {
                    item {
                        AppCard {
                            Text("标签", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            c.config.labels.entries.take(30).forEach { (k, v) ->
                                Text("$k = $v", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(AppIcons.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = "删除镜像",
            text = "确定删除镜像「$name」吗？",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.deleteImage(id, force = true) }
                    deleteConfirm = false
                    app.popOverlay()
                }
            },
            onDismiss = { deleteConfirm = false },
        )
    }
}
