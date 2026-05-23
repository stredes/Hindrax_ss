package com.hindrax.ss.domain.tools

data class ToolTutorial(
    val section: String = "TUTORIAL",
    val authorizedUse: String,
    val commandExample: String,
    val workflow: List<String>,
    val notes: List<String> = emptyList()
)

data class ToolCatalogItem(
    val command: String,
    val displayName: String = command,
    val riskLevel: ToolRiskLevel = ToolRiskLevel.LOW,
    val executionMode: ToolExecutionMode = ToolExecutionMode.TERMUX,
    val tutorial: ToolTutorial = ToolTutorialFactory.forTool(command, riskLevel, executionMode),
    val termuxPackage: String? = TermuxPackageHints.packageFor(command)
)

data class ToolCategory(
    val id: String,
    val name: String,
    val tools: List<ToolCatalogItem>,
    val capabilities: List<String>,
    val requirements: List<String> = emptyList()
)

enum class ToolRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

enum class ToolExecutionMode {
    NATIVE,
    TERMUX,
    EXTERNAL,
    OPTIONAL
}

data class EnvironmentGuideSection(
    val title: String,
    val steps: List<String>
)

object HindraxEnvironmentGuide {
    val sections: List<EnvironmentGuideSection> = listOf(
        EnvironmentGuideSection(
            title = "TUTORIAL",
            steps = listOf(
                "Usa las herramientas directamente desde el inicio de Hindrax.",
                "Elige una categoria y revisa CAPABILITIES antes de ejecutar.",
                "Cada tool abre el modulo funcional mas cercano: nativo, Termux, CYD o NFC.",
                "Cambia siempre <TARGET_AUTORIZADO> por un activo propio o autorizado."
            )
        ),
        EnvironmentGuideSection(
            title = "TERMUX_BRIDGE",
            steps = listOf(
                "Instala Termux y crea ~/.hindrax_ss/scripts.",
                "Instala solo herramientas necesarias para tu laboratorio.",
                "Valida cada script con manifiesto o allowlist antes de ejecutarlo.",
                "Guarda resultados en Reporting Engine para trazabilidad."
            )
        ),
        EnvironmentGuideSection(
            title = "SAFETY_GATE",
            steps = listOf(
                "Registra objetivos autorizados antes de tareas remotas.",
                "Mantiene herramientas HIGH en laboratorio o redes privadas controladas.",
                "No marques una sesion como VERIFIED sin evidencia de autorizacion.",
                "Documenta alcance, fecha y propietario del objetivo."
            )
        ),
        EnvironmentGuideSection(
            title = "LINUX_LAB",
            steps = listOf(
                "Usa wordlists y capturas de prueba dentro del almacenamiento del lab.",
                "Separa artefactos por cliente, practica o entorno.",
                "Verifica disponibilidad con <tool> --help antes de lanzar workflows.",
                "Prioriza comandos de diagnostico y versionado antes de pruebas activas."
            )
        )
    )
}

object TermuxPackageHints {
    fun packageFor(command: String): String? {
        return when (command.lowercase()) {
            "dig" -> "dnsutils"
            "ssh" -> "openssh"
            "objdump", "readelf", "strings" -> "binutils"
            "gcc", "clang" -> "clang"
            "python", "ruby", "perl", "go", "make", "git", "curl", "wget", "nmap", "masscan",
            "whois", "traceroute", "mtr", "tmux", "htop", "nano", "vim", "tor", "proxychains-ng" -> command.lowercase()
            "proxychains" -> "proxychains-ng"
            "apktool", "jadx", "binwalk", "radare2", "gdb", "ltrace", "strace", "sqlmap" -> command.lowercase()
            else -> null
        }
    }

    fun installCommandFor(command: String): String? {
        return packageFor(command)?.let { "pkg install -y $it" }
    }
}

