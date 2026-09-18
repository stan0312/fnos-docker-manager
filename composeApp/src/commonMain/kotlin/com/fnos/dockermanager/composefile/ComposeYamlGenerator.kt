package com.fnos.dockermanager.composefile

/**
 * 将可视化编排模型生成为标准 docker-compose YAML。
 */
object ComposeYamlGenerator {

    fun generate(project: ComposeProject): String {
        val sb = StringBuilder(2048)
        sb.append("# 由飞牛管家可视化编排生成\n")
        if (project.name.isNotBlank()) sb.appendLine("name: ${scalar(project.name)}")
        sb.appendLine("services:")
        if (project.services.isEmpty()) {
            sb.appendLine("  # （暂无服务）")
        }
        project.services.forEach { s -> appendService(sb, s) }

        if (project.networks.isNotEmpty()) {
            sb.appendLine("networks:")
            project.networks.forEach { n -> appendNetwork(sb, n) }
        }
        if (project.volumes.isNotEmpty()) {
            sb.appendLine("volumes:")
            project.volumes.forEach { v -> appendVolume(sb, v) }
        }
        return sb.toString().trimEnd() + "\n"
    }

    private fun appendService(sb: StringBuilder, s: ServiceSpec) {
        sb.appendLine("  ${s.name}:")
        if (s.image.isNotBlank()) sb.appendLine("    image: ${scalar(s.image)}")
        if (s.containerName.isNotBlank()) sb.appendLine("    container_name: ${scalar(s.containerName)}")
        if (s.command.isNotBlank()) sb.appendLine("    command: ${scalar(s.command)}")
        if (s.entrypoint.isNotBlank()) sb.appendLine("    entrypoint: ${scalar(s.entrypoint)}")
        if (s.tty) sb.appendLine("    tty: true")
        if (s.privileged) sb.appendLine("    privileged: true")
        if (s.memLimit.isNotBlank()) sb.appendLine("    mem_limit: ${scalar(s.memLimit)}")
        if (s.cpuShares.isNotBlank()) sb.appendLine("    cpu_shares: ${scalar(s.cpuShares)}")

        // 重启策略
        when (s.restart) {
            RestartPolicy.NO -> Unit
            RestartPolicy.ON_FAILURE -> {
                val retries = s.restartRetries.trim()
                sb.appendLine(if (retries.isNotEmpty()) "    restart: ${scalar("on-failure:$retries")}" else "    restart: on-failure")
            }
            else -> sb.appendLine("    restart: ${s.restart.yamlValue}")
        }

        // 端口
        val validPorts = s.ports.filter { it.isComplete }
        if (validPorts.isNotEmpty()) {
            sb.appendLine("    ports:")
            validPorts.forEach { p ->
                val host = p.hostIp.takeIf { it.isNotBlank() }
                val proto = if (p.protocol == "udp") "/udp" else ""
                val entry = buildString {
                    if (host != null) append(host).append(':')
                    append(p.hostPort).append(':').append(p.containerPort).append(proto)
                }
                sb.appendLine("      - ${scalar(entry)}")
            }
        }

        // 卷/存储挂载
        val validVolumes = s.volumes.filter { it.isComplete }
        if (validVolumes.isNotEmpty()) {
            sb.appendLine("    volumes:")
            validVolumes.forEach { v ->
                val source = if (v.type == VolumeType.VOLUME) v.name else v.hostPath
                val entry = buildString {
                    append(source).append(':').append(v.containerPath)
                    if (v.readOnly) append(":ro")
                }
                sb.appendLine("      - ${scalar(entry)}")
            }
        }

        // 环境变量
        val validEnv = s.environment.filter { it.key.isNotBlank() }
        if (validEnv.isNotEmpty()) {
            sb.appendLine("    environment:")
            validEnv.forEach { kv -> sb.appendLine("      - ${scalar("${kv.key}=${kv.value}")}") }
        }

        // 标签
        val validLabels = s.labels.filter { it.key.isNotBlank() }
        if (validLabels.isNotEmpty()) {
            sb.appendLine("    labels:")
            validLabels.forEach { kv -> sb.appendLine("      - ${scalar("${kv.key}=${kv.value}")}") }
        }

        // 网络
        when (s.networkMode) {
            NetworkMode.HOST -> sb.appendLine("    network_mode: host")
            NetworkMode.NONE -> sb.appendLine("    network_mode: none")
            NetworkMode.CUSTOM -> {
                val netName = s.customNetwork.takeIf { it.isNotBlank() }
                if (netName != null) {
                    sb.appendLine("    networks:")
                    sb.appendLine("      - ${netName}")
                } else {
                    sb.appendLine("    network_mode: bridge")
                }
            }
            NetworkMode.BRIDGE -> Unit
        }

        // 依赖
        if (s.dependsOn.isNotEmpty()) {
            sb.appendLine("    depends_on:")
            s.dependsOn.forEach { d -> sb.appendLine("      - ${d}") }
        }

        // extra_hosts
        val validHosts = s.extraHosts.filter { it.isNotBlank() }
        if (validHosts.isNotEmpty()) {
            sb.appendLine("    extra_hosts:")
            validHosts.forEach { h -> sb.appendLine("      - ${scalar(h)}") }
        }
    }

