package com.fnos.dockermanager.ui.screens.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.docker.ContainerInspect
import com.fnos.dockermanager.docker.ContainerStats
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.InfoRow
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.components.SectionTitle
import com.fnos.dockermanager.ui.components.StateBadge
import com.fnos.dockermanager.ui.components.TagChip
import com.fnos.dockermanager.ui.formatDecimal
import com.fnos.dockermanager.ui.cpuPercent
import com.fnos.dockermanager.ui.formatBytes
import com.fnos.dockermanager.ui.formatRfc3339Relative
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(id: String, name: String) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var inspect by remember { mutableStateOf<ContainerInspect?>(null) }
    var stats by remember { mutableStateOf<ContainerStats?>(null) }
    var prevStats by remember { mutableStateOf<ContainerStats?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    suspend fun reload() {
        val client = app.client ?: return
        loading = true
        error = null
        runCatching { client.inspectContainer(id) }
            .onSuccess { inspect = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    LaunchedEffect(refreshKey) { reload() }

    // 统计轮询
    LaunchedEffect(id) {
        while (true) {
            val client = app.client ?: break
            runCatching { client.containerStats(id) }.onSuccess {
                prevStats = stats
                stats = it
            }
            delay(3000)
        }
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
        if (loading && inspect == null) {
            LoadingView(Modifier.fillMaxSize().padding(padding))
        } else {
            val c = inspect
            if (c == null) {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    com.fnos.dockermanager.ui.components.ErrorBanner(error ?: "无法加载", onRetry = { refreshKey++ })
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        // 状态 + 操作
                        AppCard {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(c.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    StateBadge(c.state.status.ifBlank { c.state.running.toString() })
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!c.state.running) {
                                        Button(onClick = { scope.launch { runCatching { app.client?.startContainer(id) }; refreshKey++ } }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("启动")
                                        }
                                    }
                                    if (c.state.running && !c.state.paused) {
                                        OutlinedButton(onClick = { scope.launch { runCatching { app.client?.stopContainer(id) }; refreshKey++ } }, modifier = Modifier.weight(1f)) {
                                            Text("停止")
                                        }
                                        OutlinedButton(onClick = { scope.launch { runCatching { app.client?.restartContainer(id) }; refreshKey++ } }, modifier = Modifier.weight(1f)) {
                                            Text("重启")
                                        }
                                    }
                                    if (c.state.running && !c.state.paused) {
                                        OutlinedButton(onClick = { scope.launch { runCatching { app.client?.pauseContainer(id) }; refreshKey++ } }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    } else if (c.state.paused) {
                                        OutlinedButton(onClick = { scope.launch { runCatching { app.client?.unpauseContainer(id) }; refreshKey++ } }, modifier = Modifier.weight(1f)) {
                                            Text("取消暂停")
                                        }
                                    }
                                    OutlinedButton(onClick = { deleteConfirm = true }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { app.pushOverlay(Overlay.LogsViewer(id, c.displayName)) }, modifier = Modifier.weight(1f)) {
                                        Icon(AppIcons.Log, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("查看日志")
                                    }
                                }
                            }
                        }
                    }

                    // 实时统计
                    stats?.let { s ->
                        item {
                            AppCard {
                                Column {
                                    Text("实时统计（每 3 秒刷新）", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(10.dp))
                                    val cpu = cpuPercent(s, prevStats)
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("CPU", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                                        LinearProgressIndicator(
                                            progress = { (cpu / 100f).toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier.weight(1f).height(6.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("${formatDecimal(cpu, 1)}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val memUsage = s.memoryStats.usage
                                    val memLimit = s.memoryStats.limit
                                    val memPct = if (memLimit > 0) memUsage.toFloat() / memLimit else 0f
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text("内存", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(48.dp))
                                        LinearProgressIndicator(
                                            progress = { memPct.coerceIn(0f, 1f) },
                                            modifier = Modifier.weight(1f).height(6.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("${formatBytes(memUsage)} / ${formatBytes(memLimit)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val netRx = s.networks.values.sumOf { it.rxBytes }
                                    val netTx = s.networks.values.sumOf { it.txBytes }
                                    InfoRow("网络", "↓ ${formatBytes(netRx)}  ↑ ${formatBytes(netTx)}")
                                }
                            }
                        }
                    }

                    item {
                        AppCard {
                            Column {
                                Text("详情", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                InfoRow("镜像", c.config.image)
                                InfoRow("容器 ID", c.id.take(12))
                                InfoRow("状态", c.state.status + if (c.state.exitCode != 0) " (退出码 ${c.state.exitCode})" else "")
                                InfoRow("健康", c.state.health?.status ?: "-")
                                InfoRow("创建时间", formatRfc3339Relative(c.created))
                                InfoRow("启动时间", formatRfc3339Relative(c.state.startedAt))
                                InfoRow("重启次数", c.restartCount.toString())
                                InfoRow("用户", c.config.user ?: "-")
                                c.networkSettings.networks.entries.firstOrNull()?.let { (netName, ep) ->
                                    InfoRow("网络", "$netName ${ep.ipAddress.ifBlank { "" }}")
                                }
                            }
                        }
                    }

                    if (c.config.cmd != null) {
                        item {
                            AppCard {
                                Text("命令", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                c.config.cmd.forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
                            }
                        }
                    }

                    if (c.config.env != null) {
                        item {
                            AppCard {
                                Text("环境变量", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                c.config.env.take(30).forEach { Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, maxLines = 1) }
                                if ((c.config.env?.size ?: 0) > 30) {
                                    Text("… 共 ${c.config.env?.size} 项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }

                    if (c.mounts.isNotEmpty()) {
                        item {
                            AppCard {
                                Text("挂载", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                c.mounts.forEach { m ->
                                    InfoRow(m.destination, m.source)
                                }
                            }
                        }
                    }

                    if (c.config.labels.isNotEmpty()) {
                        item {
                            AppCard {
                                Text("标签", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(6.dp))
                                c.config.labels.entries.take(20).forEach { (k, v) ->
                                    Text("$k = $v", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(30.dp)) }
                }
            }
        }
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = "删除容器",
            text = "确定删除容器「$name」吗？${if (inspect?.state?.running == true) "容器正在运行，将强制删除。" else ""}",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.removeContainer(id, force = inspect?.state?.running == true) }
                    deleteConfirm = false
                    app.popOverlay()
                }
            },
            onDismiss = { deleteConfirm = false },
        )
    }
}
