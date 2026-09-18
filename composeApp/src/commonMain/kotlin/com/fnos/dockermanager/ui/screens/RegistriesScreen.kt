package com.fnos.dockermanager.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.connection.RegistryCredential
import com.fnos.dockermanager.docker.ImageSearchItem
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.components.ErrorBanner
import com.fnos.dockermanager.ui.components.TagChip
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistriesScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RegistryCredential?>(null) }
    var deleteTarget by remember { mutableStateOf<RegistryCredential?>(null) }

    // 搜索
    var searchTerm by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ImageSearchItem>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchRegistryExpanded by remember { mutableStateOf(false) }
    val registries = app.selectedServer?.registryAuths?.values.orEmpty()
    var searchRegistry by remember {
        mutableStateOf(RegistryCredential(serverAddress = "docker.io"))
    }

    fun doSearch() {
        if (searchTerm.isBlank()) return
        scope.launch {
            val client = app.client ?: return@launch
            searching = true
            searchError = null
            runCatching {
                client.searchImages(searchTerm.trim(), searchRegistry.takeIf { it.username.isNotBlank() })
            }.onSuccess { searchResults = it }
                .onFailure { searchError = it.message ?: "搜索失败" }
            searching = false
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("登录仓库") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 已登录仓库
            Text(
                "已配置仓库（用于拉取私有镜像）",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (registries.isEmpty()) {
                Text(
                    "尚未配置私有仓库。登录后可拉取私有镜像（如 registry.example.com:5000/myapp）。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(registries.toList(), key = { it.serverAddress }) { reg ->
                        AppCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(AppIcons.Registry, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(reg.displayServer, fontWeight = FontWeight.Medium)
                                    Text("用户：${reg.username.ifBlank { "匿名" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                                IconButton(onClick = { editing = reg }) {
                                    Icon(Icons.Default.Person, contentDescription = "编辑")
                                }
                                IconButton(onClick = { deleteTarget = reg }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            // 搜索
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("搜索镜像", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchTerm,
                        onValueChange = { searchTerm = it },
                        placeholder = { Text("nginx / portainer") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { doSearch() }, enabled = searchTerm.isNotBlank() && !searching) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("搜索")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("在仓库：", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { searchRegistryExpanded = true }) {
                        Text(searchRegistry.displayServer, maxLines = 1)
                    }
                    DropdownMenu(expanded = searchRegistryExpanded, onDismissRequest = { searchRegistryExpanded = false }) {
                        (listOf(RegistryCredential(serverAddress = "docker.io")) + registries.toList())
                            .distinctBy { it.serverAddress }
                            .forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.displayServer, maxLines = 1) },
                                    onClick = {
                                        searchRegistry = r
                                        searchRegistryExpanded = false
                                    },
                                )
                            }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            when {
                searching -> CircularProgressIndicator(Modifier.padding(20.dp))
                searchError != null -> ErrorBanner(searchError ?: "", Modifier.padding(horizontal = 16.dp), onRetry = { doSearch() })
                searchResults != null && searchResults!!.isEmpty() -> EmptyState(
                    AppIcons.Registry, "没有找到镜像", "换个关键词试试",
                    Modifier.padding(vertical = 20.dp),
                )
                searchResults != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(searchResults!!, key = { it.name }) { item ->
                        AppCard(onClick = {
                            app.pushOverlay(Overlay.PullImage(defaultTerm = item.name))
                        }) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (item.isOfficial) TagChip("官方")
                                }
                                if (item.description.isNotBlank()) {
                                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 2)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("⭐ ${item.starCount} · 点击拉取", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
                else -> Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showAddDialog || editing != null) {
        RegistryLoginDialog(
            existing = editing,
            onDismiss = { showAddDialog = false; editing = null },
            onSave = { reg ->
                scope.launch {
                    val client = app.client ?: return@launch
                    runCatching { client.registryLogin(reg) }
                        .onSuccess {
                            val server = app.selectedServer ?: return@onSuccess
                            val auths = server.registryAuths.toMutableMap()
                            auths[reg.serverAddress] = reg
                            app.saveServer(server.copy(registryAuths = auths))
                            app.showMessage("仓库 ${reg.displayServer} 登录成功")
                        }
                        .onFailure { app.showMessage("登录失败：${it.message}") }
                    showAddDialog = false
                    editing = null
                }
            },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "移除仓库",
            text = "确定移除仓库「${target.displayServer}」的登录凭据吗？",
            confirmLabel = "移除",
            destructive = true,
            onConfirm = {
                val server = app.selectedServer
                if (server != null) {
                    val auths = server.registryAuths.toMutableMap()
                    auths.remove(target.serverAddress)
                    app.saveServer(server.copy(registryAuths = auths))
                }
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun RegistryLoginDialog(
    existing: RegistryCredential?,
    onDismiss: () -> Unit,
    onSave: (RegistryCredential) -> Unit,
) {
    var serverAddress by remember { mutableStateOf(existing?.serverAddress ?: "registry.example.com:5000") }
    var username by remember { mutableStateOf(existing?.username ?: "") }
    var password by remember { mutableStateOf(existing?.password ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(if (existing == null) "登录私有仓库" else "编辑仓库凭据", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = serverAddress,
                onValueChange = { serverAddress = it.trim() },
                label = { Text("仓库地址") },
                placeholder = { Text("registry.example.com:5000") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("用户名") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码 / 访问令牌") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text("Docker Hub 登录可留空仓库地址（默认 docker.io）", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = {
                        if (serverAddress.isBlank() || username.isBlank()) return@Button
                        onSave(
                            RegistryCredential(
                                serverAddress = serverAddress,
                                username = username.trim(),
                                password = password,
                            )
                        )
                    },
                    enabled = serverAddress.isNotBlank() && username.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("登录")
                }
            }
        }
    }
}
