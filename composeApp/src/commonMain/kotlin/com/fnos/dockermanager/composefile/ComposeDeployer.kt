package com.fnos.dockermanager.composefile

import com.fnos.dockermanager.connection.RegistryCredential
import com.fnos.dockermanager.docker.ContainerCreateRequest
import com.fnos.dockermanager.docker.ContainerHostConfig
import com.fnos.dockermanager.docker.DockerClient
import com.fnos.dockermanager.docker.DockerApiException
import com.fnos.dockermanager.docker.NetworkCreateRequest
import com.fnos.dockermanager.docker.PortBinding
import com.fnos.dockermanager.docker.RestartPolicy as DockerRestartPolicy
import com.fnos.dockermanager.docker.VolumeCreateRequest
import com.fnos.dockermanager.docker.Ipam
import com.fnos.dockermanager.docker.IpamConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class DeployItem(
    val serviceName: String,
    val containerId: String? = null,
    val containerName: String? = null,
    val status: DeployStatus,
    val message: String = "",
)

enum class DeployStatus { OK, SKIPPED, FAILED }

data class DeployReport(
    val projectName: String,
    val items: List<DeployItem>,
    val startContainers: Boolean,
) {
    val okCount: Int get() = items.count { it.status == DeployStatus.OK }
    val failedCount: Int get() = items.count { it.status == DeployStatus.FAILED }
    val skippedCount: Int get() = items.count { it.status == DeployStatus.SKIPPED }
}

/**
 * 通过 Docker Engine API 部署编排项目：
 * 命名卷 → 网络 → 拉镜像 → 按依赖顺序建容器 → 启动。
 */
class ComposeDeployer(private val client: DockerClient) {