object ToolTutorialFactory {
    fun forTool(
        command: String,
        riskLevel: ToolRiskLevel,
        executionMode: ToolExecutionMode
    ): ToolTutorial {
        val normalized = command.lowercase()
        val purpose = purposeFor(normalized)
        val example = commandExampleFor(command, normalized, riskLevel, executionMode)
        val workflow = listOf(
            "1. Confirma alcance y autorizacion del objetivo.",
            "2. Verifica disponibilidad con $command --help.",
            "3. Ejecuta la plantilla sobre <TARGET_AUTORIZADO> o datos de laboratorio.",
            "4. Guarda evidencia, hora y parametros en el reporte Hindrax."
        )
        val notes = buildList {
            add(purpose)
            if (riskLevel == ToolRiskLevel.HIGH) {
                add("Riesgo HIGH: usar solo en laboratorio, red propia o permiso escrito.")
            }
            if (executionMode == ToolExecutionMode.OPTIONAL) {
                add("Herramienta opcional: puede no estar disponible en todas las builds.")
            }
        }

        return ToolTutorial(
            authorizedUse = purpose,
            commandExample = example,
            workflow = workflow,
            notes = notes
        )
    }

    private fun commandExampleFor(
        command: String,
        normalized: String,
        riskLevel: ToolRiskLevel,
        executionMode: ToolExecutionMode
    ): String {
        if (riskLevel == ToolRiskLevel.HIGH) {
            return "$command --help  # preparar solo para <LAB_AUTORIZADO>"
        }

        val example = when (normalized) {
            "nmap" -> "nmap -sV <TARGET_AUTORIZADO>"
            "masscan" -> "masscan <RANGO_AUTORIZADO> --ports <PUERTOS>"
            "netdiscover" -> "netdiscover -r <RANGO_PRIVADO_AUTORIZADO>"
            "arp-scan" -> "arp-scan <RANGO_PRIVADO_AUTORIZADO>"
            "hping3" -> "hping3 -S <TARGET_AUTORIZADO> -p <PUERTO>"
            "fping" -> "fping -a -g <RANGO_PRIVADO_AUTORIZADO>"
            "zmap" -> "zmap -p <PUERTO> <RANGO_AUTORIZADO>"
            "dnsrecon" -> "dnsrecon -d <DOMINIO_AUTORIZADO>"
            "dnsenum" -> "dnsenum <DOMINIO_AUTORIZADO>"
            "dig" -> "dig <DOMINIO_AUTORIZADO> A"
            "whois" -> "whois <DOMINIO_AUTORIZADO>"
            "traceroute" -> "traceroute <TARGET_AUTORIZADO>"
            "mtr" -> "mtr <TARGET_AUTORIZADO>"
            "theharvester" -> "theHarvester -d <DOMINIO_AUTORIZADO> -b <FUENTE_PUBLICA>"
            "recon-ng" -> "recon-ng --help  # workspace: <DOMINIO_AUTORIZADO>"
            "metagoofil" -> "metagoofil -d <DOMINIO_AUTORIZADO> -t pdf -l <LIMITE>"
            "sublist3r" -> "sublist3r -d <DOMINIO_AUTORIZADO>"
            "amass" -> "amass enum -passive -d <DOMINIO_AUTORIZADO>"
            "maltego" -> "maltego  # crear grafo para <DOMINIO_AUTORIZADO>"
            "photon" -> "photon -u https://<DOMINIO_AUTORIZADO>"
            "nikto" -> "nikto -h https://<TARGET_AUTORIZADO>"
            "whatweb" -> "whatweb https://<TARGET_AUTORIZADO>"
            "wapiti" -> "wapiti -u https://<TARGET_AUTORIZADO> --scope page"
            "dirsearch" -> "dirsearch -u https://<TARGET_AUTORIZADO> -w <WORDLIST_LAB>"
            "gobuster" -> "gobuster dir -u https://<TARGET_AUTORIZADO> -w <WORDLIST_LAB>"
            "wfuzz" -> "wfuzz -w <WORDLIST_LAB> https://<TARGET_AUTORIZADO>/FUZZ"
            "ffuf" -> "ffuf -u https://<TARGET_AUTORIZADO>/FUZZ -w <WORDLIST_LAB>"
            "burpsuite" -> "burpsuite  # proxy para <APP_AUTORIZADA>"
            "sqlmap" -> "sqlmap -u https://<TARGET_AUTORIZADO>/<RUTA_TEST> --batch --risk=1"
            "xsser" -> "xsser --url https://<TARGET_AUTORIZADO>/<RUTA_TEST>"
            "apktool" -> "apktool d <APK_PROPIO.apk> -o <SALIDA_LAB>"
            "jadx" -> "jadx -d <SALIDA_LAB> <APK_PROPIO.apk>"
            "dex2jar" -> "dex2jar <APK_PROPIO.apk>"
            "smali" -> "smali --help  # ensamblar <CODIGO_LAB>"
            "baksmali" -> "baksmali d <CLASSES.dex> -o <SALIDA_LAB>"
            "aapt" -> "aapt dump badging <APK_PROPIO.apk>"
            "apksigner" -> "apksigner verify --verbose <APK_PROPIO.apk>"
            "uber-apk-signer" -> "uber-apk-signer -a <APK_PROPIO.apk>"
            "radare2" -> "radare2 <BINARIO_LAB>"
            "gdb" -> "gdb <BINARIO_LAB>"
            "objdump" -> "objdump -x <BINARIO_LAB>"
            "readelf" -> "readelf -a <BINARIO_LAB>"
            "strings" -> "strings <ARCHIVO_LAB>"
            "ltrace" -> "ltrace <BINARIO_LAB>"
            "strace" -> "strace <COMANDO_LAB>"
            "ghex" -> "ghex <ARCHIVO_LAB>"
            "binaryninja" -> "binaryninja <BINARIO_LAB>"
            "blue_hydra" -> "blue_hydra --help  # laboratorio BLE <DISPOSITIVO_PROPIO>"
            "btscanner" -> "btscanner  # entorno Bluetooth autorizado"
            "bluelog" -> "bluelog -i <INTERFAZ_BLUETOOTH>"
            "bluez" -> "bluetoothctl  # dispositivo propio <ID_BLE>"
            "mfoc" -> "mfoc --help  # tag NFC propio <TAG_LAB>"
            "mfcuk" -> "mfcuk --help  # tag NFC propio <TAG_LAB>"
            "libnfc" -> "nfc-list  # lector NFC local"
            "nfc-tools" -> "nfc-list  # tag NFC propio"
            "binwalk" -> "binwalk <FIRMWARE_LAB.bin>"
            "foremost" -> "foremost -i <IMAGEN_LAB.dd> -o <SALIDA_LAB>"
            "scalpel" -> "scalpel <IMAGEN_LAB.dd> -o <SALIDA_LAB>"
            "bulk_extractor" -> "bulk_extractor -o <SALIDA_LAB> <IMAGEN_LAB.dd>"
            "dcfldd" -> "dcfldd if=<DISPOSITIVO_LAB> of=<IMAGEN_LAB.dd> hash=sha256"
            "firmwalker" -> "firmwalker <FIRMWARE_EXTRAIDO>"
            "firmadyne" -> "firmadyne  # emular <FIRMWARE_LAB>"
            "qemu" -> "qemu-system-<ARCH> <IMAGEN_LAB>"
            "sasquatch" -> "sasquatch <FIRMWARE_LAB.bin>"
            "python" -> "python <SCRIPT_LAB.py>"
            "ruby" -> "ruby <SCRIPT_LAB.rb>"
            "perl" -> "perl <SCRIPT_LAB.pl>"
            "bash" -> "bash <SCRIPT_LAB.sh>"
            "go" -> "go run <MODULO_LAB.go>"
            "gcc" -> "gcc <FUENTE_LAB.c> -o <BINARIO_LAB>"
            "clang" -> "clang <FUENTE_LAB.c> -o <BINARIO_LAB>"
            "make" -> "make -C <PROYECTO_LAB>"
            "pip" -> "pip install <PAQUETE_LAB>"
            "gem" -> "gem install <PAQUETE_LAB>"
            "searchsploit" -> "searchsploit <PRODUCTO_VERSION_AUTORIZADO>"
            "vim" -> "vim <ARCHIVO_LAB>"
            "nano" -> "nano <ARCHIVO_LAB>"
            "tmux" -> "tmux new -s <SESION_LAB>"
            "htop" -> "htop"
            "curl" -> "curl -I https://<TARGET_AUTORIZADO>"
            "wget" -> "wget https://<RECURSO_AUTORIZADO>"
            "git" -> "git clone <REPO_AUTORIZADO>"
            "ssh" -> "ssh <USUARIO>@<HOST_PROPIO>"
            "proxychains" -> "proxychains curl https://<TARGET_AUTORIZADO>"
            "tor" -> "tor --verify-config"
            else -> if (executionMode == ToolExecutionMode.OPTIONAL) {
                "$command --help  # herramienta opcional para <LAB_AUTORIZADO>"
            } else {
                "$command --help  # revisar opciones para <TARGET_AUTORIZADO>"
            }
        }

        return if (example.contains("<")) example else "$example  # <ENTORNO_LAB>"
    }

