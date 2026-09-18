package com.fnos.dockermanager.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.fnos.dockermanager.app.AppState
import com.fnos.dockermanager.app.LocalAppState
import com.fnos.dockermanager.app.Overlay
import com.fnos.dockermanager.app.Tab
import com.fnos.dockermanager.ui.icons.AppIcons
import com.fnos.dockermanager.ui.screens.ContainersScreen
import com.fnos.dockermanager.ui.screens.DashboardScreen
import com.fnos.dockermanager.ui.screens.ImagesScreen
import com.fnos.dockermanager.ui.screens.NetworksScreen
import com.fnos.dockermanager.ui.screens.RegistriesScreen
import com.fnos.dockermanager.ui.screens.overlays.ComposeStudioScreen
import com.fnos.dockermanager.ui.screens.overlays.ContainerDetailScreen
import com.fnos.dockermanager.ui.screens.overlays.DeployProgressScreen
import com.fnos.dockermanager.ui.screens.overlays.ImageDetailScreen
import com.fnos.dockermanager.ui.screens.overlays.LogsViewerScreen
import com.fnos.dockermanager.ui.screens.overlays.NetworkDetailScreen
import com.fnos.dockermanager.ui.screens.overlays.PullImageScreen
import com.fnos.dockermanager.ui.screens.overlays.ServerEditScreen
import com.fnos.dockermanager.ui.screens.overlays.ServerListScreen
import com.fnos.dockermanager.ui.screens.overlays.ServiceEditScreen
import com.fnos.dockermanager.ui.screens.overlays.VolumeDetailScreen
import com.fnos.dockermanager.ui.screens.overlays.VolumeListScreen
import com.fnos.dockermanager.ui.screens.overlays.YamlPreviewScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val app = LocalAppState.current

    if (app.servers.isEmpty() && app.overlayStack.isEmpty()) {
        ServerListScreen(initial = true)
        return
    }

    if (app.overlayStack.isNotEmpty()) {
        OverlayHost(app.overlayStack.last())
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(app.message) {
        val msg = app.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        app.message = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(app.currentTab.label, fontWeight = FontWeight.SemiBold)
                        app.selectedServer?.let {
                            Text(
                                "${it.name} · ${it.url}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { app.switchTab(Tab.Overview) }) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "总览",
                            tint = if (app.currentTab == Tab.Overview) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { app.pushOverlay(Overlay.ServerList()) }) {
                        Icon(Icons.Default.Settings, contentDescription = "服务器管理")
                    }
                    IconButton(onClick = { app.connect() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新连接")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                bottomItems.forEach { (tab, icon) ->
                    NavigationBarItem(
                        selected = app.currentTab == tab,
                        onClick = { app.switchTab(tab) },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, maxLines = 1) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (app.currentTab) {
            Tab.Overview -> DashboardScreen(Modifier.fillMaxSize().padding(padding))
            Tab.Containers -> ContainersScreen(Modifier.fillMaxSize().padding(padding))
            Tab.Images -> ImagesScreen(Modifier.fillMaxSize().padding(padding))
            Tab.Registries -> RegistriesScreen(Modifier.fillMaxSize().padding(padding))
            Tab.Networks -> NetworksScreen(Modifier.fillMaxSize().padding(padding))
            Tab.Compose -> ComposeStudioScreen(Modifier.fillMaxSize().padding(padding))
        }
    }
}

private data class BottomItem(val tab: Tab, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Tab.Containers, AppIcons.Container),
    BottomItem(Tab.Images, AppIcons.Image),
    BottomItem(Tab.Registries, AppIcons.Registry),
    BottomItem(Tab.Networks, AppIcons.Network),
    BottomItem(Tab.Compose, AppIcons.Compose),
)

@Composable
private fun OverlayHost(overlay: Overlay) {
    val app = LocalAppState.current
    when (overlay) {
        is Overlay.ServerList -> ServerListScreen(overlay.initial)
        is Overlay.ServerEdit -> ServerEditScreen(overlay.server)
        is Overlay.ContainerDetail -> ContainerDetailScreen(overlay.id, overlay.name)
        is Overlay.LogsViewer -> LogsViewerScreen(overlay.id, overlay.name)
        is Overlay.ImageDetail -> ImageDetailScreen(overlay.id, overlay.name)
        is Overlay.PullImage -> PullImageScreen(overlay.defaultTerm)
        is Overlay.NetworkDetail -> NetworkDetailScreen(overlay.id, overlay.name)
        is Overlay.VolumeList -> VolumeListScreen()
        is Overlay.VolumeDetail -> VolumeDetailScreen(overlay.name)
        is Overlay.ServiceEdit -> ServiceEditScreen(overlay.serviceIndex)
        is Overlay.YamlPreview -> YamlPreviewScreen(overlay.previewOnly)
        is Overlay.DeployProgress -> DeployProgressScreen(overlay.startAfterCreate)
    }
}
