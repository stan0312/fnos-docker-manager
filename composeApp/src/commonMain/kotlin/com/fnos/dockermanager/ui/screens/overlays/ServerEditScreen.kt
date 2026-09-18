package com.fnos.dockermanager.ui.screens.overlays

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.connection.ServerProfile
import com.fnos.dockermanager.docker.DockerClient
import com.fnos.dockermanager.ui.components.InfoRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerEditScreen(server: ServerProfile?) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf(server?.name ?: "") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var port by remember { mutableStateOf((server?.port ?: 2375).toString()) }
    var useTls by remember { mutableStateOf(server?.useTls ?: false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val isEdit = server != null

    fun validate(): String? {
        if (name.isBlank()) return "请填写服务器名称"
        if (host.isBlank()) return "请填写 IP 或域名"
        val p = port.toIntOrNull()
        if (p == null || p !in 1..65535) return "端口需要在 1-65535 之间"
        return null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑服务器" else "添加服务器", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("服务器名称") },
                placeholder = { Text("家里的飞牛") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text("IP 或域名") },
                placeholder = { Text("192.168.1.100") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() } },
                label = { Text("Docker API 端口") },
                placeholder = { Text("2375") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = useTls, onCheckedChange = { useTls = it })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("使用 HTTPS/TLS（2376）", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "自签名证书需先在 iPhone 上安装信任，否则请使用 HTTP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            testResult?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val v = validate() ?: return@OutlinedButton
                        testResult = null
                        error = null
                        testing = true
                        val profile = ServerProfile(
                            id = server?.id ?: app.newServerId(),
                            name = name.trim(),
                            host = host.trim(),
                            port = port.toIntOrNull() ?: 2375,
                            useTls = useTls,
                            registryAuths = server?.registryAuths ?: emptyMap(),
                        )
                        scope.launch {
                            runCatching {
                                val client = DockerClient(profile)
                                val info = client.info()
                                client.close()
                                "连接成功：Docker ${info.serverVersion ?: "?"} · ${info.operatingSystem ?: ""}".trim()
                            }.onSuccess { testResult = it }
                                .onFailure { error = "连接失败：${it.message ?: it::class.simpleName}" }
                            testing = false
                        }
                    },
                    enabled = !testing,
                    modifier = Modifier.weight(1f),
                ) {
                    if (testing) {
                        Text("测试中…")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.width(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("测试连接")
                    }
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        val v = validate()
                        if (v != null) {
                            error = v
                            return@Button
                        }
                        error = null
                        app.saveServer(
                            ServerProfile(
                                id = server?.id ?: app.newServerId(),
                                name = name.trim(),
                                host = host.trim(),
                                port = port.toIntOrNull() ?: 2375,
                                useTls = useTls,
                                registryAuths = server?.registryAuths ?: emptyMap(),
                            )
                        )
                        if (!isEdit && app.selectedServer == null) {
                            val saved = app.servers.lastOrNull()
                            if (saved != null) app.selectServer(saved, autoConnect = true)
                        }
                        app.popOverlay()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存")
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("如何开启飞牛 Docker 远程 API？", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            InfoRow("①", "在飞牛终端中编辑 /etc/docker/daemon.json，加入 hosts 配置并重启 Docker")
            InfoRow("②", "确保防火墙放行 2375/2376 端口")
            InfoRow("③", "仅建议在内网使用；公网访问请务必开启 TLS 并限制来源 IP")
            Spacer(Modifier.height(24.dp))
        }
    }
}
