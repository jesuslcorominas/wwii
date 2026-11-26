package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.TerritoryKind
import com.jesuslcorominas.wwii.core.nation.NationId
import java.awt.Color

/**
 * Fill colour of a territory in the editor: the hue comes from its nation (stable across runs, so a
 * nation always looks the same), and colonies and possessions are paler than the homeland. Only an
 * editing aid; the game colours by side (DESIGN.md).
 */
object NationColors {
    private const val HUE_STEP = 0.618034f
    private const val SATURATION = 0.55f
    private const val BRIGHTNESS = 0.85f
    private const val PALE_SATURATION = 0.3f
    private const val PALE_BRIGHTNESS = 0.95f

    /** Opaque RGB as `0xRRGGBB`. */
    fun rgb(
        nation: NationId,
        kind: TerritoryKind,
    ): Int {
        val hue = (stableHash(nation.value) * HUE_STEP) % 1f
        val pale = kind != TerritoryKind.HOMELAND
        val saturation = if (pale) PALE_SATURATION else SATURATION
        val brightness = if (pale) PALE_BRIGHTNESS else BRIGHTNESS
        return Color.HSBtoRGB(hue, saturation, brightness) and RGB_MASK
    }

    private const val RGB_MASK = 0xFFFFFF

    private fun stableHash(text: String): Int = text.fold(0) { acc, c -> (acc * 31 + c.code) and 0xFFFF }
}