    private fun appendNetwork(sb: StringBuilder, n: NetworkSpec) {
        sb.appendLine("  ${n.name}:")
        if (n.external) {
            sb.appendLine("    external: true")
            return
        }
        if (n.driver.isNotBlank()) sb.appendLine("    driver: ${scalar(n.driver)}")
        if (n.internal) sb.appendLine("    internal: true")
        if (n.subnet.isNotBlank() || n.gateway.isNotBlank()) {
            sb.appendLine("    ipam:")
            sb.appendLine("      config:")
            sb.appendLine("        - subnet: ${scalar(n.subnet)}")
            if (n.gateway.isNotBlank()) sb.appendLine("          gateway: ${scalar(n.gateway)}")
        }
    }

    private fun appendVolume(sb: StringBuilder, v: NamedVolumeSpec) {
        sb.appendLine("  ${v.name}:")
        if (v.external) {
            sb.appendLine("    external: true")
            return
        }
        if (v.driver.isNotBlank()) sb.appendLine("    driver: ${scalar(v.driver)}")
    }

    /** YAML 标量：需要时加引号，避免被解析为数字/布尔 */
    fun scalar(value: String): String {
        val v = value.trim()
        if (v.isEmpty()) return "\"\""
        val looksPlain = Regex("""^[A-Za-z0-9_./:+-]+$""").matches(v) &&
            !Regex("""^(true|false|null|yes|no|on|off|~)$""", RegexOption.IGNORE_CASE).matches(v) &&
            !Regex("""^[-+]?[0-9.]+$""").matches(v) &&
            !v.startsWith(":") && !v.endsWith(":")
        if (looksPlain) return v
        return "\"" + escapeDoubleQuoted(v) + "\""
    }

    private fun escapeDoubleQuoted(v: String): String {
        val sb = StringBuilder(v.length + 8)
        for (c in v) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\t' -> sb.append("\\t")
                '\r' -> sb.append("\\r")
                else -> {
                    if (c.code < 0x20) sb.append("\\u").append(hex4(c.code))
                    else sb.append(c)
                }
            }
        }
        return sb.toString()
    }

    /** 4 位小写十六进制（跨平台，避免 String.format） */
    private fun hex4(n: Int): String {
        val chars = "0123456789abcdef"
        return buildString(4) {
            append(chars[(n shr 12) and 0xF])
            append(chars[(n shr 8) and 0xF])
            append(chars[(n shr 4) and 0xF])
            append(chars[n and 0xF])
        }
    }
}
