@file:OptIn(ExperimentalMaterial3Api::class)

package com.fnos.dockermanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.composefile.PortMapping
import com.fnos.dockermanager.docker.ContainerCreateRequest
import com.fnos.dockermanager.docker.ContainerHostConfig
import com.fnos.dockermanager.docker.ContainerSummary
import com.fnos.dockermanager.docker.PortBinding
import com.fnos.dockermanager.docker.RestartPolicy
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.components.PortMappingEditor
import com.fnos.dockermanager.ui.components.StateBadge
import com.fnos.dockermanager.ui.icons.AppIcons
import com.fnos.dockermanager.ui.formatRelativeTime
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlin.random.Random

@Composable
fun ContainersScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var containers by remember { mutableStateOf<List<ContainerSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ContainerSummary?>(null) }
    var showRunDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    suspend fun load() {
        val client = app.client ?: return
        loading = true
        error = null
        runCatching { client.listContainers(all = true) }
            .onSuccess { containers = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    LaunchedEffect(app.selectedServer?.id, refreshKey) {
        if (app.connected) load()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showRunDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("运行容器") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 搜索 + 刷新
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索容器名称 / 镜像") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { refreshKey++ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }

            error?.let {
                com.fnos.dockermanager.ui.components.ErrorBanner(
                    text = it,
                    onRetry = { refreshKey++ },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (loading && containers.isEmpty()) {
                LoadingView()
            } else {
                val filtered = containers.filter {
                    query.isBlank() ||
                        it.displayName.contains(query, ignoreCase = true) ||
                        it.image.contains(query, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    EmptyState(
                        icon = AppIcons.Container,
                        title = if (containers.isEmpty()) "暂无容器" else "没有匹配的容器",
                        subtitle = if (containers.isEmpty()) "点击右下角「运行容器」或到编排页创建项目" else "换个关键词试试",
                    )
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { container ->
                            ContainerCard(
                                container = container,
                                onClick = { app.pushOverlay(Overlay.ContainerDetail(container.id, container.displayName)) },
                                onDelete = { deleteTarget = container },
                                onStart = { scope.launch { runCatching { app.client?.startContainer(container.id) }; refreshKey++ } },
                                onStop = { scope.launch { runCatching { app.client?.stopContainer(container.id) }; refreshKey++ } },
                                onRestart = { scope.launch { runCatching { app.client?.restartContainer(container.id) }; refreshKey++ } },
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除容器",
            text = "确定删除容器「${target.displayName}」吗？${if (target.state == "running") "运行中的容器需要强制删除。" else ""}",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.removeContainer(target.id, force = target.state == "running") }
                    deleteTarget = null
                    refreshKey++
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showRunDialog) {
        RunContainerDialog(
            onDismiss = { showRunDialog = false },
            onCreate = { image, name, ports, envs, restart, privileged, memoryMb, start ->
                scope.launch {
                    runCatching {
                        val client = app.client ?: return@launch
                        val exposed = kotlinx.serialization.json.buildJsonObject {
                            ports.filter { it.isComplete }.forEach { p ->
                                put("/${p.containerPort}/${p.protocol}", JsonObject(emptyMap()))
                            }
                        }
                        val bindings = buildMap {
                            ports.filter { it.isComplete }.forEach { p ->
                                put("/${p.containerPort}/${p.protocol}", listOf(PortBinding(hostIp = p.hostIp, hostPort = p.hostPort)))
                            }
                        }
                        val resp = client.createContainer(
                            ContainerCreateRequest(
                                name = name.ifBlank { "container_${kotlin.math.abs(Random.nextLong())}" },
                                image = image,
                                env = envs.map { "${it.first}=${it.second}" },
                                tty = false,
                                exposedPorts = exposed,
                                hostConfig = ContainerHostConfig(
                                    portBindings = bindings,
                                    restartPolicy = when (restart) {
                                        "always" -> RestartPolicy(name = "always")
                                        "unless-stopped" -> RestartPolicy(name = "unless-stopped")
                                        else -> RestartPolicy(name = "no")
                                    },
                                    privileged = privileged,
                                    memory = (memoryMb.toLongOrNull() ?: 0L) * 1024 * 1024,
                                ),
                            )
                        )
                        if (start) runCatching { client.startContainer(resp.id) }
                    }.onFailure {
                        app.showMessage("创建失败：${it.message ?: ""}")
                    }
                    showRunDialog = false
                    refreshKey++
                }
            },
        )
    }
}

@Composable
private fun ContainerCard(
    container: ContainerSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(container.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    container.image,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StateBadge(container.state)
                    Spacer(Modifier.width(8.dp))
                    if (container.ports.isNotEmpty()) {
                        Text(
                            container.ports.joinToString(" ") { "${it.publicPort}:${it.privatePort}" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "创建于 ${formatRelativeTime(container.created)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (container.state != "running") {
                        DropdownMenuItem(
                            text = { Text("启动") },
                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                            onClick = { menuOpen = false; onStart() },
                        )
                    }
                    if (container.state == "running") {
                        DropdownMenuItem(
                            text = { Text("停止") },
                            leadingIcon = { Icon(Icons.Default.Close, contentDescription = null) },
                            onClick = { menuOpen = false; onStop() },
                        )
                        DropdownMenuItem(
                            text = { Text("重启") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                            onClick = { menuOpen = false; onRestart() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun RunContainerDialog(
    onDismiss: () -> Unit,
    onCreate: (image: String, name: String, ports: List<PortMapping>, envs: List<Pair<String, String>>, restart: String, privileged: Boolean, memoryMb: String, start: Boolean) -> Unit,
) {
    var image by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var restart by remember { mutableStateOf("no") }
    var privileged by remember { mutableStateOf(false) }
    var memoryMb by remember { mutableStateOf("") }
    var startNow by remember { mutableStateOf(true) }
    var ports by remember { mutableStateOf(listOf(PortMapping(hostPort = "", containerPort = "80"))) }
    var envs by remember { mutableStateOf(listOf(Pair("", ""))) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(600.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("运行新容器", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = image,
                onValueChange = { image = it },
                label = { Text("镜像（名称:标签）") },
                placeholder = { Text("nginx:latest") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("容器名称（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Text("端口映射", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            PortMappingEditor(
                ports = ports,
                onAdd = { ports = ports + PortMapping() },
                onRemove = { i -> ports = ports.filterIndexed { idx, _ -> idx != i } },
                onUpdate = { i, p -> ports = ports.mapIndexed { idx, old -> if (idx == i) p else old } },
            )
            Spacer(Modifier.height(6.dp))
            Text("环境变量", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            envs.forEachIndexed { i, pair ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pair.first,
                        onValueChange = { v -> envs = envs.mapIndexed { idx, p -> if (idx == i) p.copy(first = v) else p } },
                        placeholder = { Text("KEY") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    OutlinedTextField(
                        value = pair.second,
                        onValueChange = { v -> envs = envs.mapIndexed { idx, p -> if (idx == i) p.copy(second = v) else p } },
                        placeholder = { Text("value") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
            Button(onClick = { envs = envs + Pair("", "") }, modifier = Modifier.padding(top = 2.dp)) {
                Text("添加环境变量")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("重启策略：", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(6.dp))
                listOf("no" to "不重启", "always" to "总是", "unless-stopped" to "除非停止").forEach { (key, label) ->
                    androidx.compose.material3.FilterChip(
                        selected = restart == key,
                        onClick = { restart = key },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = privileged, onCheckedChange = { privileged = it })
                Spacer(Modifier.width(6.dp))
                Text("特权模式", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(16.dp))
                OutlinedTextField(
                    value = memoryMb,
                    onValueChange = { memoryMb = it.filter { c -> c.isDigit() } },
                    label = { Text("内存限制 MB（可选）") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = startNow, onCheckedChange = { startNow = it })
                Spacer(Modifier.width(6.dp))
                Text("创建后立即启动", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        if (image.isBlank()) return@Button
                        onCreate(
                            image.trim(), name.trim(), ports,
                            envs.filter { it.first.isNotBlank() },
                            restart, privileged, memoryMb, startNow,
                        )
                    },
                    enabled = image.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("创建")
                }
            }
        }
    }
}
