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
    TemplarSeal
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
}
