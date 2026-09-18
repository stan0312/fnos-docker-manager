package com.fnos.dockermanager

import com.fnos.dockermanager.composefile.ComposeProject
import com.fnos.dockermanager.composefile.ComposeYamlGenerator
import com.fnos.dockermanager.composefile.ComposeYamlParser
import com.fnos.dockermanager.composefile.KeyValue
import com.fnos.dockermanager.composefile.NamedVolumeSpec
import com.fnos.dockermanager.composefile.NetworkMode
import com.fnos.dockermanager.composefile.NetworkSpec
import com.fnos.dockermanager.composefile.PortMapping
import com.fnos.dockermanager.composefile.RestartPolicy
import com.fnos.dockermanager.composefile.ServiceSpec
import com.fnos.dockermanager.composefile.VolumeMount
import com.fnos.dockermanager.composefile.VolumeType
import com.fnos.dockermanager.docker.DockerClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeYamlTest {

    private fun sampleProject() = ComposeProject(
        name = "media-server",
        services = mutableListOf(
            ServiceSpec(
                name = "web",
                image = "nginx:latest",
                containerName = "media-web",
                restart = RestartPolicy.ALWAYS,
                ports = mutableListOf(
                    PortMapping(hostPort = "8080", containerPort = "80"),
                    PortMapping(hostIp = "0.0.0.0", hostPort = "8443", containerPort = "443", protocol = "tcp"),
                ),
                volumes = mutableListOf(
                    VolumeMount(type = VolumeType.VOLUME, name = "html", containerPath = "/usr/share/nginx/html", readOnly = true),
                    VolumeMount(type = VolumeType.BIND, hostPath = "/vol1/docker/media", containerPath = "/data"),
                ),
                environment = mutableListOf(
                    KeyValue("TZ", "Asia/Shanghai"),
                    KeyValue("FLAG", "true"),
                    KeyValue("GREETING", "hello world"),
                ),
                labels = mutableListOf(KeyValue("app", "media")),
                dependsOn = mutableListOf("db"),
                tty = false,
            ),
            ServiceSpec(
                name = "db",
                image = "postgres:16",
                restart = RestartPolicy.ON_FAILURE,
                restartRetries = "3",
                environment = mutableListOf(KeyValue("POSTGRES_PASSWORD", "p@ss:word")),
                volumes = mutableListOf(
                    VolumeMount(type = VolumeType.VOLUME, name = "pgdata", containerPath = "/var/lib/postgresql/data"),
                ),
                networkMode = NetworkMode.CUSTOM,
                customNetwork = "backend",
            ),
        ),
        networks = mutableListOf(
            NetworkSpec(name = "backend", driver = "bridge", subnet = "172.20.0.0/16", gateway = "172.20.0.1"),
        ),
        volumes = mutableListOf(
            NamedVolumeSpec(name = "html"),
            NamedVolumeSpec(name = "pgdata"),
        ),
    )

    @Test
    fun generate_containsKeyElements() {
        val yaml = ComposeYamlGenerator.generate(sampleProject())
        assertContains(yaml, "name: media-server")
        assertContains(yaml, "services:")
        assertContains(yaml, "  web:")
        assertContains(yaml, "    image: nginx:latest")
        assertContains(yaml, "    restart: always")
        assertContains(yaml, "      - 8080:80")
        assertContains(yaml, "      - 0.0.0.0:8443:443")
        assertContains(yaml, "html:/usr/share/nginx/html:ro")
        assertContains(yaml, "/vol1/docker/media:/data")
        assertContains(yaml, "TZ=Asia/Shanghai")
        assertContains(yaml, "POSTGRES_PASSWORD=p@ss:word")
        assertContains(yaml, "restart: on-failure:3")
        assertContains(yaml, "depends_on:")
        assertContains(yaml, "      - db")
        assertContains(yaml, "networks:")
        assertContains(yaml, "      - backend")
        assertContains(yaml, "subnet: 172.20.0.0/16")
        assertContains(yaml, "volumes:")
    }

    @Test
    fun scalar_quotesTrickyValues() {
        val g = ComposeYamlGenerator
        assertEquals("plain123", g.scalar("plain123"))
        assertEquals("nginx:latest", g.scalar("nginx:latest"))
        assertEquals("8080:80", g.scalar("8080:80"))
        // 纯数字需要加引号，避免被 YAML 解析为数字
        assertTrue(g.scalar("123").startsWith("\""))
        // 布尔值需要加引号
        assertTrue(g.scalar("true").startsWith("\""))
        // 含空格需要加引号
        assertTrue(g.scalar("hello world").startsWith("\""))
        // 转义
        assertEquals("\"a\\\"b\"", g.scalar("a\"b"))
        assertEquals("\"a\\nb\"", g.scalar("a\nb"))
    }

    @Test
    fun parse_roundTrip() {
        val yaml = ComposeYamlGenerator.generate(sampleProject())
        val parsed = ComposeYamlParser.parse(yaml)

        assertEquals("media-server", parsed.name)
        assertEquals(2, parsed.services.size)
        val web = parsed.services.first { it.name == "web" }
        assertEquals("nginx:latest", web.image)
        assertEquals(RestartPolicy.ALWAYS, web.restart)
        assertEquals(2, web.ports.size)
        assertEquals("8080", web.ports[0].hostPort)
        assertEquals("80", web.ports[0].containerPort)
        assertEquals(2, web.volumes.size)
        assertEquals(VolumeType.VOLUME, web.volumes[0].type)
        assertTrue(web.volumes[0].readOnly)
        assertEquals(VolumeType.BIND, web.volumes[1].type)
        assertEquals("/vol1/docker/media", web.volumes[1].hostPath)
        assertEquals(3, web.environment.size)
        assertEquals("Asia/Shanghai", web.environment.first { it.key == "TZ" }.value)
        assertEquals(listOf("db"), web.dependsOn)

        val db = parsed.services.first { it.name == "db" }
        assertEquals(RestartPolicy.ON_FAILURE, db.restart)
        assertEquals("3", db.restartRetries)
        assertEquals(NetworkMode.CUSTOM, db.networkMode)
        assertEquals("backend", db.customNetwork)

        assertEquals(1, parsed.networks.size)
        assertEquals("172.20.0.0/16", parsed.networks[0].subnet)
        assertEquals("172.20.0.1", parsed.networks[0].gateway)
        assertEquals(2, parsed.volumes.size)
    }

    @Test
    fun parse_envMapAndLongSyntax() {
        val yaml = """
            services:
              app:
                image: myapp:1.0
                environment:
                  MODE: prod
                  COUNT: 3
                ports:
                  - target: 3000
                    published: 9000
                    protocol: tcp
                volumes:
                  - type: bind
                    source: /vol1/app
                    target: /srv
                    read_only: true
        """.trimIndent()
        val parsed = ComposeYamlParser.parse(yaml)
        assertEquals(1, parsed.services.size)
        val app = parsed.services[0]
        assertEquals(2, app.environment.size)
        assertEquals("prod", app.environment.first { it.key == "MODE" }.value)
        assertEquals(1, app.ports.size)
        assertEquals("9000", app.ports[0].hostPort)
        assertEquals("3000", app.ports[0].containerPort)
        assertEquals(1, app.volumes.size)
        assertEquals(VolumeType.BIND, app.volumes[0].type)
        assertTrue(app.volumes[0].readOnly)
    }

    @Test
    fun validate_detectsProblems() {
        val bad = ComposeProject(
            name = "",
            services = mutableListOf(
                ServiceSpec(name = "", image = ""),
                ServiceSpec(name = "dup", image = "a"),
                ServiceSpec(name = "dup", image = "b"),
            ),
        )
        val errors = bad.validate()
        assertTrue(errors.isNotEmpty())
        assertTrue(errors.any { it.contains("项目名称") })
    }

    @Test
    fun demux_multiplexedLogs() {
        // 构造两帧: stdout "hi\n" (3 bytes), stderr "err" (3 bytes)
        val bytes = ByteArray(8 + 3 + 8 + 3)
        bytes[0] = 1 // stdout
        bytes[4] = 0; bytes[5] = 0; bytes[6] = 0; bytes[7] = 3
        bytes[8] = 'h'.code.toByte(); bytes[9] = 'i'.code.toByte(); bytes[10] = '\n'.code.toByte()
        bytes[11] = 2 // stderr
        bytes[15] = 0; bytes[16] = 0; bytes[17] = 0; bytes[18] = 3
        bytes[19] = 'e'.code.toByte(); bytes[20] = 'r'.code.toByte(); bytes[21] = 'r'.code.toByte()
        val text = DockerClient.demuxLogStream(bytes)
        assertEquals("hi\nerr", text)
    }

    @Test
    fun demux_rawTextPassesThrough() {
        val raw = "plain log line\nsecond".encodeToByteArray()
        assertEquals("plain log line\nsecond", DockerClient.demuxLogStream(raw))
    }
}
