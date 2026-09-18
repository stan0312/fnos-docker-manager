package com.fnos.dockermanager.composefile

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar

/**
 * 将已有 docker-compose YAML 解析为可视化编排模型（支持导入后继续编辑）。
 * 基于 kaml 的 YamlNode API（kaml 0.10x 不再提供 parseToJsonElement）。
 */
object ComposeYamlParser {

    fun parse(text: String): ComposeProject {
        val rootNode = runCatching { Yaml.default.parseToYamlNode(text) }.getOrNull()
        val root = rootNode as? YamlMap ?: return ComposeProject(name = "")
        val project = ComposeProject(name = root.str("name") ?: "")

        root.map("services")?.entries?.forEach { (name, el) ->
            val s = el as? YamlMap ?: return@forEach
            project.services.add(parseService(name.content, s))
        }

        root.map("networks")?.entries?.forEach { (name, el) ->
            val n = el as? YamlMap ?: return@forEach
            project.networks.add(parseNetwork(name.content, n))
        }

        root.map("volumes")?.entries?.forEach { (name, el) ->
            val v = el as? YamlMap ?: return@forEach
            project.volumes.add(parseVolume(name.content, v))
        }

        // 服务里挂载/依赖的命名卷与网络如果没有在顶层定义，自动补全
        project.services.forEach { s ->
            s.volumes.filter { it.type == VolumeType.VOLUME }.forEach { v ->
                if (v.name.isNotBlank() && project.volumes.none { it.name == v.name }) {
                    project.volumes.add(NamedVolumeSpec(name = v.name))
                }
            }
            if (s.networkMode == NetworkMode.CUSTOM && s.customNetwork.isNotBlank() &&
                project.networks.none { it.name == s.customNetwork }
            ) {
                project.networks.add(NetworkSpec(name = s.customNetwork))
            }
        }

        return project
    }

    private fun parseService(name: String, o: YamlMap): ServiceSpec {
        val s = ServiceSpec(name = name)
        s.image = o.str("image") ?: ""
        s.containerName = o.str("container_name") ?: ""
        s.command = o.toCmdStr("command") ?: ""
        s.entrypoint = o.toCmdStr("entrypoint") ?: ""
        s.restart = RestartPolicy.fromYaml(o.str("restart"))
        val restartRaw = o.str("restart") ?: ""
        if (restartRaw.startsWith("on-failure:")) s.restartRetries = restartRaw.substringAfter(':').trim()
        s.tty = o.bool("tty") ?: false
        s.privileged = o.bool("privileged") ?: false
        s.memLimit = o.str("mem_limit") ?: ""
        s.cpuShares = o.str("cpu_shares") ?: ""

        // 端口（短语法 / 长语法）
        o.list("ports")?.items?.forEach { el ->
            val p = (el as? YamlScalar)?.content?.let { parsePortShort(it) }
                ?: (el as? YamlMap)?.let { po ->
                    PortMapping(
                        hostIp = "",
                        hostPort = po.str("published") ?: "",
                        containerPort = po.str("target") ?: "",
                        protocol = po.str("protocol") ?: "tcp",
                    )
                }
            if (p != null && p.containerPort.isNotBlank()) s.ports.add(p)
        }

        // 卷（短语法 / 长语法）
        o.list("volumes")?.items?.forEach { el ->
            val v = (el as? YamlScalar)?.content?.let { parseVolumeShort(it) }
                ?: (el as? YamlMap)?.let { vo ->
                    val type = vo.str("type") ?: "volume"
                    VolumeMount(
                        type = if (type == "bind") VolumeType.BIND else VolumeType.VOLUME,
                        name = vo.str("source") ?: "",
                        hostPath = vo.str("source") ?: "",
                        containerPath = vo.str("target") ?: "",
                        readOnly = vo.bool("read_only") ?: false,
                    )
                }
            if (v != null && v.containerPath.isNotBlank()) s.volumes.add(v)
        }

        // 环境变量（map 或 list）
        when (val env = o.get<YamlNode>("environment")) {
            is YamlMap -> env.entries.forEach { (k, v) ->
                s.environment.add(KeyValue(k.content, scalarText(v)))
            }
            is YamlList -> env.items.forEach { el ->
                (el as? YamlScalar)?.content?.let { line ->
                    val idx = line.indexOf('=')
                    if (idx > 0) s.environment.add(KeyValue(line.substring(0, idx), line.substring(idx + 1)))
                }
            }
            else -> Unit
        }

        // 标签
        when (val labels = o.get<YamlNode>("labels")) {
            is YamlMap -> labels.entries.forEach { (k, v) ->
                s.labels.add(KeyValue(k.content, scalarText(v)))
            }
            is YamlList -> labels.items.forEach { el ->
                (el as? YamlScalar)?.content?.let { line ->
                    val idx = line.indexOf('=')
                    if (idx > 0) s.labels.add(KeyValue(line.substring(0, idx), line.substring(idx + 1)))
                }
            }
            else -> Unit
        }

        // 网络
        when (o.str("network_mode")) {
            "host" -> s.networkMode = NetworkMode.HOST
            "none" -> s.networkMode = NetworkMode.NONE
            else -> {
                val nets = o.get<YamlNode>("networks")
                val first = when (nets) {
                    is YamlMap -> nets.entries.keys.firstOrNull()?.content
                    is YamlList -> (nets.items.firstOrNull() as? YamlScalar)?.content
                    else -> null
                }
                if (first != null) {
                    s.networkMode = NetworkMode.CUSTOM
                    s.customNetwork = first
                }
            }
        }

        // depends_on（list 或 map）
        when (val dep = o.get<YamlNode>("depends_on")) {
            is YamlList -> dep.items.forEach { el ->
                (el as? YamlScalar)?.content?.let { s.dependsOn.add(it) }
            }
            is YamlMap -> s.dependsOn.addAll(dep.entries.keys.map { it.content })
            else -> Unit
        }

        // extra_hosts
        o.list("extra_hosts")?.items?.forEach { el ->
            (el as? YamlScalar)?.content?.let { s.extraHosts.add(it) }
        }

        return s
    }

