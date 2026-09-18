package com.fnos.dockermanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.docker.NetworkInspect
import com.fnos.dockermanager.docker.NetworkCreateRequest
import com.fnos.dockermanager.docker.Ipam
import com.fnos.dockermanager.docker.IpamConfig
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.formatRfc3339Relative
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworksScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var networks by remember { mutableStateOf<List<NetworkInspect>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<NetworkInspect?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    suspend fun load() {
        val client = app.client ?: return
        loading = true
        error = null
        runCatching { client.listNetworks() }
            .onSuccess { networks = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    LaunchedEffect(app.selectedServer?.id, refreshKey) {
        if (app.connected) load()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("新建网络") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "共 ${networks.size} 个网络",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
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
            if (loading && networks.isEmpty()) {
                LoadingView()
            } else if (networks.isEmpty()) {
                EmptyState(AppIcons.Network, "暂无自定义网络", "点击右下角新建 bridge 网络，用于编排中容器互联")
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(networks, key = { it.id }) { network ->
                        NetworkCard(
                            network = network,
                            onClick = { app.pushOverlay(Overlay.NetworkDetail(network.id, network.name)) },
                            onDelete = { deleteTarget = network },
                        )
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除网络",
            text = "确定删除网络「${target.name}」吗？${if (target.containers.isNotEmpty()) "该网络仍有 ${target.containers.size} 个容器连接。" else ""}",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.deleteNetwork(target.id) }
                    deleteTarget = null
                    refreshKey++
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }

    if (showCreate) {
        NetworkCreateDialog(
            onDismiss = { showCreate = false },
            onCreate = { name, driver, subnet, gateway, internal, attachable ->
                scope.launch {
                    runCatching {
                        app.client?.createNetwork(
                            NetworkCreateRequest(
                                name = name,
                                driver = driver,
                                internal = internal,
                                attachable = attachable,
                                ipam = Ipam(
                                    config = if (subnet.isNotBlank()) {
                                        listOf(IpamConfig(subnet = subnet, gateway = gateway.ifBlank { null }))
                                    } else emptyList(),
                                ),
                            )
                        )
                    }.onSuccess {
                        app.showMessage("网络 $name 创建成功")
                    }.onFailure {
                        app.showMessage("创建失败：${it.message}")
                    }
                    showCreate = false
                    refreshKey++
                }
            },
        )
    }
}

@Composable
private fun NetworkCard(
    network: NetworkInspect,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Network, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(network.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(network.driver)
                        network.ipam.config.firstOrNull()?.subnet?.let { append(" · ").append(it) }
                        append(" · ").append(network.containers.size).append(" 容器")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
                Text(
                    "创建于 ${formatRfc3339Relative(network.created)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "操作")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
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
private fun NetworkCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, driver: String, subnet: String, gateway: String, internal: Boolean, attachable: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var driver by remember { mutableStateOf("bridge") }
    var subnet by remember { mutableStateOf("") }
    var gateway by remember { mutableStateOf("") }
    var internal by remember { mutableStateOf(false) }
    var attachable by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text("新建网络", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("网络名称") },
                placeholder = { Text("my-net") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("驱动", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Row(Modifier.padding(top = 4.dp)) {
                listOf("bridge", "macvlan", "ipvlan", "overlay").forEach { d ->
                    androidx.compose.material3.FilterChip(
                        selected = driver == d,
                        onClick = { driver = d },
                        label = { Text(d) },
                        modifier = Modifier.padding(end = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = subnet,
                onValueChange = { subnet = it },
                label = { Text("子网（可选）") },
                placeholder = { Text("172.20.0.0/16") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = gateway,
                onValueChange = { gateway = it },
                label = { Text("网关（可选）") },
                placeholder = { Text("172.20.0.1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = internal, onCheckedChange = { internal = it })
                Spacer(Modifier.width(8.dp))
                Text("内部网络（禁止外网访问）", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = attachable, onCheckedChange = { attachable = it })
                Spacer(Modifier.width(8.dp))
                Text("允许手动连接容器", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = { if (name.isNotBlank()) onCreate(name.trim(), driver, subnet.trim(), gateway.trim(), internal, attachable) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("创建")
                }
            }
        }
    }
}
