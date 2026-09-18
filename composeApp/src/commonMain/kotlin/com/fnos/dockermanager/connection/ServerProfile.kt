package com.fnos.dockermanager.connection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RegistryCredential(
    val serverAddress: String = "docker.io",
    val username: String = "",
    val password: String = "",
    val email: String = "",
) {
    val displayServer: String
        get() = serverAddress.ifBlank { "docker.io" }
}

@Serializable
data class ServerProfile(
    val id: String = "",
    val name: String = "",
    val host: String = "",
    val port: Int = 2375,
    val useTls: Boolean = false,
    val registryAuths: Map<String, RegistryCredential> = emptyMap(),
) {
    val url: String
        get() = "${if (useTls) "https" else "http"}://$host:$port"

    val isComplete: Boolean
        get() = name.isNotBlank() && host.isNotBlank() && port in 1..65535
}

object ServerProfileJson {
    val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
}
