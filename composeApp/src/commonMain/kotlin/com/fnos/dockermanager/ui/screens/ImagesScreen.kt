package com.fnos.dockermanager.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.docker.ImageSummary
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.formatBytes
import com.fnos.dockermanager.ui.formatRelativeTime
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var images by remember { mutableStateOf<List<ImageSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<ImageSummary?>(null) }
    var refreshKey by remember { mutableStateOf(0) }

    suspend fun load() {
        val client = app.client ?: return
        loading = true
        error = null
        runCatching { client.listImages() }
            .onSuccess { images = it }
            .onFailure { error = it.message ?: "加载失败" }
        loading = false
    }

    LaunchedEffect(app.selectedServer?.id, refreshKey) {
        if (app.connected) load()
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { app.pushOverlay(Overlay.PullImage()) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("拉取镜像") },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索镜像名称") },
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

            if (loading && images.isEmpty()) {
                LoadingView()
            } else {
                val filtered = images.filter {
                    query.isBlank() ||
                        it.displayName.contains(query, ignoreCase = true) ||
                        it.tags.any { t -> t.contains(query, ignoreCase = true) }
                }
                if (filtered.isEmpty()) {
                    EmptyState(
                        icon = AppIcons.Image,
                        title = if (images.isEmpty()) "暂无本地镜像" else "没有匹配的镜像",
                        subtitle = if (images.isEmpty()) "点击右下角「拉取镜像」从仓库拉取" else "换个关键词试试",
                    )
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filtered, key = { it.id }) { image ->
                            ImageCard(
                                image = image,
                                onClick = { app.pushOverlay(Overlay.ImageDetail(image.id, image.displayName)) },
                                onDelete = { deleteTarget = image },
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除镜像",
            text = "确定删除镜像「${target.displayName}」吗？",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.deleteImage(target.id, force = true) }
                    deleteTarget = null
                    refreshKey++
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
private fun ImageCard(
    image: ImageSummary,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                AppIcons.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(image.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Row {
                    image.tags.take(2).forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatBytes(image.size)} · ${formatRelativeTime(image.created)} · 被 ${image.containers} 个容器使用",
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
