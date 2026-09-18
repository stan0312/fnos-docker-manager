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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.composefile.KeyValue
import com.fnos.dockermanager.composefile.NetworkMode
import com.fnos.dockermanager.composefile.PortMapping
import com.fnos.dockermanager.composefile.RestartPolicy
import com.fnos.dockermanager.composefile.ServiceSpec
import com.fnos.dockermanager.composefile.VolumeMount
import com.fnos.dockermanager.connection.ServerStore
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.components.KeyValueEditor
import com.fnos.dockermanager.ui.components.KeyValueRow
import com.fnos.dockermanager.ui.components.PortMappingEditor
import com.fnos.dockermanager.ui.components.SectionTitle
import com.fnos.dockermanager.ui.components.VolumeMountEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceEditScreen(serviceIndex: Int) {
    val app = LocalAppState.current
    val isNew = serviceIndex < 0
    val original = if (isNew) null else app.composeServices.getOrNull(serviceIndex)

    // 工作副本（深拷贝，避免直接修改快照列表）
    var name by remember { mutableStateOf(original?.name ?: "") }
    var image by remember { mutableStateOf(original?.image ?: "") }
    var containerName by remember { mutableStateOf(original?.containerName ?: "") }
    var command by remember { mutableStateOf(original?.command ?: "") }
    var entrypoint by remember { mutableStateOf(original?.entrypoint ?: "") }
    var tty by remember { mutableStateOf(original?.tty ?: false) }
    var privileged by remember { mutableStateOf(original?.privileged ?: false) }
    var restart by remember { mutableStateOf(original?.restart ?: RestartPolicy.NO) }
    var restartRetries by remember { mutableStateOf(original?.restartRetries ?: "") }
    var memLimit by remember { mutableStateOf(original?.memLimit ?: "") }
    var cpuShares by remember { mutableStateOf(original?.cpuShares ?: "") }
    var networkMode by remember { mutableStateOf(original?.networkMode ?: NetworkMode.BRIDGE) }
    var customNetwork by remember { mutableStateOf(original?.customNetwork ?: "") }

    var ports by remember { mutableStateOf(original?.ports?.toMutableList() ?: mutableListOf<PortMapping>()) }
    var volumes by remember { mutableStateOf(original?.volumes?.toMutableList() ?: mutableListOf<VolumeMount>()) }
    var environment by remember { mutableStateOf(original?.environment?.toMutableList() ?: mutableListOf<KeyValue>()) }
    var labels by remember { mutableStateOf(original?.labels?.toMutableList() ?: mutableListOf<KeyValue>()) }
    var dependsOn by remember { mutableStateOf(original?.dependsOn?.toMutableList() ?: mutableListOf<String>()) }
    var extraHosts by remember { mutableStateOf(original?.extraHosts?.toMutableList() ?: mutableListOf<String>()) }

    var error by remember { mutableStateOf<String?>(null) }

    val networkNames = app.composeNetworks.map { it.name }.filter { it.isNotBlank() }

    fun save() {
        if (name.isBlank()) {
            error = "请填写服务名称"
            return
        }
        if (image.isBlank()) {
            error = "请填写镜像名称"
            return
        }
        error = null
        val spec = ServiceSpec(
            name = name.trim(),
            image = image.trim(),
            containerName = containerName.trim(),
            command = command.trim(),
            entrypoint = entrypoint.trim(),
            restart = restart,
            restartRetries = restartRetries.trim(),
            ports = ports,
            volumes = volumes,
            environment = environment,
            labels = labels,
            networkMode = networkMode,
            customNetwork = customNetwork.trim(),
            dependsOn = dependsOn,
            tty = tty,
            privileged = privileged,
            memLimit = memLimit.trim(),
            cpuShares = cpuShares.trim(),
            extraHosts = extraHosts,
        )
        if (isNew) {
            app.composeServices.add(spec)
        } else {
            if (serviceIndex < app.composeServices.size) {
                app.composeServices[serviceIndex] = spec
            }
        }
        app.popOverlay()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "添加服务" else "编辑服务", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存服务", fontWeight = FontWeight.SemiBold)
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
            }

            // 基础
            AppCard {
                Column {
                    Text("基础配置", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("服务名称 *") },
                        placeholder = { Text("web") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = image,
                        onValueChange = { image = it },
                        label = { Text("镜像 *") },
                        placeholder = { Text("nginx:latest") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = containerName,
                        onValueChange = { containerName = it },
                        label = { Text("容器名称（可选）") },
                        placeholder = { Text("my-web") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        label = { Text("启动命令（可选）") },
                        placeholder = { Text("nginx -g 'daemon off;'") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = entrypoint,
                        onValueChange = { entrypoint = it },
                        label = { Text("入口点 entrypoint（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = tty, onCheckedChange = { tty = it })
                        Spacer(Modifier.width(8.dp))
                        Text("开启 TTY 终端", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = privileged, onCheckedChange = { privileged = it })
                        Spacer(Modifier.width(8.dp))
                        Text("特权模式（谨慎使用）", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 端口
            AppCard {
                Column {
                    Text("端口映射", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    PortMappingEditor(
                        ports = ports,
                        onAdd = { ports.add(PortMapping()) },
                        onRemove = { i -> ports.removeAt(i) },
                        onUpdate = { i, p -> ports[i] = p },
                    )
                }
            }

            // 存储
            AppCard {
                Column {
                    Text("存储与挂载", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "选择储存位置：可直接输入飞牛路径，或点击下方预设/最近使用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.height(8.dp))
                    VolumeMountEditor(
                        mounts = volumes,
                        pathPresets = ServerStore.PATH_PRESETS,
                        recentPaths = app.recentPaths,
                        onAdd = { volumes.add(VolumeMount()) },
                        onRemove = { i -> volumes.removeAt(i) },
                        onUpdate = { i, m -> volumes[i] = m },
                        onUsePath = { i, path ->
                            volumes[i] = volumes[i].copy(hostPath = path)
                            app.addRecentPath(path)
                        },
                    )
                }
            }

            // 环境变量
            AppCard {
                Column {
                    Text("环境变量", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    KeyValueEditor(
                        rows = environment.map { KeyValueRow(it.key, it.value) },
                        keyLabel = "键",
                        valueLabel = "值",
                        onAdd = { environment.add(KeyValue()) },
                        onRemove = { i -> environment.removeAt(i) },
                        onKeyChange = { i, v -> environment[i] = environment[i].copy(key = v) },
                        onValueChange = { i, v -> environment[i] = environment[i].copy(value = v) },
                    )
                }
            }

            // 网络
            AppCard {
                Column {
                    Text("网络", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        NetworkMode.entries.forEach { mode ->
                            FilterChip(
                                selected = networkMode == mode,
                                onClick = { networkMode = mode },
                                label = { Text(mode.label) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                    }
                    if (networkMode == NetworkMode.CUSTOM) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customNetwork,
                            onValueChange = { customNetwork = it },
                            label = { Text("自定义网络名称") },
                            placeholder = { Text("backend") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (networkNames.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text("已有网络：", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            Row(Modifier.padding(top = 4.dp)) {
                                networkNames.forEach { netName ->
                                    FilterChip(
                                        selected = customNetwork == netName,
                                        onClick = { customNetwork = netName },
                                        label = { Text(netName) },
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 重启策略
            AppCard {
                Column {
                    Text("重启策略", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        RestartPolicy.entries.forEach { policy ->
                            FilterChip(
                                selected = restart == policy,
                                onClick = { restart = policy },
                                label = { Text(policy.label) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                    }
                    if (restart == RestartPolicy.ON_FAILURE) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = restartRetries,
                            onValueChange = { restartRetries = it.filter { c -> c.isDigit() } },
                            label = { Text("最大重试次数（可选）") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 资源
            AppCard {
                Column {
                    Text("资源限制", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = memLimit,
                        onValueChange = { memLimit = it },
                        label = { Text("内存限制（如 512m / 2g，可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = cpuShares,
                        onValueChange = { cpuShares = it.filter { c -> c.isDigit() } },
                        label = { Text("CPU 份额（相对权重，可选）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 依赖
            AppCard {
                Column {
                    Text("依赖服务 depends_on", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val candidates = app.composeServices.map { it.name }.filter { it.isNotBlank() && it != name }
                    if (candidates.isEmpty()) {
                        Text(
                            "暂无可依赖的服务（保存后回到项目页，先添加其他服务）",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    } else {
                        Row {
                            candidates.forEach { depName ->
                                val selected = dependsOn.contains(depName)
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (selected) dependsOn.remove(depName) else dependsOn.add(depName)
                                    },
                                    label = { Text(depName) },
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                        }
                    }
                }
            }

            // extra_hosts
            AppCard {
                Column {
                    Text("extra_hosts（自定义 Host 映射，可选）", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    extraHosts.forEachIndexed { index, host ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { v -> extraHosts[index] = v },
                                placeholder = { Text("host.docker.internal:host-gateway") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { extraHosts.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    OutlinedButton(onClick = { extraHosts.add("") }) {
                        Text("添加 Host 映射")
                    }
                }
            }

            // 标签
            AppCard {
                Column {
                    Text("标签 Labels", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    KeyValueEditor(
                        rows = labels.map { KeyValueRow(it.key, it.value) },
                        keyLabel = "键",
                        valueLabel = "值",
                        onAdd = { labels.add(KeyValue()) },
                        onRemove = { i -> labels.removeAt(i) },
                        onKeyChange = { i, v -> labels[i] = labels[i].copy(key = v) },
                        onValueChange = { i, v -> labels[i] = labels[i].copy(value = v) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
