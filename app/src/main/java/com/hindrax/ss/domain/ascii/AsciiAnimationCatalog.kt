package com.hindrax.ss.domain.ascii

data class AsciiAnimationSpec(
    val key: String,
    val label: String,
    val frameMillis: Long,
    val frames: List<String>
) {
    init {
        require(frames.isNotEmpty()) { "ASCII animation requires at least one frame." }
        require(frameMillis >= 80L) { "Frame speed must stay readable on mobile screens." }
    }
}

enum class AsciiAnimationContext {
    Dashboard,
    UtilsHub,
    Time,
    Notes,
    Checklist,
    Calculator,
    Converter,
    System,
    Measure,
    Audio,
    Text,
    Random,
    Catalog,
    TemplarSeal,
    Network,
    Chat,
    Inventory,
    Location,
    Music,
    Terminal,
    Tasks
}

object AsciiAnimationCatalog {
    fun forContext(context: AsciiAnimationContext): AsciiAnimationSpec {
        return when (context) {
            AsciiAnimationContext.Dashboard -> dashboardPulse
            AsciiAnimationContext.UtilsHub -> utilsHub
            AsciiAnimationContext.Time -> clockSweep
            AsciiAnimationContext.Notes -> noteCursor
            AsciiAnimationContext.Checklist -> checklistScan
            AsciiAnimationContext.Calculator -> calculatorPulse
            AsciiAnimationContext.Converter -> converterFlow
            AsciiAnimationContext.System -> systemSignal
            AsciiAnimationContext.Measure -> measureSweep
            AsciiAnimationContext.Audio -> audioWave
            AsciiAnimationContext.Text -> textParser
            AsciiAnimationContext.Random -> randomSelector
            AsciiAnimationContext.Catalog -> catalogIndex
            AsciiAnimationContext.TemplarSeal -> templarSeal
            AsciiAnimationContext.Network -> networkSweep
            AsciiAnimationContext.Chat -> chatStream
            AsciiAnimationContext.Inventory -> inventoryFlow
            AsciiAnimationContext.Location -> locationRadar
            AsciiAnimationContext.Music -> musicDeck
            AsciiAnimationContext.Terminal -> terminalMatrix
            AsciiAnimationContext.Tasks -> tasksBoard
        }
    }

    private val dashboardPulse = AsciiAnimationSpec(
        key = "dashboard-pulse",
        label = "CORE_PULSE",
        frameMillis = 180L,
        frames = listOf(
            """
            [HINDRAX]  .----.      core:sync
            signal    |    |  .    modules:ready
            pulse     '----'       api:listen
            """.trimIndent(),
            """
            [HINDRAX]  .----.      core:sync
            signal    | :: |  ..   modules:ready
            pulse     '----'       api:listen
            """.trimIndent(),
            """
            [HINDRAX]  .----.      core:sync
            signal    |::::|  ...  modules:ready
            pulse     '----'       api:listen
            """.trimIndent(),
            """
            [HINDRAX]  .----.      core:sync
            signal    | :: |  ..   modules:ready
            pulse     '----'       api:listen
            """.trimIndent()
        )
    )

    private val utilsHub = AsciiAnimationSpec(
        key = "utils-hub",
        label = "UTIL_ROUTER",
        frameMillis = 150L,
        frames = listOf(
            """
            +-- utils --+
            | time  >  |
            | calc     |
            | system   |
            +----------+
            """.trimIndent(),
            """
            +-- utils --+
            | time     |
            | calc  >  |
            | system   |
            +----------+
            """.trimIndent(),
            """
            +-- utils --+
            | time     |
            | calc     |
            | system > |
            +----------+
            """.trimIndent()
        )
    )

    private val clockSweep = AsciiAnimationSpec(
        key = "clock-sweep",
        label = "TIME_SWEEP",
        frameMillis = 160L,
        frames = listOf(
            """
              .---.
             /  |  \
            |   o   |
             \     /
              '---'
            """.trimIndent(),
            """
              .---.
             /     \
            |   o-- |
             \     /
              '---'
            """.trimIndent(),
            """
              .---.
             /     \
            |   o   |
             \  |  /
              '---'
            """.trimIndent(),
            """
              .---.
             /     \
            | --o   |
             \     /
              '---'
            """.trimIndent()
        )
    )

    private val noteCursor = AsciiAnimationSpec(
        key = "note-cursor",
        label = "NOTE_CURSOR",
        frameMillis = 240L,
        frames = listOf(
            "[ nota_local ]\n> escribir_",
            "[ nota_local ]\n> escribir ",
            "[ nota_local ]\n> escribir_"
        )
    )

    private val checklistScan = AsciiAnimationSpec(
        key = "checklist-scan",
        label = "CHECK_SCAN",
        frameMillis = 180L,
        frames = listOf(
            "[ ] item_a\n[ ] item_b\n[ ] item_c",
            "[x] item_a\n[ ] item_b\n[ ] item_c",
            "[x] item_a\n[x] item_b\n[ ] item_c",
            "[x] item_a\n[x] item_b\n[x] item_c"
        )
    )

    private val calculatorPulse = AsciiAnimationSpec(
        key = "calculator-pulse",
        label = "CALC_PIPE",
        frameMillis = 170L,
        frames = listOf(
            "A + B  ->  [   ]",
            "A + B  ->  [=  ]",
            "A + B  ->  [== ]",
            "A + B  ->  [===]"
        )
    )

    private val converterFlow = AsciiAnimationSpec(
        key = "converter-flow",
        label = "UNIT_FLOW",
        frameMillis = 170L,
        frames = listOf(
            "kg ----> lb\nkm      mi\nc       f",
            "kg      lb\nkm ----> mi\nc       f",
            "kg      lb\nkm      mi\nc  ----> f"
        )
    )

