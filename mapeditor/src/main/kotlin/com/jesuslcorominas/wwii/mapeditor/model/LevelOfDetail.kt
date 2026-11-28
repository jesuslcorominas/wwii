package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.CityKind

/**
 * What the map canvas shows at a zoom level, given the width of a hex on screen in pixels. Zoomed out,
 * the map keeps only what helps to find one's way: capitals first, then nothing.
 */
data class LevelOfDetail(
    /** City kinds whose names are drawn. */
    val labelledCities: Set<CityKind>,
    /** Whether the dots of plain cities are drawn (capitals always are). */
    val plainCityDots: Boolean,
    /** Whether mountain and airport symbols are drawn. */
    val smallSymbols: Boolean,
) {
    companion object {
        private val ALL = CityKind.entries.toSet()
        private val CAPITALS = setOf(CityKind.NATIONAL_CAPITAL, CityKind.TERRITORY_CAPITAL)
        private val NATIONAL = setOf(CityKind.NATIONAL_CAPITAL)

        /** Hex widths on screen (pixels) at which each level starts. */
        const val ALL_LABELS_PX = 70.0
        const val CAPITAL_LABELS_PX = 36.0
        const val NATIONAL_LABELS_PX = 20.0
        const val SYMBOLS_PX = 14.0

        /** Smallest and largest size of city names (sp), and their share of the hex width. */
        const val MIN_LABEL_SP = 13f
        const val MAX_LABEL_SP = 30f
        private const val LABEL_SHARE_OF_HEX = 0.2f

        /**
         * Size of city names (sp) for a hex [hexPx] wide on screen: it grows with the zoom, never under
         * [MIN_LABEL_SP] so it can be read from afar, and stops at [MAX_LABEL_SP] when hexes are huge.
         */
        fun labelSize(hexPx: Double): Float =
            (hexPx.toFloat() * LABEL_SHARE_OF_HEX).coerceIn(MIN_LABEL_SP, MAX_LABEL_SP)

        fun forHexWidth(hexPx: Double): LevelOfDetail =
            LevelOfDetail(
                labelledCities =
                    when {
                        hexPx >= ALL_LABELS_PX -> ALL
                        hexPx >= CAPITAL_LABELS_PX -> CAPITALS
                        hexPx >= NATIONAL_LABELS_PX -> NATIONAL
                        else -> emptySet()
                    },
                plainCityDots = hexPx >= SYMBOLS_PX,
                smallSymbols = hexPx >= SYMBOLS_PX,
            )
    }
}

/** A label candidate: its rectangle on screen ([left], [top], [width], [height]) and its city kind. */
data class LabelBox(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val kind: CityKind,
) {
    fun overlaps(other: LabelBox): Boolean =
        left < other.left + other.width &&
            other.left < left + width &&
            top < other.top + other.height &&
            other.top < top + height
}

/**
 * The labels to draw, without overlaps: national capitals first, then territory capitals, then the
 * rest, each kept only if it does not cover one already kept. Returns the indexes into [boxes] in
 * drawing order; ties keep the input order, so the result is stable.
 */
fun placeLabels(boxes: List<LabelBox>): List<Int> {
    val priority = listOf(CityKind.NATIONAL_CAPITAL, CityKind.TERRITORY_CAPITAL, CityKind.CITY)
    val kept = mutableListOf<Int>()
    boxes.indices
        .sortedBy { priority.indexOf(boxes[it].kind) }
        .forEach { i -> if (kept.none { boxes[it].overlaps(boxes[i]) }) kept += i }
    return kept
}
