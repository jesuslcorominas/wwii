package com.jesuslcorominas.wwii.mapeditor.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Editor settings for one map: the local reference image and how the grid sits on it. They are not
 * map data (D1): they live in a sidecar file next to the map, `<map>.editor.json`, which git ignores
 * because it holds local paths.
 */
@Serializable
data class Calibration(
    val imagePath: String? = null,
    val originX: Double = DEFAULT_ORIGIN,
    val originY: Double = DEFAULT_ORIGIN,
    val hexWidth: Double = DEFAULT_WIDTH,
    val hexHeight: Double = DEFAULT_WIDTH * 2 / HexLayout.SQRT3,
    val imageOpacity: Float = 1f,
    val dataOpacity: Float = DEFAULT_DATA_OPACITY,
) {
    init {
        require(hexWidth > 0 && hexHeight > 0) { "hex size must be positive" }
    }

    val layout: HexLayout get() = HexLayout(originX, originY, hexWidth, hexHeight)

    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private const val DEFAULT_ORIGIN = 40.0
        private const val DEFAULT_WIDTH = 40.0
        private const val DEFAULT_DATA_OPACITY = 0.6f
        private const val SUFFIX = ".editor.json"

        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        /** Throws if the text is not a calibration or holds impossible values (a hex size of zero). */
        fun decode(text: String): Calibration = json.decodeFromString(serializer(), text)

        /** The sidecar file of [mapFile]: `europe.json` → `europe.editor.json`. */
        fun sidecarOf(mapFile: File): File = File(mapFile.parentFile, mapFile.name.removeSuffix(".json") + SUFFIX)
    }
}