    private fun purposeFor(normalized: String): String {
        return when (normalized) {
            "nmap", "masscan", "zmap" -> "Mapeo de red y puertos dentro de alcance autorizado."
            "netdiscover", "arp-scan", "fping" -> "Descubrimiento de hosts en red privada autorizada."
            "dnsrecon", "dnsenum", "dig", "whois" -> "Consulta y enumeracion DNS/registro sobre dominios propios o autorizados."
            "traceroute", "mtr", "hping3" -> "Diagnostico de rutas, latencia y conectividad controlada."
            "theharvester", "recon-ng", "metagoofil", "sublist3r", "amass", "maltego", "photon" -> "OSINT pasivo o documentado sobre activos autorizados."
            "nikto", "whatweb", "wapiti", "dirsearch", "gobuster", "wfuzz", "ffuf", "burpsuite", "sqlmap", "xsser" -> "Revision web en aplicaciones propias, laboratorio o con permiso explicito."
            "apktool", "jadx", "dex2jar", "smali", "baksmali", "aapt", "apksigner", "uber-apk-signer" -> "Analisis de APK propio, interno o autorizado."
            "radare2", "gdb", "objdump", "readelf", "strings", "ltrace", "strace", "ghex", "binaryninja" -> "Inspeccion de binarios y procesos de laboratorio."
            "hashcat", "john", "hydra", "medusa", "cewl", "crunch" -> "Auditoria de credenciales exclusivamente en laboratorio o cuentas propias."
            "aircrack-ng", "aireplay-ng", "airodump-ng", "reaver", "bully", "hcxdumptool", "hcxtools" -> "Diagnostico inalambrico en red propia con adaptador compatible."
            "blue_hydra", "btscanner", "bluelog", "bluez" -> "Diagnostico Bluetooth/BLE sobre dispositivos propios o autorizados."
            "mfoc", "mfcuk", "libnfc", "nfc-tools" -> "Lectura y diagnostico NFC/RFID en tags propios o de laboratorio."
            "binwalk", "foremost", "scalpel", "bulk_extractor", "dcfldd", "firmwalker", "firmadyne", "qemu", "sasquatch" -> "Forense, firmware e IoT sobre imagenes y artefactos de laboratorio."
            "modscan", "modpoll", "plcscan" -> "Diagnostico ICS/SCADA solo en simuladores o bancos industriales autorizados."
            "tcpdump", "ettercap", "bettercap", "dsniff", "wireshark-cli" -> "Captura e inspeccion de trafico en laboratorio o redes propias."
            "metasploit-framework", "routersploit", "searchsploit" -> "Investigacion de CVEs y ejercicios controlados en laboratorio autorizado."
            else -> "Utilidad de soporte para automatizacion, terminal o preparacion del laboratorio Hindrax."
        }
    }
}

