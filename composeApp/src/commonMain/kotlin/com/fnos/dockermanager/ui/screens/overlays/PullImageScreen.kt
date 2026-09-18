package com.fnos.dockermanager.ui.screens.overlays

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.connection.RegistryCredential
import com.fnos.dockermanager.docker.ImageSearchItem
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullImageScreen(defaultTerm: String = "") {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var term by remember { mutableStateOf(defaultTerm) }
    var tag by remember { mutableStateOf("latest") }
    var registryExpanded by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ImageSearchItem>?>(null) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // 拉取进度
    var pulling by remember { mutableStateOf(false) }
    var pullError by remember { mutableStateOf<String?>(null) }
    var pullDone by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableStateOf<Float?>(null) }
    val pullLogs = remember { mutableStateListOf<String>() }

    val registries = remember {
        buildList {
            add(RegistryCredential(serverAddress = "docker.io", username = "", password = ""))
            addAll(app.selectedServer?.registryAuths?.values.orEmpty().filter { it.serverAddress != "docker.io" })
        }
    }
    var selectedRegistry by remember {
        mutableStateOf(registries.firstOrNull() ?: RegistryCredential(serverAddress = "docker.io"))
    }

    fun pullImage(fromImage: String, imageTag: String) {
        scope.launch {
            val client = app.client ?: return@launch
            pulling = true
            pullError = null
            pullDone = false
            progressFraction = null
            pullLogs.clear()
            try {
                client.pullImage(
                    fromImage = fromImage,
                    tag = imageTag,
                    registryAuth = selectedRegistry.takeIf { it.username.isNotBlank() },
                ).collect { ev ->
                    if (ev.error != null) {
                        pullError = ev.error
                        pullLogs.add("错误: ${ev.error}")
                    }
                    ev.id?.let { id ->
                        val line = ev.status?.let { "[$id] $it" } ?: ""
                        if (line.isNotEmpty()) {
                            if (pullLogs.size > 60) pullLogs.removeAt(0)
                            pullLogs.add(line)
                        }
                    } ?: ev.status?.let { status ->
                        if (pullLogs.size > 60) pullLogs.removeAt(0)
                        pullLogs.add(status)
                    }
                    val detail = ev.progressDetail
                    val current = detail?.get("current")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    val total = detail?.get("total")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                    if (current != null && total != null && total > 0) {
                        progressFraction = (current.toFloat() / total).coerceIn(0f, 1f)
                    }
                }
                pullDone = true
                pullLogs.add("✅ 镜像拉取完成")
                app.showMessage("镜像 $fromImage:$imageTag 拉取完成")
            } catch (e: Exception) {
                pullError = e.message ?: "拉取失败"
                pullLogs.add("失败: ${e.message ?: ""}")
            } finally {
                pulling = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("拉取镜像", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = term,
                            onValueChange = { term = it },
                            label = { Text("镜像名称") },
                            placeholder = { Text("nginx / portainer/portainer-ce") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = tag,
                            onValueChange = { tag = it },
                            label = { Text("标签") },
                            placeholder = { Text("latest") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("仓库：", style = MaterialTheme.typography.bodySmall)
                        Box {
                            OutlinedButton(onClick = { registryExpanded = true }) {
                                Text(selectedRegistry.displayServer, maxLines = 1)
                            }
                            DropdownMenu(expanded = registryExpanded, onDismissRequest = { registryExpanded = false }) {
                                registries.forEach { r ->
                                    DropdownMenuItem(
                                        text = { Text(r.displayServer) },
                                        onClick = {
                                            selectedRegistry = r
                                            registryExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Button(onClick = { pullImage(term.trim(), tag.trim().ifBlank { "latest" }) }, enabled = term.isNotBlank() && !pulling, modifier = Modifier.weight(1f)) {
                            Text("直接拉取")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(onClick = {
                        scope.launch {
                            val client = app.client ?: return@launch
                            searching = true
                            searchError = null
                            runCatching {
                                client.searchImages(term.trim(), selectedRegistry.takeIf { it.username.isNotBlank() })
                            }.onSuccess { results = it }
                                .onFailure { searchError = it.message ?: "搜索失败" }
                            searching = false
                        }
                    }, enabled = term.isNotBlank() && !pulling) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("搜索仓库")
                    }
                }
            }

            // 拉取进度
            if (pulling || pullDone || pullError != null) {
                item {
                    AppCard {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (pulling) {
                                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("正在拉取 ${term.trim()}:${tag.trim().ifBlank { "latest" }} …", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                                } else if (pullDone) {
                                    Text("拉取完成", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("拉取失败", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            progressFraction?.let { f ->
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { f }, modifier = Modifier.fillMaxWidth().height(5.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            pullLogs.takeLast(15).forEach { line ->
                                Text(line, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 搜索结果
            when {
                searching -> item { CircularProgressIndicator(Modifier.padding(20.dp)) }
                searchError != null -> item { com.fnos.dockermanager.ui.components.ErrorBanner(searchError ?: "", onRetry = null) }
                results != null && results!!.isEmpty() -> item {
                    EmptyState(AppIcons.Registry, "没有找到镜像", "试试更短的关键词")
                }
                results != null -> {
                    item { Text("搜索结果（来自 ${selectedRegistry.displayServer}）", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall) }
                    items(results!!, key = { it.name }) { item ->
                        AppCard(onClick = { pullImage(item.name, tag.trim().ifBlank { "latest" }) }) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    if (item.isOfficial) {
                                        com.fnos.dockermanager.ui.components.TagChip("官方")
                                    }
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
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }
}