    private fun parseNetwork(name: String, o: YamlMap): NetworkSpec {
        val n = NetworkSpec(name = name)
        n.driver = o.str("driver") ?: "bridge"
        n.internal = o.bool("internal") ?: false
        n.external = o.bool("external") ?: false
        val ipam = o.map("ipam")
        val configs = ipam?.list("config")
        (configs?.items?.firstOrNull() as? YamlMap)?.let { cfg ->
            n.subnet = cfg.str("subnet") ?: ""
            n.gateway = cfg.str("gateway") ?: ""
        }
        return n
    }

    private fun parseVolume(name: String, o: YamlMap): NamedVolumeSpec {
        val v = NamedVolumeSpec(name = name)
        v.driver = o.str("driver") ?: "local"
        v.external = o.bool("external") ?: false
        return v
    }

    // ---- 短语法 ----

    private fun parsePortShort(s: String): PortMapping {
        // 支持 "8080:80" / "127.0.0.1:8080:80" / "8080:80/udp" / "80"（仅容器端口）
        var rest = s
        var protocol = "tcp"
        if (rest.endsWith("/udp", ignoreCase = true)) {
            protocol = "udp"
            rest = rest.dropLast(4)
        }
        val parts = rest.split(':')
        return when (parts.size) {
            1 -> PortMapping(hostPort = "", containerPort = parts[0], protocol = protocol)
            2 -> PortMapping(hostPort = parts[0], containerPort = parts[1], protocol = protocol)
            else -> PortMapping(hostIp = parts[0], hostPort = parts[1], containerPort = parts[2], protocol = protocol)
        }
    }

    private fun parseVolumeShort(s: String): VolumeMount {
        // "name:/path:ro" / "/host:/path:ro" / "/path"
        val parts = s.split(':')
        val lastIsMode = parts.lastOrNull() == "ro" || parts.lastOrNull() == "rw"
        val content = if (lastIsMode) parts.dropLast(1) else parts
        val source = content.firstOrNull() ?: ""
        val target = content.drop(1).joinToString(":")
        val isBind = source.startsWith("/") || source.startsWith("./") || source.startsWith("~")
        return VolumeMount(
            type = if (isBind) VolumeType.BIND else VolumeType.VOLUME,
            name = if (isBind) "" else source,
            hostPath = if (isBind) source else "",
            containerPath = target,
            readOnly = lastIsMode && parts.last() == "ro",
        )
    }

    // ---- YamlNode 工具 ----

    private fun YamlNode?.asMap(): YamlMap? = this as? YamlMap
    private fun YamlNode?.asList(): YamlList? = this as? YamlList
    private fun YamlNode?.asScalar(): YamlScalar? = this as? YamlScalar

    private fun YamlMap?.map(key: String): YamlMap? = this?.get<YamlNode>(key)?.asMap()
    private fun YamlMap?.list(key: String): YamlList? = this?.get<YamlNode>(key)?.asList()
    private fun YamlMap?.str(key: String): String? = this?.get<YamlNode>(key)?.asScalar()?.content
    private fun YamlMap?.bool(key: String): Boolean? = this?.get<YamlNode>(key)?.asScalar()?.content
        ?.let { it.toBooleanStrictOrNull() ?: it.toIntOrNull()?.let { n -> n != 0 } }

    private fun scalarText(v: YamlNode?): String = v.asScalar()?.content ?: ""

    private fun YamlMap.toCmdStr(key: String): String? = when (val v = this.get<YamlNode>(key)) {
        is YamlScalar -> v.content
        is YamlList -> v.items.mapNotNull { (it as? YamlScalar)?.content }.joinToString(" ").ifBlank { null }
        else -> null
    }
}