object AndraxToolCatalog {
    val categories: List<ToolCategory> = listOf(
        ToolCategory(
            id = "network-recon",
            name = "Reconocimiento de Red",
            tools = tools(
                "nmap", "masscan", "netdiscover", "arp-scan", "hping3", "fping",
                "zmap", "dnsrecon", "dnsenum", "dig", "whois", "traceroute", "mtr"
            ),
            capabilities = listOf(
                "Descubrimiento de hosts",
                "Escaneo TCP/UDP",
                "Enumeracion DNS",
                "Fingerprinting de servicios",
                "Latencia y rutas"
            )
        ),
        ToolCategory(
            id = "osint",
            name = "OSINT",
            tools = tools("theHarvester", "recon-ng", "metagoofil", "sublist3r", "amass", "maltego", "photon"),
            capabilities = listOf(
                "Subdominios",
                "Correos publicos",
                "Metadata de documentos",
                "Infraestructura publica"
            )
        ),
        ToolCategory(
            id = "web-security",
            name = "Web Security",
            tools = tools(
                "nikto", "whatweb", "wapiti", "dirsearch", "gobuster",
                "wfuzz", "ffuf", "burpsuite", "sqlmap", "xsser"
            ),
            capabilities = listOf(
                "Enumeracion web",
                "Identificacion de tecnologias",
                "Revision HTTP",
                "Descubrimiento de rutas",
                "Pruebas SQLi/XSS"
            )
        ),
        ToolCategory(
            id = "android-apk-analysis",
            name = "Android Security / APK Analysis",
            tools = tools("apktool", "jadx", "dex2jar", "smali", "baksmali", "aapt", "apksigner", "uber-apk-signer"),
            capabilities = listOf(
                "Decompilacion APK",
                "Lectura de manifiestos",
                "Analisis de permisos",
                "Conversion DEX/JAR",
                "Firma de APK"
            )
        ),
        ToolCategory(
            id = "reverse-engineering",
            name = "Ingenieria Inversa",
            tools = listOf(
                ToolCatalogItem("radare2"),
                ToolCatalogItem("gdb"),
                ToolCatalogItem("objdump"),
                ToolCatalogItem("readelf"),
                ToolCatalogItem("strings"),
                ToolCatalogItem("ltrace"),
                ToolCatalogItem("strace"),
                ToolCatalogItem("ghex"),
                ToolCatalogItem("binaryninja", executionMode = ToolExecutionMode.OPTIONAL)
            ),
            capabilities = listOf(
                "Desensamblado",
                "Depuracion",
                "Analisis ELF",
                "Inspeccion de procesos"
            )
        ),
        ToolCategory(
            id = "password-auditing",
            name = "Password Auditing",
            tools = highRiskTools("hashcat", "john", "hydra", "medusa", "cewl", "crunch"),
            capabilities = listOf(
                "Hash cracking",
                "Wordlists",
                "Fuerza bruta controlada",
                "Ataques de diccionario"
            )
        ),
        ToolCategory(
            id = "wireless-security",
            name = "Wireless Security",
            tools = highRiskTools("aircrack-ng", "aireplay-ng", "airodump-ng", "reaver", "bully", "hcxdumptool", "hcxtools"),
            capabilities = listOf(
                "Diagnostico Wi-Fi",
                "Captura de handshakes",
                "WPS auditing"
            ),
            requirements = listOf("Root", "Adaptador compatible")
        ),
        ToolCategory(
            id = "bluetooth",
            name = "Bluetooth",
            tools = tools("blue_hydra", "btscanner", "bluelog", "bluez"),
            capabilities = listOf(
                "Descubrimiento Bluetooth",
                "Enumeracion BLE"
            )
        ),
        ToolCategory(
            id = "nfc-rfid",
            name = "NFC / RFID",
            tools = tools("mfoc", "mfcuk", "libnfc", "nfc-tools"),
            capabilities = listOf(
                "Lectura MIFARE",
                "Diagnostico NFC"
            )
        ),
        ToolCategory(
            id = "digital-forensics",
            name = "Forense Digital",
            tools = tools("binwalk", "foremost", "scalpel", "bulk_extractor", "strings", "dcfldd"),
            capabilities = listOf(
                "Firmware extraction",
                "File carving",
                "Recuperacion de datos"
            )
        ),
        ToolCategory(
            id = "iot-firmware",
            name = "IoT / Firmware",
            tools = tools("firmwalker", "binwalk", "firmadyne", "qemu", "sasquatch"),
            capabilities = listOf(
                "Desempaquetado firmware",
                "Emulacion",
                "Configuracion embebida"
            )
        ),
        ToolCategory(
            id = "ics-scada",
            name = "ICS / SCADA",
            tools = highRiskTools("modscan", "modpoll", "plcscan"),
            capabilities = listOf(
                "Diagnostico Modbus",
                "Enumeracion industrial"
            )
        ),
        ToolCategory(
            id = "sniffing",
            name = "Redes y Sniffing",
            tools = highRiskTools("tcpdump", "ettercap", "bettercap", "dsniff", "wireshark-cli"),
            capabilities = listOf(
                "Captura de trafico",
                "MITM en laboratorio",
                "Inspeccion de paquetes"
            )
        ),
        ToolCategory(
            id = "automation-scripting",
            name = "Automatizacion y Scripting",
            tools = tools("python", "ruby", "perl", "bash", "go", "gcc", "clang", "make", "pip", "gem"),
            capabilities = listOf(
                "Lenguajes de automatizacion",
                "Compilacion local",
                "Gestion de paquetes",
                "Scripts reproducibles"
            )
        ),
        ToolCategory(
            id = "exploitation-frameworks",
            name = "Explotacion Frameworks",
            tools = highRiskTools("metasploit-framework", "routersploit", "searchsploit"),
            capabilities = listOf(
                "Laboratorios de explotacion",
                "Busqueda de CVEs",
                "Automatizacion de pruebas autorizadas"
            )
        ),
        ToolCategory(
            id = "linux-utilities",
            name = "Utilidades Linux Generales",
            tools = tools("vim", "nano", "tmux", "htop", "curl", "wget", "git", "ssh", "proxychains", "tor"),
            capabilities = listOf(
                "Edicion y terminal",
                "Transferencia de datos",
                "Control de versiones",
                "Tunneling y proxy"
            )
        )
    )

