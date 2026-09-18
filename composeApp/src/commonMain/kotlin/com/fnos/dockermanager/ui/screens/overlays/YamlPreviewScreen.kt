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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.composefile.ComposeYamlGenerator
import com.fnos.dockermanager.copyToClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YamlPreviewScreen(previewOnly: Boolean) {
    val app = LocalAppState.current
    val yaml = rememberYaml(app)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YAML 预览", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { app.popOverlay() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        copyToClipboard(yaml)
                        app.showMessage("已复制到剪贴板")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                    }
                },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        copyToClipboard(yaml)
                        app.showMessage("已复制到剪贴板")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text("复制")
                }
                Button(
                    onClick = {
                        app.popOverlay()
                        app.pushOverlay(Overlay.DeployProgress(startAfterCreate = true))
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("部署此项目")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "此 YAML 由可视化模型实时生成，可直接用于服务器上的 docker compose。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
            ) {
                items(yaml.split('\n').size) { i ->
                    Text(
                        yaml.split('\n')[i].ifEmpty { " " },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun rememberYaml(app: com.fnos.dockermanager.app.AppState): String =
    androidx.compose.runtime.remember(app.projectName, app.composeServices.size, app.composeNetworks.size, app.composeVolumes.size) {
        ComposeYamlGenerator.generate(app.buildComposeProject())
    }
