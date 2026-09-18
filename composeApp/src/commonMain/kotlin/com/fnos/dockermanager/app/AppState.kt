package com.fnos.dockermanager.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fnos.dockermanager.composefile.ComposeProject
import com.fnos.dockermanager.composefile.DeployReport
import com.fnos.dockermanager.composefile.NamedVolumeSpec
import com.fnos.dockermanager.composefile.NetworkSpec
import com.fnos.dockermanager.composefile.ServiceSpec
import com.fnos.dockermanager.connection.ServerProfile
import com.fnos.dockermanager.connection.ServerStore
import com.fnos.dockermanager.docker.DockerClient
import com.fnos.dockermanager.docker.SystemInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class Tab(val label: String) {
    Overview("总览"),
    Containers("容器"),
    Images("镜像"),
    Registries("仓库"),
    Networks("网络"),
    Compose("编排"),
}

sealed interface Overlay {
    data class ServerList(val initial: Boolean = false) : Overlay
    data class ServerEdit(val server: ServerProfile? = null) : Overlay
    data class ContainerDetail(val id: String, val name: String) : Overlay
    data class LogsViewer(val id: String, val name: String) : Overlay
    data class ImageDetail(val id: String, val name: String) : Overlay
    data class PullImage(val defaultTerm: String = "") : Overlay
    data class NetworkDetail(val id: String, val name: String) : Overlay
    data object VolumeList : Overlay
    data class VolumeDetail(val name: String) : Overlay
    data class ServiceEdit(val serviceIndex: Int) : Overlay
    data class YamlPreview(val previewOnly: Boolean = false) : Overlay
    data class DeployProgress(val startAfterCreate: Boolean = true) : Overlay
}

data class DeployState(
    val logs: List<String> = emptyList(),
    val report: DeployReport? = null,
    val running: Boolean = false,
    val failed: Boolean = false,
    val errorMessage: String? = null,
)

@OptIn(ExperimentalUuidApi::class)
class AppState(val scope: CoroutineScope) {

    val serverStore = ServerStore()

    var servers by mutableStateOf(serverStore.loadServers())
    var selectedServer by mutableStateOf<ServerProfile?>(null)
    var client by mutableStateOf<DockerClient?>(null)
    var connecting by mutableStateOf(false)
    var connected by mutableStateOf(false)
    var systemInfo by mutableStateOf<SystemInfo?>(null)
    var lastError by mutableStateOf<String?>(null)

    var message by mutableStateOf<String?>(null)

    var currentTab by mutableStateOf(Tab.Overview)
    val overlayStack = mutableStateListOf<Overlay>()

    val recentPaths = mutableStateListOf<String>().apply {
        addAll(serverStore.loadRecentPaths())
    }

    // ---- Compose 编排编辑器状态 ----
    var projectName by mutableStateOf("")
    val composeServices = mutableStateListOf<ServiceSpec>()
    val composeNetworks = mutableStateListOf<NetworkSpec>()
    val composeVolumes = mutableStateListOf<NamedVolumeSpec>()
    var deployState by mutableStateOf<DeployState?>(null)

    init {
        val id = serverStore.selectedServerId()
        val server = servers.find { it.id == id } ?: servers.firstOrNull()
        if (server != null) {
            selectedServer = server
            connect()
        }
    }

    // ============ 连接 ============

    fun selectServer(server: ServerProfile, autoConnect: Boolean = true) {
        selectedServer = server
        serverStore.setSelectedServerId(server.id)
        connected = false
        systemInfo = null
        lastError = null
        client?.close()
        client = null
        if (autoConnect) connect()
    }

    fun connect() {
        val server = selectedServer ?: return
        if (connecting) return
        scope.launch {
            connecting = true
            connected = false
            lastError = null
            try {
                val newClient = DockerClient(server)
                val info = newClient.info()
                client = newClient
                systemInfo = info
                connected = true
                message = "已连接 ${server.name}"
            } catch (e: Exception) {
                lastError = "连接失败：${e.message ?: e::class.simpleName}"
                connected = false
            } finally {
                connecting = false
            }
        }
    }

    fun refreshInfo() {
        scope.launch {
            runCatching { client?.info() }.onSuccess {
                if (it != null) systemInfo = it
            }.onFailure {
                lastError = it.message
            }
        }
    }

    // ============ 服务器管理 ============

    @OptIn(ExperimentalUuidApi::class)
    fun newServerId(): String = Uuid.random().toString()

    fun saveServer(server: ServerProfile) {
        serverStore.saveServer(server)
        servers = serverStore.loadServers()
        if (selectedServer?.id == server.id) selectedServer = server
    }

    fun deleteServer(id: String) {
        if (selectedServer?.id == id) {
            client?.close()
            client = null
            selectedServer = null
            connected = false
            systemInfo = null
        }
        serverStore.deleteServer(id)
        servers = serverStore.loadServers()
    }

    // ============ 导航 ============

    fun switchTab(tab: Tab) {
        currentTab = tab
    }

    fun pushOverlay(overlay: Overlay) {
        overlayStack.add(overlay)
    }

    fun popOverlay() {
        if (overlayStack.isNotEmpty()) overlayStack.removeAt(overlayStack.lastIndex)
    }

    fun clearOverlays() {
        overlayStack.clear()
    }

    // ============ 编排编辑器 ============

    fun resetComposeProject() {
        projectName = ""
        composeServices.clear()
        composeNetworks.clear()
        composeVolumes.clear()
        deployState = null
    }

    fun loadComposeProject(project: ComposeProject) {
        projectName = project.name
        composeServices.clear()
        composeServices.addAll(project.services)
        composeNetworks.clear()
        composeNetworks.addAll(project.networks)
        composeVolumes.clear()
        composeVolumes.addAll(project.volumes)
        deployState = null
    }

    fun buildComposeProject(): ComposeProject = ComposeProject(
        name = projectName,
        services = composeServices.toMutableList(),
        networks = composeNetworks.toMutableList(),
        volumes = composeVolumes.toMutableList(),
    )

    // ============ 常用路径 ============

    fun addRecentPath(path: String) {
        if (path.isBlank()) return
        recentPaths.remove(path)
        recentPaths.add(0, path)
        while (recentPaths.size > 20) recentPaths.removeAt(recentPaths.lastIndex)
        serverStore.addRecentPath(path)
    }

    fun showMessage(text: String) {
        message = text
    }
}
