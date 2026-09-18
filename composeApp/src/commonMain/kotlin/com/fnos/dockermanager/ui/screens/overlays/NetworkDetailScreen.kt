package com.fnos.dockermanager.ui.screens.overlays

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.fnos.dockermanager.docker.ContainerSummary
import com.fnos.dockermanager.docker.NetworkInspect
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.InfoRow
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.formatRfc3339Relative
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDetailScreen(id: String, name: String) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var network by remember { mutableStateOf<NetworkInspect?>(null) }
    var containers by remember { mutableStateOf<List<ContainerSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var connectTarget by remember { mutableStateOf<ContainerSummary?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    suspend fun reload() {
        val client = app.client ?: return
        loading = true
        runCatching {
            network = client.inspectNetwork(id)
            containers = client.listContainers(all = true)
        }
        loading = false
    }

    LaunchedEffect(refreshKey) { reload() }

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
        val n = network
        if (loading && n == null) {
            LoadingView(Modifier.fillMaxSize().padding(padding))
        } else if (n == null) {
            Text("无法加载网络信息", Modifier.padding(padding).padding(16.dp))
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    AppCard {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(n.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(6.dp))
                            InfoRow("驱动", n.driver)
                            InfoRow("作用域", n.scope)
                            InfoRow("子网", n.ipam.config.firstOrNull()?.subnet ?: "-")
                            InfoRow("网关", n.ipam.config.firstOrNull()?.gateway ?: "-")
                            InfoRow("内部网络", if (n.internal) "是" else "否")
                            InfoRow("IPv6", if (n.enableIpv6) "是" else "否")
                            InfoRow("创建时间", formatRfc3339Relative(n.created))
                            InfoRow("网络 ID", n.id.take(12))
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { app.showMessage("请先在容器详情中查看网络，或在创建容器时选择网络") },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(AppIcons.Container, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("连接容器")
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { deleteConfirm = true },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Spacer(Modifier.width(4.dp))
                                    Text("删除网络")
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "已连接容器（${n.containers.size}）",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
                if (n.containers.isEmpty()) {
                    item {
                        Text(
                            "暂无容器连接。可在创建容器时选择此网络。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                } else {
                    items(n.containers.entries.toList(), key = { it.key }) { (containerId, info) ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Container, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(info.name.ifBlank { containerId.take(12) }, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text(info.ipv4Address.ifBlank { "-" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        runCatching { app.client?.disconnectContainerFromNetwork(id, containerId, force = true) }
                                        refreshKey++
                                    }
                                }) {
                                    Icon(Icons.Default.Link, contentDescription = "断开", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(30.dp)) }
            }
        }
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = "删除网络",
            text = "确定删除网络「$name」吗？",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.deleteNetwork(id) }
                    deleteConfirm = false
                    app.popOverlay()
                }
            },
            onDismiss = { deleteConfirm = false },
        )
    }
}