    private val systemSignal = AsciiAnimationSpec(
        key = "system-signal",
        label = "DEVICE_SIGNAL",
        frameMillis = 150L,
        frames = listOf(
            "flash [.]  cam [ ]  qr [ ]",
            "flash [:]  cam [.]  qr [ ]",
            "flash [::] cam [:]  qr [.]",
            "flash [:]  cam [::] qr [:]"
        )
    )

    private val measureSweep = AsciiAnimationSpec(
        key = "measure-sweep",
        label = "MEASURE_SWEEP",
        frameMillis = 160L,
        frames = listOf(
            "0----1----2----3\n^",
            "0----1----2----3\n     ^",
            "0----1----2----3\n          ^",
            "0----1----2----3\n               ^"
        )
    )

    private val audioWave = AsciiAnimationSpec(
        key = "audio-wave",
        label = "AUDIO_WAVE",
        frameMillis = 120L,
        frames = listOf(
            "mic |.|   |.|   |.|",
            "mic |:|  |:::|  |:|",
            "mic |:::||:::::||:::|",
            "mic |:|  |:::|  |:|"
        )
    )

    private val textParser = AsciiAnimationSpec(
        key = "text-parser",
        label = "TEXT_PARSE",
        frameMillis = 180L,
        frames = listOf(
            "raw_text     -> words",
            "RAW_TEXT     -> WORDS",
            "raw text     -> clean",
            "chars:scan   -> copy"
        )
    )

    private val randomSelector = AsciiAnimationSpec(
        key = "random-selector",
        label = "RANDOM_PICK",
        frameMillis = 130L,
        frames = listOf(
            "[a] b  c",
            " a [b] c",
            " a  b [c]",
            " a [b] c"
        )
    )

    private val catalogIndex = AsciiAnimationSpec(
        key = "catalog-index",
        label = "INDEX_SCROLL",
        frameMillis = 180L,
        frames = listOf(
            "01 time\n02 calc\n03 notes",
            "02 calc\n03 notes\n04 system",
            "03 notes\n04 system\n05 audio",
            "04 system\n05 audio\n06 text"
        )
    )

    private val templarSeal = AsciiAnimationSpec(
        key = "templar-seal",
        label = "TEMPLAR_SEAL",
        frameMillis = 160L,
        frames = listOf(
            """
              /\
             /++\   shield:idle
             |><|   blade:ready
            """.trimIndent(),
            """
              /\
             /##\   shield:pulse
             |<>|   blade:scan
            """.trimIndent(),
            """
              /\
             /++\   shield:guard
             |\/|   blade:sync
            """.trimIndent(),
            """
              /\
             /##\   shield:pulse
             |<>|   blade:scan
            """.trimIndent()
        )
    )

    private val networkSweep = AsciiAnimationSpec(
        key = "network-sweep",
        label = "NET_SWEEP",
        frameMillis = 120L,
        frames = listOf(
            "node . . .\nlan  [>---]\nport  .. ..",
            "node .:. .\nlan  [- >-]\nport  :.. .",
            "node .:.:.\nlan  [--->]\nport  .:.:",
            "node .:. .\nlan  [- >-]\nport  :.. ."
        )
    )

    private val chatStream = AsciiAnimationSpec(
        key = "chat-stream",
        label = "CHAT_STREAM",
        frameMillis = 150L,
        frames = listOf(
            "msg[01] --> peer\nsync    .\nthread  open",
            "msg[01] ---> peer\nsync    :\nthread  open",
            "msg[02] ----> peer\nsync    ::\nthread  live",
            "msg[02] ---> peer\nsync    :\nthread  live"
        )
    )

    private val inventoryFlow = AsciiAnimationSpec(
        key = "inventory-flow",
        label = "ITEM_FLOW",
        frameMillis = 150L,
        frames = listOf(
            "stock [||||]\nitem  +1\nsync  .",
            "stock [||| ]\nitem  -1\nsync  :",
            "stock [||||]\nitem  +\nsync  ::",
            "stock [|||:]\nitem  ok\nsync  :"
        )
    )

    private val locationRadar = AsciiAnimationSpec(
        key = "location-radar",
        label = "GEO_RADAR",
        frameMillis = 130L,
        frames = listOf(
            "   .   \n --+-- \n   o   ",
            "  ...  \n --o-- \n   |   ",
            " .:::.\n --o-- \n  / \\  ",
            "  ...  \n --o-- \n   |   "
        )
    )

    private val musicDeck = AsciiAnimationSpec(
        key = "music-deck",
        label = "AUDIO_DECK",
        frameMillis = 110L,
        frames = listOf(
            "[|..] beat\nL == R\nwave .",
            "[||.] beat\nL => R\nwave :",
            "[|||] beat\nL == R\nwave ::",
            "[.||] beat\nL <= R\nwave :"
        )
    )

    private val terminalMatrix = AsciiAnimationSpec(
        key = "terminal-matrix",
        label = "TERM_MATRIX",
        frameMillis = 120L,
        frames = listOf(
            "$ run\n> .\n[ok]",
            "$ run\n> :\n[ok]",
            "$ exec\n> ::\n[ready]",
            "$ exec\n> :\n[ready]"
        )
    )

    private val tasksBoard = AsciiAnimationSpec(
        key = "tasks-board",
        label = "TASK_BOARD",
        frameMillis = 160L,
        frames = listOf(
            "[todo] [run ] [done]\n  ^",
            "[todo] [run ] [done]\n         ^",
            "[todo] [run ] [done]\n                ^",
            "[todo] [run ] [done]\n         ^"
        )
    )
}