    val allTools: List<ToolCatalogItem> = categories.flatMap { it.tools }.distinctBy { it.command }

    fun categoryById(id: String): ToolCategory {
        return categories.first { it.id == id }
    }

    private fun tools(vararg commands: String): List<ToolCatalogItem> {
        return commands.map { ToolCatalogItem(command = it) }
    }

    private fun highRiskTools(vararg commands: String): List<ToolCatalogItem> {
        return commands.map { ToolCatalogItem(command = it, riskLevel = ToolRiskLevel.HIGH) }
    }
}

object AndraxInternalArchitecture {
    val layers: List<String> = listOf(
        "ANDRAX APK",
        "AX Terminal",
        "DCO Launcher",
        "Linux Environment",
        "Tool Packages",
        "Wordlists",
        "Scripts",
        "Optional Root/Hardware Support"
    )

    val specialComponents: List<String> = listOf(
        "AX Terminal",
        "DCO",
        "AXSurf",
        "Wordlists integradas"
    )

    val dependencies: List<String> = listOf(
        "Root",
        "SELinux permisivo",
        "Adaptadores externos USB OTG",
        "Chipsets Wi-Fi compatibles",
        "Almacenamiento amplio",
        "ARM64"
    )
}

object HindraxToolArchitecture {
    val layers: List<String> = listOf(
        "UI Android moderna",
        "Safety Gate",
        "Task Catalog",
        "Termux Bridge",
        "Linux Lab",
        "Reporting Engine",
        "Workflow Automation"
    )
}
