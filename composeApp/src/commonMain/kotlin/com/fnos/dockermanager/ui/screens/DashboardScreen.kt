package com.fnos.dockermanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.app.Tab
import com.fnos.dockermanager.docker.ContainerSummary
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ErrorBanner
import com.fnos.dockermanager.ui.components.StateBadge
import com.fnos.dockermanager.ui.formatBytes
import com.fnos.dockermanager.ui.icons.AppIcons
import com.fnos.dockermanager.ui.formatRelativeTime

data class DashboardData(
    val containers: List<ContainerSummary> = emptyList(),
    val imageCount: Long = 0,
    val networkCount: Long = 0,
    val volumeCount: Long = 0,
)

@Composable
fun DashboardScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current
    var data by remember { mutableStateOf<DashboardData?>(null) }
    var loading by remember { mutableStateOf(false) }

    suspend fun load() {
        val client = app.client ?: return
        loading = true
        val containers = runCatching { client.listContainers(all = true) }.getOrDefault(emptyList())
        val images = runCatching { client.listImages() }.getOrDefault(emptyList())
        val networks = runCatching { client.listNetworks() }.getOrDefault(emptyList())
        val volumes = runCatching { client.listVolumes() }.getOrDefault(emptyList())
        data = DashboardData(
            containers = containers,
            imageCount = images.size.toLong(),
            networkCount = networks.size.toLong(),
            volumeCount = volumes.size.toLong(),
        )
        loading = false
    }

    LaunchedEffect(app.selectedServer?.id) {
        if (app.connected) load()
    }
    LaunchedEffect(app.connected) {
        if (app.connected) load()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 连接状态
        item {
            when {
                app.connecting -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("正在连接 ${app.selectedServer?.name ?: ""} …", color = MaterialTheme.colorScheme.outline)
                }

                app.lastError != null -> ErrorBanner(
                    text = app.lastError ?: "",
                    onRetry = { app.connect() },
                )

                else -> Unit
            }
        }

        // 系统信息
        app.systemInfo?.let { info ->
            item {
                AppCard {
                    Column {
                        Text("服务器信息", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            InfoCell("Docker", info.serverVersion ?: "-", Modifier.weight(1f))
                            InfoCell("系统", info.operatingSystem?.substringBefore(' ') ?: "-", Modifier.weight(1f))
                            InfoCell("架构", info.architecture ?: "-", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            InfoCell("CPU", "${info.ncpu} 核", Modifier.weight(1f))
                            InfoCell("内存", formatBytes(info.memTotal), Modifier.weight(1f))
                            InfoCell("驱动", info.driver ?: "-", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 统计卡片
        item {
            val d = data
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "容器",
                    value = if (d == null) "…" else "${d.containers.count { it.state == "running" }}/${d.containers.size}",
                    icon = AppIcons.Container,
                    onClick = { app.switchTab(Tab.Containers) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "镜像",
                    value = d?.imageCount?.toString() ?: "…",
                    icon = AppIcons.Image,
                    onClick = { app.switchTab(Tab.Images) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    label = "网络",
                    value = d?.networkCount?.toString() ?: "…",
                    icon = AppIcons.Network,
                    onClick = { app.switchTab(Tab.Networks) },
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "存储卷",
                    value = d?.volumeCount?.toString() ?: "…",
                    icon = AppIcons.Volume,
                    onClick = { app.pushOverlay(Overlay.VolumeList) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 快捷操作
        item {
            AppCard {
                Text("快捷操作", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                QuickActionRow(
                    actions = listOf(
                        QuickAction("拉取镜像", AppIcons.Image) { app.pushOverlay(Overlay.PullImage()) },
                        QuickAction("创建编排", AppIcons.Compose) { app.switchTab(Tab.Compose) },
                        QuickAction("仓库登录", AppIcons.Registry) { app.switchTab(Tab.Registries) },
                        QuickAction("新建网络", AppIcons.Network) { app.switchTab(Tab.Networks) },
                    ),
                )
            }
        }

        // 容器概览
        val containers = data?.containers.orEmpty()
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("容器概览", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    "查看全部 →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { app.switchTab(Tab.Containers) },
                )
            }
        }
        if (containers.isEmpty() && data != null) {
            item {
                Text(
                    "暂无容器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        items(containers.take(5), key = { it.id }) { container ->
            AppCard(onClick = { app.pushOverlay(Overlay.ContainerDetail(container.id, container.displayName)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(container.displayName, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(
                            container.image,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    StateBadge(container.state)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun QuickActionRow(actions: List<QuickAction>) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            Column(
                Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .clickable(onClick = action.onClick)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(action.label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
