package com.fnos.dockermanager.connection

import com.fnos.dockermanager.createAppSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/**
 * 服务器配置与常用路径的本地持久化。
 * 说明：v1 将注册表密码保存在应用本地设置（NSUserDefaults），未使用钥匙串，
 * 仅建议在可信设备上使用；后续版本可迁移到 Keychain。
 */
class ServerStore(private val settings: Settings = createAppSettings()) {

    private val json = ServerProfileJson.json

    fun loadServers(): List<ServerProfile> {
        val raw = settings.getStringOrNull(KEY_SERVERS) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ServerProfile.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun saveServers(servers: List<ServerProfile>) {
        val raw = json.encodeToString(ListSerializer(ServerProfile.serializer()), servers)
        settings.putString(KEY_SERVERS, raw)
    }

    fun saveServer(server: ServerProfile) {
        val servers = loadServers().toMutableList()
        val idx = servers.indexOfFirst { it.id == server.id }
        if (idx >= 0) servers[idx] = server else servers.add(server)
        saveServers(servers)
    }

    fun deleteServer(id: String) {
        saveServers(loadServers().filterNot { it.id == id })
        if (selectedServerId() == id) settings.remove(KEY_SELECTED)
    }

    fun selectedServerId(): String? = settings.getStringOrNull(KEY_SELECTED)

    fun setSelectedServerId(id: String?) {
        if (id == null) settings.remove(KEY_SELECTED) else settings.putString(KEY_SELECTED, id)
    }

    // ---- 常用存储路径（用于编排/挂载选择）----

    fun loadRecentPaths(): List<String> =
        settings.getStringOrNull(KEY_RECENT_PATHS)
            ?.split('\u0001')
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    fun addRecentPath(path: String) {
        if (path.isBlank()) return
        val list = loadRecentPaths().toMutableList()
        list.remove(path)
        list.add(0, path)
        settings.putString(KEY_RECENT_PATHS, list.take(20).joinToString("\u0001"))
    }

    fun clearRecentPaths() {
        settings.remove(KEY_RECENT_PATHS)
    }

    // ---- 主题 ----

    fun loadThemeMode(): String = settings.getStringOrNull(KEY_THEME) ?: "system"

    fun saveThemeMode(mode: String) {
        settings.putString(KEY_THEME, mode)
    }

    companion object {
        const val KEY_SERVERS = "servers"
        const val KEY_SELECTED = "selected_server"
        const val KEY_RECENT_PATHS = "recent_paths"
        const val KEY_THEME = "theme_mode"

        /** 飞牛常见存储根路径预设，可按需编辑 */
        val PATH_PRESETS = listOf(
            "/vol1/docker",
            "/vol2/docker",
            "/vol1/1000/docker",
            "/vol2/1000/docker",
            "/home/docker",
            "/data/docker",
        )
    }
}
