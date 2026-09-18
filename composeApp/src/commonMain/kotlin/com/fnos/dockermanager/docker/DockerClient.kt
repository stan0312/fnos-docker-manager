package com.fnos.dockermanager.docker

import com.fnos.dockermanager.connection.RegistryCredential
import com.fnos.dockermanager.connection.ServerProfile
import com.fnos.dockermanager.createPlatformHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 基于 Docker Engine API (v1.24+) 的 REST 客户端。
 * 通过 http(s)://host:port 直连飞牛 NAS 上已开启远程访问的 Docker Daemon。
 */
class DockerClient(
    val server: ServerProfile,
    engine: HttpClientEngine = createPlatformHttpClientEngine(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val baseUrl: String = server.url.trimEnd('/')

    private val client = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            requestTimeoutMillis = 0
            socketTimeoutMillis = 0
        }
    }

    // ============ 基础 ============

    fun close() = client.close()

    suspend fun ping(): Boolean =
        runCatching { client.get("$baseUrl/_ping").status.isSuccess() }.getOrDefault(false)

    suspend fun version(): VersionInfo = getJson("/version")

    suspend fun info(): SystemInfo = getJson("/info")

    // ============ 容器 ============

    suspend fun listContainers(all: Boolean = true): List<ContainerSummary> =
        getJson("/containers/json") {
            parameter("all", all)
            parameter("size", false)
        }

    suspend fun inspectContainer(id: String): ContainerInspect = getJson("/containers/$id/json")

    suspend fun createContainer(request: ContainerCreateRequest): ContainerCreateResponse =
        postJson("/containers/create") {
            parameter("name", request.name)
            setBody(request)
        }

    suspend fun startContainer(id: String) = postEmpty("/containers/$id/start")
    suspend fun stopContainer(id: String, timeoutSeconds: Int = 10) =
        postEmpty("/containers/$id/stop") { parameter("t", timeoutSeconds) }
    suspend fun restartContainer(id: String, timeoutSeconds: Int = 10) =
        postEmpty("/containers/$id/restart") { parameter("t", timeoutSeconds) }
    suspend fun killContainer(id: String, signal: String = "SIGKILL") =
        postEmpty("/containers/$id/kill") { parameter("signal", signal) }
    suspend fun pauseContainer(id: String) = postEmpty("/containers/$id/pause")
    suspend fun unpauseContainer(id: String) = postEmpty("/containers/$id/unpause")

    suspend fun removeContainer(id: String, force: Boolean = false, removeVolumes: Boolean = false) =
        deleteEmpty("/containers/$id") {
            parameter("force", force)
            parameter("v", removeVolumes)
        }

    /**
     * 拉取容器日志。Docker 日志流可能使用 8 字节帧头的多路复用格式，此处自动解帧。
     */
    suspend fun containerLogs(
        id: String,
        tail: Int = 500,
        timestamps: Boolean = false,
        sinceSeconds: Long? = null,
        onChunk: (String) -> Unit,
    ) {
        client.prepareGet("$baseUrl/containers/$id/logs") {
            parameter("stdout", true)
            parameter("stderr", true)
            parameter("tail", tail.coerceAtLeast(1).toString())
            parameter("timestamps", timestamps)
            if (sinceSeconds != null) parameter("since", sinceSeconds.toString())
        }.execute { response ->
            throwIfError(response, "GET", "/containers/$id/logs")
            val raw = response.bodyAsChannel().toByteArraySafe()
            onChunk(demuxLogStream(raw))
        }
    }

    suspend fun containerStats(id: String): ContainerStats =
        getJson("/containers/$id/stats") { parameter("stream", false) }

    // ============ 镜像 ============

    suspend fun listImages(): List<ImageSummary> = getJson("/images/json") { parameter("all", false) }

    suspend fun inspectImage(id: String): ImageInspect = getJson("/images/$id/json")

    suspend fun deleteImage(id: String, force: Boolean = false): List<JsonElement> {
        val response = client.delete("$baseUrl/images/$id") {
            parameter("force", force)
        }
        throwIfError(response, "DELETE", "/images/$id")
        val text = response.bodyAsText()
        return if (text.isBlank()) emptyList()
        else runCatching { json.decodeFromString<List<JsonElement>>(text) }.getOrDefault(emptyList())
    }

    /** 拉取镜像，返回进度事件流（NDJSON 行） */
    fun pullImage(
        fromImage: String,
        tag: String = "latest",
        registryAuth: RegistryCredential? = null,
    ): Flow<PullProgressEvent> = flow {
        client.preparePost("$baseUrl/images/create") {
            parameter("fromImage", fromImage)
            parameter("tag", tag)
            registryAuth?.let { header("X-Registry-Auth", registryAuthHeader(it)) }
        }.execute { response ->
            throwIfError(response, "POST", "/images/create")
            response.bodyAsChannel().forEachLine { line ->
                if (line.isNotBlank()) {
                    val ev = runCatching { json.decodeFromString<PullProgressEvent>(line) }.getOrNull()
                    if (ev != null) emit(ev)
                }
            }
        }
    }

    suspend fun searchImages(
        term: String,
        registryAuth: RegistryCredential? = null,
        limit: Int = 50,
    ): List<ImageSearchItem> {
        val response = client.get("$baseUrl/images/search") {
            parameter("term", term)
            parameter("limit", limit)
            registryAuth?.let { header("X-Registry-Auth", registryAuthHeader(it)) }
        }
        throwIfError(response, "GET", "/images/search")
        return json.decodeFromString(response.bodyAsText())
    }

    /** 登录/校验注册表凭据（等价 docker login） */
    suspend fun registryLogin(auth: RegistryCredential): AuthResponse =
        postJson("/auth") { setBody(auth.toRegistryAuth()) }

    // ============ 网络 ============

    suspend fun listNetworks(): List<NetworkInspect> = getJson("/networks")

    suspend fun inspectNetwork(id: String): NetworkInspect = getJson("/networks/$id")

    suspend fun createNetwork(request: NetworkCreateRequest): NetworkCreateResponse =
        postJson("/networks/create") { setBody(request) }

    suspend fun deleteNetwork(id: String) = deleteEmpty("/networks/$id")

    suspend fun connectContainerToNetwork(networkId: String, containerId: String) =
        postEmpty("/networks/$networkId/connect") {
            setBody(NetworkConnectRequest(container = containerId))
        }

    suspend fun disconnectContainerFromNetwork(networkId: String, containerId: String, force: Boolean = false) =
        postEmpty("/networks/$networkId/disconnect") {
            parameter("force", force)
            setBody(buildJsonObject { put("Container", containerId) })
        }

    // ============ 卷 ============

    suspend fun listVolumes(): List<VolumeInspect> {
        val response = client.get("$baseUrl/volumes")
        throwIfError(response, "GET", "/volumes")
        val parsed = json.decodeFromString<VolumeListResponse>(response.bodyAsText())
        return parsed.volumes.orEmpty()
    }

    suspend fun createVolume(request: VolumeCreateRequest): VolumeInspect =
        postJson("/volumes/create") { setBody(request) }

    suspend fun deleteVolume(name: String, force: Boolean = false) =
        deleteEmpty("/volumes/$name") { parameter("force", force) }

    // ============ 事件 ============

    fun events(sinceSeconds: Long? = null): Flow<DockerEvent> = flow {
        client.prepareGet("$baseUrl/events") {
            if (sinceSeconds != null) parameter("since", sinceSeconds.toString())
        }.execute { response ->
            throwIfError(response, "GET", "/events")
            response.bodyAsChannel().forEachLine { line ->
                if (line.isNotBlank()) {
                    val ev = runCatching { json.decodeFromString<DockerEvent>(line) }.getOrNull()
                    if (ev != null) emit(ev)
                }
            }
        }
    }

    // ============ 内部工具 ============

    private suspend inline fun <reified T> getJson(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = client.get("$baseUrl$path") { block() }
        throwIfError(response, "GET", path)
        return response.body()
    }

    private suspend inline fun <reified T> postJson(
        path: String,
        crossinline block: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            block()
        }
        throwIfError(response, "POST", path)
        return response.body()
    }

    private suspend fun postEmpty(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) {
        val response = client.post("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            block()
        }
        throwIfError(response, "POST", path)
    }

    private suspend fun deleteEmpty(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) {
        val response = client.delete("$baseUrl$path") {
            contentType(ContentType.Application.Json)
            block()
        }
        throwIfError(response, "DELETE", path)
    }

    private suspend fun throwIfError(response: HttpResponse, method: String, path: String) {
        if (!response.status.isSuccess()) {
            val text = runCatching { response.bodyAsText() }.getOrDefault("")
            val message = runCatching {
                json.decodeFromString<DockerErrorBody>(text).message
            }.getOrDefault(text)
            throw DockerApiException(response.status.value, method, path, message)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun registryAuthHeader(auth: RegistryCredential): String {
        val jsonStr = json.encodeToString(auth.toRegistryAuth())
        return Base64.encode(jsonStr.encodeToByteArray())
    }

    private fun RegistryCredential.toRegistryAuth() = RegistryAuth(
        username = username,
        password = password,
        email = email,
        serverAddress = serverAddress,
    )

    companion object {
        const val DEFAULT_PORT = 2375
        const val TLS_PORT = 2376

        /** 将 8 字节帧头多路复用流还原为纯文本；非多路复用流原样返回 */
        fun demuxLogStream(bytes: ByteArray): String {
            if (bytes.size < 8) return bytes.decodeToString()
            // 多路复用帧头: [type(1)][0,0,0][size(4, big-endian)]
            val firstType = bytes[0].toInt() and 0xff
            val headerLooksValid = firstType in 0..2 &&
                bytes[1] == 0.toByte() && bytes[2] == 0.toByte() && bytes[3] == 0.toByte()
            if (!headerLooksValid) return bytes.decodeToString()

            val out = ArrayList<Byte>(bytes.size)
            var pos = 0
            var headerOk = true
            while (pos + 8 <= bytes.size && headerOk) {
                val frameSize =
                    ((bytes[pos + 4].toInt() and 0xff) shl 24) or
                        ((bytes[pos + 5].toInt() and 0xff) shl 16) or
                        ((bytes[pos + 6].toInt() and 0xff) shl 8) or
                        (bytes[pos + 7].toInt() and 0xff)
                pos += 8
                if (frameSize < 0 || pos + frameSize > bytes.size) {
                    headerOk = false
                    break
                }
                for (i in 0 until frameSize) out.add(bytes[pos + i])
                pos += frameSize
            }
            if (!headerOk) return bytes.decodeToString()
            return out.toByteArray().decodeToString()
        }
    }
}

/** 流式读取通道为字节数组（KMP 安全，不依赖 java.io） */
private suspend fun ByteReadChannel.toByteArraySafe(): ByteArray {
    val buffer = ByteArray(8192)
    val out = ArrayList<Byte>(8192)
    while (true) {
        val n = readAvailable(buffer)
        if (n < 0) break
        if (n > 0) for (i in 0 until n) out.add(buffer[i])
    }
    return out.toByteArray()
}

/** 逐行回调通道内容（按 \n 切分，去掉 \r） */
private suspend fun ByteReadChannel.forEachLine(onLine: suspend (String) -> Unit) {
    val buffer = ByteArray(8192)
    var partial = ArrayList<Byte>(256)
    while (true) {
        val n = readAvailable(buffer)
        if (n < 0) break
        if (n == 0) continue
        for (i in 0 until n) {
            val b = buffer[i]
            if (b == '\n'.code.toByte()) {
                if (partial.isNotEmpty()) {
                    val line = partial.toByteArray().decodeToString().trimEnd('\r')
                    if (line.isNotEmpty()) onLine(line)
                    partial = ArrayList(256)
                }
            } else {
                partial.add(b)
            }
        }
    }
    if (partial.isNotEmpty()) {
        val line = partial.toByteArray().decodeToString().trimEnd('\r')
        if (line.isNotEmpty()) onLine(line)
    }
}
