package com.fnos.dockermanager.composefile

import kotlinx.serialization.Serializable

enum class RestartPolicy(val yamlValue: String, val label: String) {
    NO("no", "不重启"),
    ALWAYS("always", "总是重启"),
    ON_FAILURE("on-failure", "失败时重启"),
    UNLESS_STOPPED("unless-stopped", "除非手动停止");

    companion object {
        fun fromYaml(value: String?): RestartPolicy = when (value?.lowercase()?.substringBefore(':')) {
            "always" -> ALWAYS
            "on-failure" -> ON_FAILURE
            "unless-stopped" -> UNLESS_STOPPED
            else -> NO
        }

        fun fromDockerName(value: String?): RestartPolicy = when (value) {
            "always" -> ALWAYS
            "on-failure" -> ON_FAILURE
            "unless-stopped" -> UNLESS_STOPPED
            else -> NO
        }
    }
}

enum class NetworkMode(val label: String) {
    BRIDGE("bridge（默认）"),
    HOST("host（宿主机网络）"),
    NONE("none（无网络）"),
    CUSTOM("自定义网络"),
}

enum class VolumeType(val label: String) {
    VOLUME("命名卷"),
    BIND("目录挂载（绑定）"),
}

@Serializable
data class KeyValue(
    var key: String = "",
    var value: String = "",
)

@Serializable
data class PortMapping(
    var hostIp: String = "",
    var hostPort: String = "",
    var containerPort: String = "",
    var protocol: String = "tcp",
) {
    val isComplete: Boolean get() = containerPort.isNotBlank() && hostPort.isNotBlank()
}

@Serializable
data class VolumeMount(
    var type: VolumeType = VolumeType.VOLUME,
    var name: String = "",
    var hostPath: String = "",
    var containerPath: String = "",
    var readOnly: Boolean = false,
) {
    val isComplete: Boolean get() = containerPath.isNotBlank() && (name.isNotBlank() || hostPath.isNotBlank())
}

@Serializable
data class ServiceSpec(
    var name: String = "",
    var image: String = "",
    var containerName: String = "",
    var command: String = "",
    var entrypoint: String = "",
    var restart: RestartPolicy = RestartPolicy.NO,
    var restartRetries: String = "",
    var ports: MutableList<PortMapping> = mutableListOf(),
    var volumes: MutableList<VolumeMount> = mutableListOf(),
    var environment: MutableList<KeyValue> = mutableListOf(),
    var labels: MutableList<KeyValue> = mutableListOf(),
    var networkMode: NetworkMode = NetworkMode.BRIDGE,
    var customNetwork: String = "",
    var dependsOn: MutableList<String> = mutableListOf(),
    var tty: Boolean = false,
    var privileged: Boolean = false,
    var memLimit: String = "",
    var cpuShares: String = "",
    var extraHosts: MutableList<String> = mutableListOf(),
) {
    val isValid: Boolean get() = name.isNotBlank() && image.isNotBlank()
}

@Serializable
data class NetworkSpec(
    var name: String = "",
    var driver: String = "bridge",
    var subnet: String = "",
    var gateway: String = "",
    var internal: Boolean = false,
    var external: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank()
}

@Serializable
data class NamedVolumeSpec(
    var name: String = "",
    var driver: String = "local",
    var external: Boolean = false,
) {
    val isValid: Boolean get() = name.isNotBlank()
}

@Serializable
data class ComposeProject(
    var name: String = "",
    var services: MutableList<ServiceSpec> = mutableListOf(),
    var networks: MutableList<NetworkSpec> = mutableListOf(),
    var volumes: MutableList<NamedVolumeSpec> = mutableListOf(),
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("请填写项目名称")
        if (services.isEmpty()) errors.add("请至少添加一个服务")
        val seen = mutableSetOf<String>()
        services.forEach { s ->
            if (!s.isValid) errors.add("服务「${s.name.ifBlank { "(未命名)" }}」缺少名称或镜像")
            if (!seen.add(s.name)) errors.add("服务名称重复：${s.name}")
            s.ports.filter { it.isComplete }.forEach { p ->
                if (!p.containerPort.all { it.isDigit() }) errors.add("服务「${s.name}」端口映射无效")
            }
        }
        networks.filter { it.isValid }.forEach { n ->
            if (n.subnet.isNotBlank() && n.subnet.split(".").size != 4 && !n.subnet.contains('/')) {
                errors.add("网络「${n.name}」子网格式无效（示例 172.20.0.0/16）")
            }
        }
        return errors
    }
}
