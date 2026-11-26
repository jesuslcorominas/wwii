package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.HexDirections
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A point in reference-image pixels. */
data class Point(
    val x: Double,
    val y: Double,
) {
    fun distanceTo(other: Point): Double = hypot(x - other.x, y - other.y)
}

/**
 * Pointy-top hex grid laid over the reference image, in image pixels. Orientation and scale belong
 * to the editor, not to `core` (decisions D1 and D5).
 *
 * [originX], [originY]: centre of hex (0, 0). [hexWidth]: distance between the centres of two hexes
 * side by side (flat side to flat side). [hexHeight]: point to point. A regular hex has
 * `hexHeight = hexWidth * 2 / sqrt(3)`; they are separate because a scan can be stretched a little.
 */
data class HexLayout(
    val originX: Double,
    val originY: Double,
    val hexWidth: Double,
    val hexHeight: Double,
) {
    init {
        require(hexWidth > 0 && hexHeight > 0) { "hex size must be positive" }
    }

    private val rowStep: Double get() = hexHeight * ROW_FACTOR

    fun center(coord: HexCoord): Point =
        Point(
            x = originX + hexWidth * (coord.q + coord.r / 2.0),
            y = originY + rowStep * coord.r,
        )

    /** The hex that contains [point]. */
    fun hexAt(point: Point): HexCoord {
        val fr = (point.y - originY) / rowStep
        val fq = (point.x - originX) / hexWidth - fr / 2
        return cubeRound(fq, fr)
    }

    /** The six corners of [coord], clockwise from the top-right one. */
    fun corners(coord: HexCoord): List<Point> {
        val c = center(coord)
        return (0 until CORNERS).map { i ->
            val angle = Math.toRadians(CORNER_STEP_DEG * i - CORNER_OFFSET_DEG)
            Point(c.x + hexWidth / SQRT3 * cos(angle), c.y + hexHeight / 2 * sin(angle))
        }
    }

    /** The side of the hex under [point] that is closest to it, as the pair of hexes it separates. */
    fun nearestSide(point: Point): Pair<HexCoord, HexCoord> {
        val hex = hexAt(point)
        val c = center(hex)
        val neighbor =
            HexDirections.ALL
                .map { hex + it }
                .minBy { n ->
                    val nc = center(n)
                    Point((c.x + nc.x) / 2, (c.y + nc.y) / 2).distanceTo(point)
                }
        return hex to neighbor
    }

    /** End points of the side shared by the adjacent hexes [a] and [b]. */
    fun sideEnds(
        a: HexCoord,
        b: HexCoord,
    ): Pair<Point, Point> {
        val ca = center(a)
        val cb = center(b)
        val mid = Point((ca.x + cb.x) / 2, (ca.y + cb.y) / 2)
        val (p, q) = corners(a).sortedBy { it.distanceTo(mid) }
        return p to q
    }

    /** Hexes whose centre falls inside a `width` × `height` image, row by row. */
    fun coordsInside(
        width: Double,
        height: Double,
    ): List<HexCoord> = coordsInside(0.0, 0.0, width, height)

    /** Hexes whose centre falls inside the rectangle from ([minX], [minY]) to ([maxX], [maxY]), row by row. */
    fun coordsInside(
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
    ): List<HexCoord> {
        val rMin = ceil((minY - originY) / rowStep).toInt()
        val rMax = floor((maxY - originY) / rowStep).toInt()
        val result = mutableListOf<HexCoord>()
        for (r in rMin..rMax) {
            val qMin = ceil((minX - originX) / hexWidth - r / 2.0).toInt()
            val qMax = floor((maxX - originX) / hexWidth - r / 2.0).toInt()
            for (q in qMin..qMax) result += HexCoord(q, r)
        }
        return result
    }

    companion object {
        val SQRT3: Double = sqrt(3.0)
        private const val ROW_FACTOR = 0.75
        private const val CORNERS = 6
        private const val CORNER_STEP_DEG = 60.0
        private const val CORNER_OFFSET_DEG = 30.0

        /** A regular hex of [width] (flat side to flat side). */
        fun regular(
            originX: Double,
            originY: Double,
            width: Double,
        ): HexLayout = HexLayout(originX, originY, width, width * 2 / SQRT3)

        private fun cubeRound(
            fq: Double,
            fr: Double,
        ): HexCoord {
            val fs = -fq - fr
            var q = fq.roundToInt()
            var r = fr.roundToInt()
            val s = fs.roundToInt()
            val dq = abs(q - fq)
            val dr = abs(r - fr)
            val ds = abs(s - fs)
            if (dq > dr && dq > ds) {
                q = -r - s
            } else if (dr > ds) {
                r = -q - s
            }
            return HexCoord(q, r)
        }
    }
}
