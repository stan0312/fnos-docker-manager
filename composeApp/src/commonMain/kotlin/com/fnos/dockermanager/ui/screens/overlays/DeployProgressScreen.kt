package com.fnos.dockermanager.ui.screens.overlays

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Tab
import com.fnos.dockermanager.composefile.ComposeDeployer
import com.fnos.dockermanager.composefile.DeployStatus
import com.fnos.dockermanager.ui.components.AppCard
import com.fnos.dockermanager.ui.icons.AppIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeployProgressScreen(startAfterCreate: Boolean) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val logs = remember { mutableStateListOf<String>() }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val client = app.client
        val project = app.buildComposeProject()
        if (client == null) {
            errorMessage = "未连接服务器"
            failed = true
            done = true
            return@LaunchedEffect
        }
        running = true
        val deployer = ComposeDeployer(client)
        try {
            val report = deployer.deploy(project, startContainers = startAfterCreate) { line ->
                logs.add(line)
            }
            logs.add("")
            logs.add("==== 部署结果 ====")
            logs.add("成功 ${report.okCount} · 失败 ${report.failedCount} · 跳过 ${report.skippedCount}")
            report.items.filter { it.status == DeployStatus.FAILED }.forEach {
                logs.add("❌ ${it.serviceName}: ${it.message}")
            }
            failed = report.failedCount > 0
            done = true
        } catch (e: Exception) {
            errorMessage = e.message ?: "部署异常"
            failed = true
            done = true
            logs.add("部署异常：${e.message}")
        } finally {
            running = false
        }
    }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.scrollToItem(logs.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("部署 · ${app.projectName.ifBlank { "未命名项目" }}", fontWeight = FontWeight.SemiBold, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            if (done) {
                Button(
                    onClick = {
                        app.popOverlay()
                        app.switchTab(Tab.Containers)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(48.dp),
                ) {
                    Text(if (failed) "完成（有失败项，可查看上方日志）" else "完成，去容器页查看", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    running -> {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在部署…", fontWeight = FontWeight.Medium)
                    }
                    failed -> {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("部署完成，存在失败项", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                    else -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("部署成功", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                items(logs.size) { i ->
                    Text(
                        logs[i].ifEmpty { " " },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
