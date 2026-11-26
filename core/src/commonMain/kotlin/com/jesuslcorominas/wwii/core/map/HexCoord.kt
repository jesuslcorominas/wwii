package com.jesuslcorominas.wwii.core.map

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Axial hex coordinate. The third cube coordinate is derived: `s = -q - r`.
 *
 * The engine does not know whether hexes are drawn flat or pointy, nor their size in pixels: that
 * is a layout concern of `ui` and the map editor. Directions are therefore plain indexes 0..5
 * (see [HexDirections]), never compass names.
 */
data class HexCoord(
    val q: Int,
    val r: Int,
) : Comparable<HexCoord> {
    val s: Int get() = -q - r

    operator fun plus(other: HexCoord): HexCoord = HexCoord(q + other.q, r + other.r)

    operator fun minus(other: HexCoord): HexCoord = HexCoord(q - other.q, r - other.r)

    fun scale(factor: Int): HexCoord = HexCoord(q * factor, r * factor)

    /** Neighbour in [direction] (0..5). */
    fun neighbor(direction: Int): HexCoord = this + HexDirections.delta(direction)

    /** The six neighbours in direction order 0..5, whether or not they are on the map. */
    fun neighbors(): List<HexCoord> = HexDirections.ALL.map { this + it }

    fun isAdjacentTo(other: HexCoord): Boolean = distanceTo(other) == 1

    fun distanceTo(other: HexCoord): Int {
        val d = this - other
        return max(abs(d.q), max(abs(d.r), abs(d.s)))
    }

    /**
     * Hexes on the straight line from this hex to [target], both included, in order.
     *
     * Uses cube interpolation with a fixed tiny offset so that points falling exactly on a hex
     * border always round the same way: the result is identical on every platform.
     */
    fun lineTo(target: HexCoord): List<HexCoord> {
        val n = distanceTo(target)
        if (n == 0) return listOf(this)
        val aq = q + NUDGE_Q
        val ar = r + NUDGE_R
        val bq = target.q + NUDGE_Q
        val br = target.r + NUDGE_R
        return (0..n).map { i ->
            val t = i.toDouble() / n
            cubeRound(aq + (bq - aq) * t, ar + (br - ar) * t)
        }
    }

    /** Hexes at exactly [radius] from this one, starting at direction 4 and walking directions 0..5. */
    fun ring(radius: Int): List<HexCoord> {
        require(radius >= 0) { "radius must be >= 0, was $radius" }
        if (radius == 0) return listOf(this)
        val result = ArrayList<HexCoord>(6 * radius)
        var current = this + HexDirections.delta(RING_START_DIRECTION).scale(radius)
        for (side in 0 until HexDirections.COUNT) {
            repeat(radius) {
                result += current
                current = current.neighbor(side)
            }
        }
        return result
    }

    /** Hexes at distance 0..[radius] from this one, by increasing distance. */
    fun range(radius: Int): List<HexCoord> {
        require(radius >= 0) { "radius must be >= 0, was $radius" }
        return (0..radius).flatMap { ring(it) }
    }

    override fun compareTo(other: HexCoord): Int = compareValuesBy(this, other, { it.r }, { it.q })

    override fun toString(): String = "($q,$r)"

    private companion object {
        const val RING_START_DIRECTION = 4
        const val NUDGE_Q = 1e-6
        const val NUDGE_R = 2e-6

        fun cubeRound(
            fq: Double,
            fr: Double,
        ): HexCoord {
            val fs = -fq - fr
            var rq = fq.roundToInt()
            var rr = fr.roundToInt()
            val rs = fs.roundToInt()
            val dq = abs(rq - fq)
            val dr = abs(rr - fr)
            val ds = abs(rs - fs)
            if (dq > dr && dq > ds) {
                rq = -rr - rs
            } else if (dr > ds) {
                rr = -rq - rs
            }
            return HexCoord(rq, rr)
        }
    }
}

/** The six axial directions, indexed 0..5. Consecutive indexes are consecutive around a hex. */
object HexDirections {
    const val COUNT = 6

    val ALL: List<HexCoord> =
        listOf(
            HexCoord(1, 0),
            HexCoord(1, -1),
            HexCoord(0, -1),
            HexCoord(-1, 0),
            HexCoord(-1, 1),
            HexCoord(0, 1),
        )

    fun delta(direction: Int): HexCoord {
        require(direction in 0 until COUNT) { "direction must be in 0..5, was $direction" }
        return ALL[direction]
    }

    /** Direction from [from] to the adjacent hex [to], or null if they are not adjacent. */
    fun between(
        from: HexCoord,
        to: HexCoord,
    ): Int? = ALL.indexOf(to - from).takeIf { it >= 0 }
}
