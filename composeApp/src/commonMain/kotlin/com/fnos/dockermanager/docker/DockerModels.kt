package com.fnos.dockermanager.docker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// ============ System ============

@Serializable
data class SystemInfo(
    @SerialName("ID") val id: String? = null,
    @SerialName("Name") val name: String? = null,
    @SerialName("ServerVersion") val serverVersion: String? = null,
    @SerialName("ApiVersion") val apiVersion: String? = null,
    @SerialName("OperatingSystem") val operatingSystem: String? = null,
    @SerialName("OSVersion") val osVersion: String? = null,
    @SerialName("KernelVersion") val kernelVersion: String? = null,
    @SerialName("Architecture") val architecture: String? = null,
    @SerialName("NCPU") val ncpu: Long = 0,
    @SerialName("MemTotal") val memTotal: Long = 0,
    @SerialName("DockerRootDir") val dockerRootDir: String? = null,
    @SerialName("Containers") val containers: Long = 0,
    @SerialName("ContainersRunning") val containersRunning: Long = 0,
    @SerialName("ContainersPaused") val containersPaused: Long = 0,
    @SerialName("ContainersStopped") val containersStopped: Long = 0,
    @SerialName("Images") val images: Long = 0,
    @SerialName("Driver") val driver: String? = null,
    @SerialName("CgroupVersion") val cgroupVersion: String? = null,
)

@Serializable
data class VersionInfo(
    @SerialName("Version") val version: String? = null,
    @SerialName("ApiVersion") val apiVersion: String? = null,
    @SerialName("MinAPIVersion") val minApiVersion: String? = null,
    @SerialName("GitCommit") val gitCommit: String? = null,
    @SerialName("GoVersion") val goVersion: String? = null,
    @SerialName("Os") val os: String? = null,
    @SerialName("Arch") val arch: String? = null,
    @SerialName("KernelVersion") val kernelVersion: String? = null,
)

// ============ Containers ============

@Serializable
data class PortBindingSummary(
    @SerialName("IP") val ip: String? = null,
    @SerialName("PrivatePort") val privatePort: Int = 0,
    @SerialName("PublicPort") val publicPort: Int = 0,
    @SerialName("Type") val type: String = "tcp",
)

