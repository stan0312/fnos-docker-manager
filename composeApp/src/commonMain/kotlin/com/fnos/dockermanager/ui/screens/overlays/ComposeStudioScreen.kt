package com.fnos.dockermanager.ui.screens.overlays

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.composefile.ComposeYamlParser
import com.fnos.dockermanager.composefile.NamedVolumeSpec
import com.fnos.dockermanager.composefile.NetworkSpec
import com.fnos.dockermanager.composefile.ServiceSpec
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.ConfirmDialog
import com.fnos.dockermanager.ui.components.EmptyState
import com.fnos.dockermanager.ui.components.SectionTitle
import com.fnos.dockermanager.ui.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeStudioScreen(modifier: Modifier = Modifier) {
    val app = LocalAppState.current

    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var clearConfirm by remember { mutableStateOf(false) }
    var deployError by remember { mutableStateOf<String?>(null) }

    // 网络/卷编辑弹窗
    var editNetwork by remember { mutableStateOf<NetworkSpec?>(null) }
    var showNetworkDialog by remember { mutableStateOf(false) }
    var editVolume by remember { mutableStateOf<NamedVolumeSpec?>(null) }
    var showVolumeDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "可视化创建 docker-compose 项目，编辑完成后一键部署到飞牛",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // 项目名称
        item {
            OutlinedTextField(
                value = app.projectName,
                onValueChange = { app.projectName = it },
                label = { Text("项目名称") },
                placeholder = { Text("例如 media-server") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // 操作行
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导入 YAML")
                }
                OutlinedButton(onClick = {
                    if (app.composeServices.isEmpty()) {
                        deployError = "请先添加至少一个服务"
                    } else {
                        deployError = null
                        app.pushOverlay(Overlay.YamlPreview())
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Preview, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("预览 YAML")
                }
                OutlinedButton(onClick = { clearConfirm = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("清空")
                }
            }
        }

        deployError?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }

        // 服务
        item {
            SectionTitle(
                "服务（${app.composeServices.size}）",
                action = {
                    TextButton(onClick = { app.pushOverlay(Overlay.ServiceEdit(-1)) }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("添加服务")
                    }
                },
            )
        }
        if (app.composeServices.isEmpty()) {
            item {
                EmptyState(AppIcons.Compose, "还没有服务", "点击「添加服务」，可视化配置镜像、端口、存储、环境变量等")
            }
        } else {
            itemsIndexed(app.composeServices) { index, service ->
                ServiceCard(
                    service = service,
                    onClick = { app.pushOverlay(Overlay.ServiceEdit(index)) },
                    onDelete = {
                        app.composeServices.removeAt(index)
                        app.composeServices.forEach { s ->
                            s.dependsOn.remove(service.name)
                        }
                    },
                )
            }
        }

        // 网络
        item {
            SectionTitle(
                "网络（${app.composeNetworks.size}）",
                action = {
                    TextButton(onClick = { editNetwork = null; showNetworkDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("添加网络")
                    }
                },
            )
        }
        if (app.composeNetworks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    app.composeNetworks.forEachIndexed { index, network ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editNetwork = network; showNetworkDialog = true }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(AppIcons.Network, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(network.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${network.driver}${if (network.subnet.isNotBlank()) " · ${network.subnet}" else ""}${if (network.external) " · 外部" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            IconButton(onClick = { app.composeNetworks.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 卷
        item {
            SectionTitle(
                "卷（${app.composeVolumes.size}）",
                action = {
                    TextButton(onClick = { editVolume = null; showVolumeDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("添加卷")
                    }
                },
            )
        }
        if (app.composeVolumes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    app.composeVolumes.forEachIndexed { index, volume ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { editVolume = volume; showVolumeDialog = true }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(AppIcons.Volume, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${volume.name}${if (volume.external) "（外部卷）" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { app.composeVolumes.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // 部署
        item {
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val errors = app.buildComposeProject().validate()
                    if (errors.isNotEmpty()) {
                        deployError = errors.joinToString("\n")
                    } else {
                        deployError = null
                        app.pushOverlay(Overlay.DeployProgress(startAfterCreate = true))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("部署到服务器", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "部署将按顺序执行：创建卷 → 创建网络 → 拉取镜像 → 按依赖顺序创建容器 → 启动",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    // 导入 YAML
    if (showImport) {
        Dialog(onDismissRequest = { showImport = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(20.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .padding(16.dp),
            ) {
                Text("导入 docker-compose YAML", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it; importError = null },
                    placeholder = { Text("services:\n  web:\n    image: nginx:latest\n    ports:\n      - \"8080:80\"") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                importError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    TextButton(onClick = { showImport = false }, modifier = Modifier.weight(1f)) { Text("取消") }
                    Button(
                        onClick = {
                            runCatching { ComposeYamlParser.parse(importText) }
                                .onSuccess {
                                    app.loadComposeProject(it)
                                    showImport = false
                                    importText = ""
                                    app.showMessage("导入成功，共 ${it.services.size} 个服务")
                                }
                                .onFailure { importError = "解析失败：${it.message ?: ""}" }
                        },
                        enabled = importText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("导入")
                    }
                }
            }
        }
    }

    // 清空确认
    if (clearConfirm) {
        ConfirmDialog(
            title = "清空编排",
            text = "确定清空当前项目（服务、网络、卷）吗？此操作不会影响服务器上已运行的容器。",
            confirmLabel = "清空",
            destructive = true,
            onConfirm = {
                app.resetComposeProject()
                clearConfirm = false
            },
            onDismiss = { clearConfirm = false },
        )
    }

    // 网络编辑
    if (showNetworkDialog) {
        NetworkSpecDialog(
            existing = editNetwork,
            onDismiss = { showNetworkDialog = false; editNetwork = null },
            onSave = { spec ->
                val idx = app.composeNetworks.indexOfFirst { it.name == spec.name }
                if (idx >= 0) app.composeNetworks[idx] = spec else app.composeNetworks.add(spec)
                showNetworkDialog = false
                editNetwork = null
            },
        )
    }

    // 卷编辑
    if (showVolumeDialog) {
        VolumeSpecDialog(
            existing = editVolume,
            onDismiss = { showVolumeDialog = false; editVolume = null },
            onSave = { spec ->
                val idx = app.composeVolumes.indexOfFirst { it.name == spec.name }
                if (idx >= 0) app.composeVolumes[idx] = spec else app.composeVolumes.add(spec)
                showVolumeDialog = false
                editVolume = null
            },
        )
    }
}

@Composable
private fun ServiceCard(
    service: ServiceSpec,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Compose, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(service.image.ifBlank { "（未填镜像）" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Row {
                    if (service.ports.any { it.isComplete }) {
                        com.fnos.dockermanager.ui.components.TagChip(
                            service.ports.filter { it.isComplete }.joinToString(" ") { "${it.hostPort}:${it.containerPort}" },
                        )
                    }
                    if (service.volumes.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        com.fnos.dockermanager.ui.components.TagChip("存储 ${service.volumes.size}")
                    }
                    if (service.environment.isNotEmpty()) {
                        Spacer(Modifier.width(6.dp))
                        com.fnos.dockermanager.ui.components.TagChip("环境变量 ${service.environment.size}")
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkSpecDialog(
    existing: NetworkSpec?,
    onDismiss: () -> Unit,
    onSave: (NetworkSpec) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var driver by remember { mutableStateOf(existing?.driver ?: "bridge") }
    var subnet by remember { mutableStateOf(existing?.subnet ?: "") }
    var gateway by remember { mutableStateOf(existing?.gateway ?: "") }
    var internal by remember { mutableStateOf(existing?.internal ?: false) }
    var external by remember { mutableStateOf(existing?.external ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(if (existing == null) "添加网络" else "编辑网络", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("网络名称") },
                placeholder = { Text("backend") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = driver,
                onValueChange = { driver = it },
                label = { Text("驱动") },
                placeholder = { Text("bridge") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = internal, onCheckedChange = { internal = it })
                Spacer(Modifier.width(8.dp))
                Text("内部网络", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = external, onCheckedChange = { external = it })
                Spacer(Modifier.width(8.dp))
                Text("外部网络（已存在于服务器，不创建）", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                NetworkSpec(
                                    name = name.trim(),
                                    driver = driver.trim().ifBlank { "bridge" },
                                    subnet = subnet.trim(),
                                    gateway = gateway.trim(),
                                    internal = internal,
                                    external = external,
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSpecDialog(
    existing: NamedVolumeSpec?,
    onDismiss: () -> Unit,
    onSave: (NamedVolumeSpec) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var driver by remember { mutableStateOf(existing?.driver ?: "local") }
    var external by remember { mutableStateOf(existing?.external ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(if (existing == null) "添加卷" else "编辑卷", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("卷名称") },
                placeholder = { Text("appdata") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = driver,
                onValueChange = { driver = it },
                label = { Text("驱动") },
                placeholder = { Text("local") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = external, onCheckedChange = { external = it })
                Spacer(Modifier.width(8.dp))
                Text("外部卷（已存在于服务器，不创建）", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onSave(
                                NamedVolumeSpec(
                                    name = name.trim(),
                                    driver = driver.trim().ifBlank { "local" },
                                    external = external,
                                )
                            )
                        }
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存")
                }
            }
        }
    }
}
