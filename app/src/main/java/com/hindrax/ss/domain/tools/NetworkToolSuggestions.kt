package com.hindrax.ss.domain.tools

data class NetworkPortSuggestion(
    val port: Int,
    val service: String,
    val reason: String
)

data class NetworkToolSuggestion(
    val toolId: String,
    val label: String,
    val reason: String
)

data class NetworkPortProfile(
    val id: String,
    val label: String,
    val ports: List<Int>,
    val description: String
)

object NetworkToolSuggestions {
    val defaultPortProfile = NetworkPortProfile(
        id = "common",
        label = "COMMON",
        ports = listOf(22, 53, 80, 443, 445, 3306, 5432, 6379, 8080, 8443, 9999),
        description = "Servicios comunes, web, bases de datos y nodo Hindrax."
    )

    val profiles: List<NetworkPortProfile> = listOf(
        defaultPortProfile,
        NetworkPortProfile(
            id = "web",
            label = "WEB",
            ports = listOf(80, 443, 8000, 8080, 8081, 8443, 9000),
            description = "HTTP, HTTPS, paneles web y APIs locales."
        ),
        NetworkPortProfile(
            id = "lan",
            label = "LAN",
            ports = listOf(22, 53, 80, 139, 443, 445, 548, 631, 8080),
            description = "Administracion, DNS, SMB/AFP, impresoras y web local."
        ),
        NetworkPortProfile(
            id = "iot",
            label = "IOT",
            ports = listOf(23, 80, 443, 554, 1883, 5683, 8000, 8080, 8883),
            description = "Telnet/HTTP/RTSP/MQTT/CoAP en dispositivos propios."
        ),
        NetworkPortProfile(
            id = "hindrax",
            label = "HINDRAX",
            ports = listOf(80, 443, 8080, 8443, 9999),
            description = "Consola web, APIs y canal mesh Hindrax."
        ),
        NetworkPortProfile(
            id = "cyd",
            label = "CYD",
            ports = listOf(80, 443, 8080, 8888),
            description = "Firmware CYD/Bruce, panel HTTP y servicios auxiliares."
        )
    )

    fun profileById(id: String): NetworkPortProfile {
        return profiles.firstOrNull { it.id == id } ?: defaultPortProfile
    }

    fun parsePorts(raw: String?): List<Int> {
        val parsed = raw.orEmpty()
            .split(",", " ", "\n", "\t")
            .mapNotNull { token -> token.trim().toIntOrNull() }
            .filter { it in 1..65535 }
            .distinct()
        return parsed.ifEmpty { defaultPortProfile.ports }
    }

    fun portCsv(profileId: String): String {
        return profileById(profileId).ports.joinToString(",")
    }

    fun toolsForNode(isHindraxNode: Boolean, isCyd: Boolean, hasIp: Boolean): List<NetworkToolSuggestion> {
        val base = buildList {
            if (hasIp) {
                add(NetworkToolSuggestion("ping", "PING", "Valida conectividad basica."))
                add(NetworkToolSuggestion("port_scan", "PORT_SCAN", "Detecta servicios expuestos dentro del alcance."))
                add(NetworkToolSuggestion("banner_grab", "BANNER", "Lee cabeceras/banners en puertos HTTP o TCP conocidos."))
            }
            if (isHindraxNode) {
                add(NetworkToolSuggestion("hindrax_chat", "MESH_CHAT", "Empareja y sincroniza datos del nodo Hindrax."))
                add(NetworkToolSuggestion("live_location", "GEO_LIVE", "Solicita ubicacion en tiempo real si el peer la comparte."))
            }
            if (isCyd) {
                add(NetworkToolSuggestion("cyd_console", "CYD_CONSOLE", "Abre consola y archivos del firmware conectado."))
            }
        }
        return base.distinctBy { it.toolId }
    }

    fun portsForNode(isHindraxNode: Boolean, isCyd: Boolean): List<NetworkPortSuggestion> {
        val profile = when {
            isHindraxNode -> profileById("hindrax")
            isCyd -> profileById("cyd")
            else -> defaultPortProfile
        }
        return profile.ports.map { port ->
            NetworkPortSuggestion(
                port = port,
                service = serviceName(port),
                reason = reasonFor(port)
            )
        }
    }

    fun serviceName(port: Int): String {
        return when (port) {
            22 -> "SSH"
            23 -> "TELNET"
            53 -> "DNS"
            80 -> "HTTP"
            139 -> "NETBIOS"
            443 -> "HTTPS"
            445 -> "SMB"
            548 -> "AFP"
            554 -> "RTSP"
            631 -> "IPP"
            1883 -> "MQTT"
            3306 -> "MYSQL"
            5432 -> "POSTGRES"
            5683 -> "COAP"
            6379 -> "REDIS"
            8000 -> "HTTP_ALT"
            8080 -> "HTTP_ALT"
            8081 -> "HTTP_ALT"
            8443 -> "HTTPS_ALT"
            8883 -> "MQTT_TLS"
            8888 -> "HTTP_ALT"
            9000 -> "APP"
            9999 -> "HINDRAX_MESH"
            else -> "TCP"
        }
    }

    private fun reasonFor(port: Int): String {
        return when (port) {
            80, 443, 8000, 8080, 8081, 8443, 8888, 9000 -> "web/api"
            22, 23 -> "admin"
            53 -> "dns"
            139, 445, 548 -> "fileshare"
            554 -> "video"
            1883, 8883 -> "mqtt"
            9999 -> "hindrax"
            else -> "service"
        }
    }
}
