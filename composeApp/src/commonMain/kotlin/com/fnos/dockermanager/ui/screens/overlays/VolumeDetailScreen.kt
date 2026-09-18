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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.docker.VolumeInspect
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.InfoRow
import com.fnos.dockermanager.ui.components.LoadingView
import com.fnos.dockermanager.ui.formatRfc3339Relative
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeDetailScreen(name: String) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var volume by remember { mutableStateOf<VolumeInspect?>(null) }
    var loading by remember { mutableStateOf(true) }
    var deleteConfirm by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(refreshKey) {
        val client = app.client ?: return@LaunchedEffect
        loading = true
        runCatching {
            client.listVolumes().firstOrNull { it.name == name }
        }.onSuccess { volume = it }
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
        val v = volume
        if (loading && v == null) {
            LoadingView(Modifier.fillMaxSize().padding(padding))
        } else if (v == null) {
            Text("无法加载卷信息", Modifier.padding(padding).padding(16.dp))
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                AppCard {
                    Column {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(v.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        InfoRow("驱动", v.driver)
                        InfoRow("挂载点", v.mountpoint)
                        InfoRow("作用域", v.scope)
                        InfoRow("创建时间", formatRfc3339Relative(v.createdAt))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { deleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("删除此卷")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "提示：删除卷会永久清除其中数据。可在编排页的服务「存储挂载」中引用此卷。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    if (deleteConfirm) {
        ConfirmDialog(
            title = "删除卷",
            text = "确定删除卷「$name」吗？数据不可恢复。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                scope.launch {
                    runCatching { app.client?.deleteVolume(name, force = true) }
                    deleteConfirm = false
                    app.popOverlay()
                }
            },
            onDismiss = { deleteConfirm = false },
        )
    }
}