@Serializable
data class ContainerSummary(
    @SerialName("Id") val id: String,
    @SerialName("Names") val names: List<String> = emptyList(),
    @SerialName("Image") val image: String = "",
    @SerialName("ImageID") val imageId: String = "",
    @SerialName("Command") val command: String = "",
    @SerialName("Created") val created: Long = 0,
    @SerialName("State") val state: String = "unknown",
    @SerialName("Status") val status: String = "",
    @SerialName("Ports") val ports: List<PortBindingSummary> = emptyList(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("SizeRw") val sizeRw: Long? = null,
    @SerialName("SizeRootFs") val sizeRootFs: Long? = null,
) {
    val displayName: String
        get() = names.firstOrNull()?.removePrefix("/") ?: id.take(12)
}

@Serializable
data class ContainerState(
    @SerialName("Status") val status: String = "",
    @SerialName("Running") val running: Boolean = false,
    @SerialName("Paused") val paused: Boolean = false,
    @SerialName("Restarting") val restarting: Boolean = false,
    @SerialName("OOMKilled") val oomKilled: Boolean = false,
    @SerialName("Dead") val dead: Boolean = false,
    @SerialName("Pid") val pid: Int = 0,
    @SerialName("ExitCode") val exitCode: Int = 0,
    @SerialName("Error") val error: String = "",
    @SerialName("StartedAt") val startedAt: String = "",
    @SerialName("FinishedAt") val finishedAt: String = "",
    @SerialName("Health") val health: HealthState? = null,
)

@Serializable
data class HealthState(
    @SerialName("Status") val status: String = "",
    @SerialName("FailingStreak") val failingStreak: Int = 0,
    @SerialName("Log") val log: List<HealthLog> = emptyList(),
)

@Serializable
data class HealthLog(
    @SerialName("Start") val start: String = "",
    @SerialName("End") val end: String = "",
    @SerialName("ExitCode") val exitCode: Int = 0,
    @SerialName("Output") val output: String = "",
)

@Serializable
data class RestartPolicy(
    @SerialName("Name") val name: String = "",
    @SerialName("MaximumRetryCount") val maximumRetryCount: Int = 0,
)

@Serializable
data class ContainerConfig(
    @SerialName("Hostname") val hostname: String? = null,
    @SerialName("Image") val image: String = "",
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String>? = null,
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("Tty") val tty: Boolean = false,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, JsonElement> = emptyMap(),
    @SerialName("WorkingDir") val workingDir: String? = null,
    @SerialName("User") val user: String? = null,
)

@Serializable
data class HostConfigInfo(
    @SerialName("Binds") val binds: List<String>? = null,
    @SerialName("NetworkMode") val networkMode: String = "",
    @SerialName("RestartPolicy") val restartPolicy: RestartPolicy = RestartPolicy(),
    @SerialName("Privileged") val privileged: Boolean = false,
    @SerialName("Memory") val memory: Long = 0,
    @SerialName("NanoCpus") val nanoCpus: Long = 0,
    @SerialName("CpuShares") val cpuShares: Long = 0,
    @SerialName("ExtraHosts") val extraHosts: List<String>? = null,
)

@Serializable
data class NetworkEndpoint(
    @SerialName("IPAddress") val ipAddress: String = "",
    @SerialName("GlobalIPv6Address") val globalIpv6Address: String = "",
    @SerialName("MacAddress") val macAddress: String = "",
)

@Serializable
data class ContainerNetworks(
    @SerialName("Networks") val networks: Map<String, NetworkEndpoint> = emptyMap(),
)

@Serializable
data class MountPoint(
    @SerialName("Type") val type: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Source") val source: String = "",
    @SerialName("Destination") val destination: String = "",
    @SerialName("Driver") val driver: String = "",
    @SerialName("Mode") val mode: String = "",
    @SerialName("RW") val rw: Boolean = true,
    @SerialName("Propagation") val propagation: String = "",
)

@Serializable
data class ContainerInspect(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Created") val created: String = "",
    @SerialName("State") val state: ContainerState = ContainerState(),
    @SerialName("Config") val config: ContainerConfig = ContainerConfig(),
    @SerialName("HostConfig") val hostConfig: HostConfigInfo = HostConfigInfo(),
    @SerialName("NetworkSettings") val networkSettings: ContainerNetworks = ContainerNetworks(),
    @SerialName("Mounts") val mounts: List<MountPoint> = emptyList(),
    @SerialName("Image") val image: String = "",
    @SerialName("RestartCount") val restartCount: Int = 0,
) {
    val displayName: String get() = name.removePrefix("/").ifEmpty { id.take(12) }
}

// ============ Container create ============

@Serializable
data class PortBinding(
    @SerialName("HostIp") val hostIp: String = "",
    @SerialName("HostPort") val hostPort: String = "",
)

@Serializable
data class ContainerCreateRequest(
    @SerialName("name") val name: String,
    @SerialName("Image") val image: String,
    @SerialName("Cmd") val cmd: List<String>? = null,
    @SerialName("Entrypoint") val entrypoint: List<String>? = null,
    @SerialName("Env") val env: List<String> = emptyList(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("Tty") val tty: Boolean = false,
    @SerialName("ExposedPorts") val exposedPorts: Map<String, JsonElement> = emptyMap(),
    @SerialName("HostConfig") val hostConfig: ContainerHostConfig = ContainerHostConfig(),
    @SerialName("NetworkingConfig") val networkingConfig: NetworkingConfig? = null,
)

@Serializable
data class ContainerHostConfig(
    @SerialName("Binds") val binds: List<String> = emptyList(),
    @SerialName("PortBindings") val portBindings: Map<String, List<PortBinding>> = emptyMap(),
    @SerialName("RestartPolicy") val restartPolicy: RestartPolicy = RestartPolicy(name = "no"),
    @SerialName("NetworkMode") val networkMode: String = "bridge",
    @SerialName("Privileged") val privileged: Boolean = false,
    @SerialName("Memory") val memory: Long = 0,
    @SerialName("NanoCpus") val nanoCpus: Long = 0,
    @SerialName("CpuShares") val cpuShares: Long = 0,
    @SerialName("ExtraHosts") val extraHosts: List<String> = emptyList(),
)

@Serializable
data class NetworkingConfig(
    @SerialName("EndpointsConfig") val endpointsConfig: Map<String, EndpointConfig> = emptyMap(),
)

@Serializable
data class EndpointConfig(
    @SerialName("Aliases") val aliases: List<String> = emptyList(),
)

@Serializable
data class ContainerCreateResponse(
    @SerialName("Id") val id: String = "",
    @SerialName("Warnings") val warnings: List<String> = emptyList(),
)

// ============ Images ============

@Serializable
data class ImageSummary(
    @SerialName("Id") val id: String,
    @SerialName("RepoTags") val repoTags: List<String>? = null,
    @SerialName("RepoDigests") val repoDigests: List<String>? = null,
    @SerialName("Created") val created: Long = 0,
    @SerialName("Size") val size: Long = 0,
    @SerialName("SharedSize") val sharedSize: Long = 0,
    @SerialName("VirtualSize") val virtualSize: Long = 0,
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("Containers") val containers: Long = 0,
) {
    val tags: List<String> get() = repoTags.orEmpty().filter { it != "<none>:<none>" }
    val digestTags: List<String> get() = repoDigests.orEmpty().filter { it != "<none>@<none>" }
    val shortId: String get() = id.removePrefix("sha256:").take(12)
    val displayName: String
        get() = tags.firstOrNull()
            ?: digestTags.firstOrNull()?.substringBefore('@')
            ?: id.removePrefix("sha256:").take(12)
}

@Serializable
data class ImageInspect(
    @SerialName("Id") val id: String = "",
    @SerialName("RepoTags") val repoTags: List<String>? = null,
    @SerialName("RepoDigests") val repoDigests: List<String>? = null,
    @SerialName("Created") val created: String = "",
    @SerialName("Size") val size: Long = 0,
    @SerialName("Architecture") val architecture: String = "",
    @SerialName("Os") val os: String = "",
    @SerialName("Config") val config: ContainerConfig = ContainerConfig(),
    @SerialName("Author") val author: String = "",
    @SerialName("DockerVersion") val dockerVersion: String = "",
    @SerialName("VirtualSize") val virtualSize: Long = 0,
)

@Serializable
data class ImageSearchItem(
    @SerialName("description") val description: String = "",
    @SerialName("is_official") val isOfficial: Boolean = false,
    @SerialName("is_automated") val isAutomated: Boolean = false,
    @SerialName("name") val name: String = "",
    @SerialName("star_count") val starCount: Int = 0,
)

@Serializable
data class PullProgressEvent(
    @SerialName("status") val status: String? = null,
    @SerialName("progressDetail") val progressDetail: JsonObject? = null,
    @SerialName("progress") val progress: String? = null,
    @SerialName("id") val id: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("stream") val stream: String? = null,
)

// ============ Networks ============

@Serializable
data class IpamConfig(
    @SerialName("Subnet") val subnet: String? = null,
    @SerialName("Gateway") val gateway: String? = null,
    @SerialName("IPRange") val ipRange: String? = null,
)

@Serializable
data class Ipam(
    @SerialName("Driver") val driver: String = "default",
    @SerialName("Config") val config: List<IpamConfig> = emptyList(),
    @SerialName("Options") val options: Map<String, String> = emptyMap(),
)

@Serializable
data class NetworkContainerInfo(
    @SerialName("Name") val name: String = "",
    @SerialName("EndpointID") val endpointId: String = "",
    @SerialName("MacAddress") val macAddress: String = "",
    @SerialName("IPv4Address") val ipv4Address: String = "",
    @SerialName("IPv6Address") val ipv6Address: String = "",
)

@Serializable
data class NetworkInspect(
    @SerialName("Name") val name: String = "",
    @SerialName("Id") val id: String = "",
    @SerialName("Created") val created: String = "",
    @SerialName("Scope") val scope: String = "",
    @SerialName("Driver") val driver: String = "",
    @SerialName("EnableIPv6") val enableIpv6: Boolean = false,
    @SerialName("IPAM") val ipam: Ipam = Ipam(),
    @SerialName("Internal") val internal: Boolean = false,
    @SerialName("Attachable") val attachable: Boolean = false,
    @SerialName("Ingress") val ingress: Boolean = false,
    @SerialName("ConfigFrom") val configFrom: JsonElement? = null,
    @SerialName("ConfigOnly") val configOnly: Boolean = false,
    @SerialName("Containers") val containers: Map<String, NetworkContainerInfo> = emptyMap(),
    @SerialName("Options") val options: Map<String, String> = emptyMap(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class NetworkCreateRequest(
    @SerialName("Name") val name: String,
    @SerialName("Driver") val driver: String = "bridge",
    @SerialName("Internal") val internal: Boolean = false,
    @SerialName("Attachable") val attachable: Boolean = false,
    @SerialName("EnableIPv6") val enableIpv6: Boolean = false,
    @SerialName("IPAM") val ipam: Ipam = Ipam(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
)

@Serializable
data class NetworkCreateResponse(
    @SerialName("Id") val id: String = "",
    @SerialName("Warning") val warning: String = "",
)

@Serializable
data class NetworkConnectRequest(
    @SerialName("Container") val container: String,
    @SerialName("EndpointConfig") val endpointConfig: EndpointConfig = EndpointConfig(),
)

// ============ Volumes ============

@Serializable
data class VolumeInspect(
    @SerialName("Name") val name: String = "",
    @SerialName("Driver") val driver: String = "",
    @SerialName("Mountpoint") val mountpoint: String = "",
    @SerialName("CreatedAt") val createdAt: String = "",
    @SerialName("Status") val status: JsonElement? = null,
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
    @SerialName("Scope") val scope: String = "",
    @SerialName("Options") val options: Map<String, String> = emptyMap(),
)

@Serializable
data class VolumeListResponse(
    @SerialName("Volumes") val volumes: List<VolumeInspect>? = null,
    @SerialName("Warnings") val warnings: List<String>? = null,
)

@Serializable
data class VolumeCreateRequest(
    @SerialName("Name") val name: String,
    @SerialName("Driver") val driver: String = "local",
    @SerialName("DriverOpts") val driverOpts: Map<String, String> = emptyMap(),
    @SerialName("Labels") val labels: Map<String, String> = emptyMap(),
)

// ============ Registry auth ============

@Serializable
data class RegistryAuth(
    @SerialName("username") val username: String = "",
    @SerialName("password") val password: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("serveraddress") val serverAddress: String = "",
    @SerialName("identitytoken") val identityToken: String = "",
)

@Serializable
data class AuthResponse(
    @SerialName("Status") val status: String = "",
    @SerialName("IdentityToken") val identityToken: String = "",
)

// ============ Stats ============

@Serializable
data class CpuUsage(
    @SerialName("total_usage") val totalUsage: Long = 0,
    @SerialName("percpu_usage") val percpuUsage: List<Long> = emptyList(),
    @SerialName("usage_in_kernelmode") val usageInKernelmode: Long = 0,
    @SerialName("usage_in_usermode") val usageInUsermode: Long = 0,
)

@Serializable
data class CpuStats(
    @SerialName("cpu_usage") val cpuUsage: CpuUsage = CpuUsage(),
    @SerialName("system_cpu_usage") val systemCpuUsage: Long = 0,
    @SerialName("online_cpus") val onlineCpus: Long = 0,
    @SerialName("throttling_data") val throttlingData: JsonElement? = null,
)

@Serializable
data class MemoryStats(
    @SerialName("usage") val usage: Long = 0,
    @SerialName("limit") val limit: Long = 0,
    @SerialName("stats") val stats: JsonElement? = null,
)

@Serializable
data class NetworkStat(
    @SerialName("rx_bytes") val rxBytes: Long = 0,
    @SerialName("tx_bytes") val txBytes: Long = 0,
)

@Serializable
data class BlkioStat(
    @SerialName("io_service_bytes_recursive") val ioServiceBytesRecursive: List<JsonElement>? = null,
)

@Serializable
data class ContainerStats(
    @SerialName("read") val read: String = "",
    @SerialName("preread") val preread: String = "",
    @SerialName("cpu_stats") val cpuStats: CpuStats = CpuStats(),
    @SerialName("precpu_stats") val precpuStats: CpuStats = CpuStats(),
    @SerialName("memory_stats") val memoryStats: MemoryStats = MemoryStats(),
    @SerialName("networks") val networks: Map<String, NetworkStat> = emptyMap(),
    @SerialName("pids_stats") val pidsStats: JsonElement? = null,
    @SerialName("blkio_stats") val blkioStats: BlkioStat = BlkioStat(),
)

// ============ Events ============

@Serializable
data class DockerEvent(
    @SerialName("Type") val type: String = "",
    @SerialName("Action") val action: String = "",
    @SerialName("Actor") val actor: EventActor = EventActor(),
    @SerialName("time") val time: Long = 0,
    @SerialName("timeNano") val timeNano: Long = 0,
)

@Serializable
data class EventActor(
    @SerialName("ID") val id: String = "",
    @SerialName("Attributes") val attributes: Map<String, String> = emptyMap(),
)

// ============ Errors ============

@Serializable
data class DockerErrorBody(
    @SerialName("message") val message: String = "",
)

class DockerApiException(
    val statusCode: Int,
    val method: String,
    val path: String,
    val body: String,
) : Exception("Docker API $method $path 失败 (HTTP $statusCode): ${body.take(300)}")

class DockerConnectionException(
    val host: String,
    cause: Throwable,
) : Exception("无法连接 $host: ${cause.message ?: cause::class.simpleName}", cause)
