package com.fnos.dockermanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.fnos.dockermanager.composefile.PortMapping
import com.fnos.dockermanager.composefile.VolumeMount
import com.fnos.dockermanager.composefile.VolumeType

/** 端口映射编辑器 */
@Composable
fun PortMappingEditor(
    ports: List<PortMapping>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, PortMapping) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ports.forEachIndexed { index, p ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = p.hostPort,
                    onValueChange = { onUpdate(index, p.copy(hostPort = it)) },
                    label = { Text("宿主机端口") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(5.dp))
                Text("→", color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.width(5.dp))
                OutlinedTextField(
                    value = p.containerPort,
                    onValueChange = { onUpdate(index, p.copy(containerPort = it)) },
                    label = { Text("容器端口") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemove(index) }) {
                    Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.width(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("添加端口映射")
        }
    }
}

/** 卷/存储挂载编辑器（含储存位置选择） */
@Composable
fun VolumeMountEditor(
    mounts: List<VolumeMount>,
    pathPresets: List<String>,
    recentPaths: List<String>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onUpdate: (Int, VolumeMount) -> Unit,
    onUsePath: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        mounts.forEachIndexed { index, m ->
            val isBind = m.type == VolumeType.BIND
            Column(
                Modifier.fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isBind) "目录挂载" else "命名卷",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        onUpdate(index, m.copy(type = if (isBind) VolumeType.VOLUME else VolumeType.BIND))
                    }) {
                        Text("切换为${if (isBind) "命名卷" else "目录挂载"}")
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.outline)
                    }
                }
                if (isBind) {
                    OutlinedTextField(
                        value = m.hostPath,
                        onValueChange = { onUpdate(index, m.copy(hostPath = it)) },
                        label = { Text("宿主机路径（储存位置）") },
                        placeholder = { Text("/vol1/docker/appdata") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val suggestions = (pathPresets + recentPaths).distinct().filter { it.isNotBlank() }
                    if (suggestions.isNotEmpty()) {
                        LazyRow(modifier = Modifier.padding(top = 6.dp)) {
                            items(suggestions.size) { i ->
                                val path = suggestions[i]
                                AssistChip(
                                    onClick = { onUsePath(index, path) },
                                    label = { Text(path, maxLines = 1) },
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = m.name,
                        onValueChange = { onUpdate(index, m.copy(name = it)) },
                        label = { Text("卷名称") },
                        placeholder = { Text("data") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = m.containerPath,
                    onValueChange = { onUpdate(index, m.copy(containerPath = it)) },
                    label = { Text("容器内路径") },
                    placeholder = { Text("/app/data") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Switch(
                        checked = m.readOnly,
                        onCheckedChange = { onUpdate(index, m.copy(readOnly = it)) },
                        modifier = Modifier.scale(0.8f),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("只读挂载 (ro)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = onAdd, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.width(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("添加存储挂载")
        }
    }
}