    suspend fun deploy(
        project: ComposeProject,
        startContainers: Boolean = true,
        onLog: (String) -> Unit,
    ): DeployReport {
        val items = mutableListOf<DeployItem>()
        val createdNetworks = mutableListOf<String>()

        // 1. 命名卷
        project.volumes.filter { it.isValid && !it.external }.forEach { v ->
            onLog("创建卷 ${v.name} ...")
            try {
                client.createVolume(VolumeCreateRequest(name = v.name, driver = v.driver))
                items.add(DeployItem("卷:${v.name}", status = DeployStatus.OK, message = "卷已创建"))
            } catch (e: DockerApiException) {
                if (e.statusCode == 409) {
                    items.add(DeployItem("卷:${v.name}", status = DeployStatus.OK, message = "卷已存在"))
                } else {
                    onLog("卷 ${v.name} 创建失败: ${e.message}")
                    items.add(DeployItem("卷:${v.name}", status = DeployStatus.FAILED, message = e.message ?: ""))
                }
            } catch (e: Exception) {
                items.add(DeployItem("卷:${v.name}", status = DeployStatus.FAILED, message = e.message ?: ""))
            }
        }

        // 2. 网络
        project.networks.filter { it.isValid && !it.external }.forEach { n ->
            onLog("创建网络 ${n.name} ...")
            try {
                val ipamConfigs = buildList {
                    if (n.subnet.isNotBlank()) {
                        add(IpamConfig(subnet = n.subnet, gateway = n.gateway.ifBlank { null }))
                    }
                }
                client.createNetwork(
                    NetworkCreateRequest(
                        name = n.name,
                        driver = n.driver.ifBlank { "bridge" },
                        internal = n.internal,
                        ipam = Ipam(config = ipamConfigs),
                    )
                )
                createdNetworks.add(n.name)
                items.add(DeployItem("网络:${n.name}", status = DeployStatus.OK, message = "网络已创建"))
            } catch (e: DockerApiException) {
                if (e.statusCode == 409 || e.message?.contains("already exists") == true) {
                    createdNetworks.add(n.name)
                    items.add(DeployItem("网络:${n.name}", status = DeployStatus.OK, message = "网络已存在"))
                } else {
                    onLog("网络 ${n.name} 创建失败: ${e.message}")
                    items.add(DeployItem("网络:${n.name}", status = DeployStatus.FAILED, message = e.message ?: ""))
                }
            } catch (e: Exception) {
                items.add(DeployItem("网络:${n.name}", status = DeployStatus.FAILED, message = e.message ?: ""))
            }
        }

        // 3. 拉取镜像（去重）
        val imagesToPull = project.services.map { it.image.trim() to it }.distinctBy { it.first }
        val pulled = mutableSetOf<String>()
        for ((image, service) in imagesToPull) {
            if (image.isBlank() || image in pulled) continue
            val registryAuth = resolveRegistryAuth(image, client)
            onLog("拉取镜像 $image ...")
            var lastError: String? = null
            try {
                client.pullImage(fromImage = image, registryAuth = registryAuth).collect { ev ->
                    ev.error?.let { lastError = it }
                    ev.status?.let {
                        val progress = ev.progress?.let { p -> " $p" } ?: ""
                        onLog("镜像 $image: $it$progress")
                    }
                }
                if (lastError != null) {
                    onLog("镜像 $image 拉取失败: $lastError")
                    items.add(DeployItem(service.name, status = DeployStatus.FAILED, message = lastError!!))
                    continue
                }
                pulled.add(image)
                onLog("镜像 $image 就绪")
            } catch (e: Exception) {
                onLog("镜像 $image 拉取异常: ${e.message}")
                items.add(DeployItem(service.name, status = DeployStatus.FAILED, message = e.message ?: ""))
            }
        }

        // 4. 按依赖顺序创建容器
        val order = topoSort(project.services)
        for (s in order) {
            if (items.any { it.serviceName == s.name && it.status == DeployStatus.FAILED }) {
                items.add(DeployItem(s.name, status = DeployStatus.SKIPPED, message = "依赖项失败，跳过"))
                continue
            }
            onLog("创建容器 ${s.containerName.ifBlank { s.name }} ...")
            try {
                val req = buildCreateRequest(s, project)
                val resp = client.createContainer(req)
                items.add(DeployItem(s.name, containerId = resp.id, containerName = s.containerName, status = DeployStatus.OK, message = "已创建"))
            } catch (e: DockerApiException) {
                val msg = e.message ?: ""
                onLog("容器 ${s.name} 创建失败: $msg")
                items.add(DeployItem(s.name, status = DeployStatus.FAILED, message = msg))
            } catch (e: Exception) {
                onLog("容器 ${s.name} 创建异常: ${e.message}")
                items.add(DeployItem(s.name, status = DeployStatus.FAILED, message = e.message ?: ""))
            }
        }

        // 5. 启动
        if (startContainers) {
            val created = items.filter { it.status == DeployStatus.OK && it.containerId != null }
            for (item in created) {
                onLog("启动容器 ${item.containerName ?: item.serviceName} ...")
                try {
                    client.startContainer(item.containerId!!)
                } catch (e: Exception) {
                    onLog("启动失败: ${e.message}")
                }
            }
        }

        return DeployReport(project.name, items, startContainers)
    }

    private fun buildCreateRequest(s: ServiceSpec, project: ComposeProject): ContainerCreateRequest {
        val env = s.environment.filter { it.key.isNotBlank() }.map { "${it.key}=${it.value}" }
        val labels = s.labels.filter { it.key.isNotBlank() }.associate { it.key to it.value }

        val exposedPorts = buildJsonObject {
            s.ports.filter { it.isComplete }.forEach { p ->
                put("/${p.containerPort}/${p.protocol}", JsonObject(emptyMap()))
            }
        }
        val portBindings = buildMap {
            s.ports.filter { it.isComplete }.forEach { p ->
                val key = "/${p.containerPort}/${p.protocol}"
                put(key, listOf(PortBinding(hostIp = p.hostIp, hostPort = p.hostPort)))
            }
        }

        val binds = s.volumes.filter { it.isComplete }.map { v ->
            val source = if (v.type == VolumeType.VOLUME) v.name else v.hostPath
            val suffix = if (v.readOnly) ":ro" else ""
            "$source:${v.containerPath}$suffix"
        }

        val networkMode = when (s.networkMode) {
            NetworkMode.HOST -> "host"
            NetworkMode.NONE -> "none"
            NetworkMode.CUSTOM -> s.customNetwork.ifBlank { "bridge" }
            NetworkMode.BRIDGE -> "bridge"
        }

        val restartPolicy = when (s.restart) {
            RestartPolicy.NO -> DockerRestartPolicy(name = "no")
            RestartPolicy.ALWAYS -> DockerRestartPolicy(name = "always")
            RestartPolicy.ON_FAILURE -> DockerRestartPolicy(
                name = "on-failure",
                maximumRetryCount = s.restartRetries.toIntOrNull() ?: 0,
            )
            RestartPolicy.UNLESS_STOPPED -> DockerRestartPolicy(name = "unless-stopped")
        }

        val memory = parseMemLimit(s.memLimit)
        val nanoCpus = s.cpuShares.toLongOrNull()?.let { it * 1_000_000_000 / 1024 } ?: 0
        val cpuShares = s.cpuShares.toLongOrNull() ?: 0

        return ContainerCreateRequest(
            name = s.containerName.ifBlank { s.name },
            image = s.image.trim(),
            cmd = s.command.splitWhitespaceCommand(),
            entrypoint = s.entrypoint.splitWhitespaceCommand(),
            env = env,
            labels = labels,
            tty = s.tty,
            exposedPorts = exposedPorts,
            hostConfig = ContainerHostConfig(
                binds = binds,
                portBindings = portBindings,
                restartPolicy = restartPolicy,
                networkMode = networkMode,
                privileged = s.privileged,
                memory = memory,
                nanoCpus = nanoCpus,
                cpuShares = cpuShares,
                extraHosts = s.extraHosts.filter { it.isNotBlank() },
            ),
        )
    }

    private fun parseMemLimit(value: String): Long {
        val v = value.trim().lowercase()
        if (v.isEmpty()) return 0
        val num = v.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0
        return when {
            v.endsWith("g") -> (num * 1024 * 1024 * 1024).toLong()
            v.endsWith("m") -> (num * 1024 * 1024).toLong()
            v.endsWith("k") -> (num * 1024).toLong()
            v.endsWith("b") -> num.toLong()
            else -> num.toLong()
        }
    }

    private fun resolveRegistryAuth(image: String, client: DockerClient): RegistryCredential? {
        val registryHost = image.substringBefore('/', missingDelimiterValue = "").takeIf {
            it.contains('.') || it.contains(':') || it == "localhost"
        } ?: "docker.io"
        return client.server.registryAuths[registryHost]
            ?: if (registryHost == "docker.io") client.server.registryAuths["docker.io"] else null
    }

    /** 依据 depends_on 拓扑排序；无依赖关系的保持原顺序 */
    private fun topoSort(services: List<ServiceSpec>): List<ServiceSpec> {
        val result = mutableListOf<ServiceSpec>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(s: ServiceSpec) {
            if (s.name in visited) return
            if (s.name in visiting) return // 环形依赖：避免死循环
            visiting.add(s.name)
            s.dependsOn.forEach { dep ->
                services.firstOrNull { it.name == dep }?.let { visit(it) }
            }
            visiting.remove(s.name)
            visited.add(s.name)
            result.add(s)
        }

        services.forEach { visit(it) }
        return result
    }

    private fun String.splitWhitespaceCommand(): List<String>? {
        if (isBlank()) return null
        return split(Regex("""\s+(?=([^"]*"[^"]*")*[^"]*$)"""))
            .map { it.trim('"') }
            .filter { it.isNotBlank() }
    }
}
