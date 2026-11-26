package com.jesuslcorominas.wwii.core.map

/**
 * The side shared by two adjacent hexes. Canonical: `HexEdge.of(a, b) == HexEdge.of(b, a)`, with
 * [first] always the smaller coordinate. Used for rivers.
 */
class HexEdge private constructor(
    val first: HexCoord,
    val second: HexCoord,
) {
    fun touches(coord: HexCoord): Boolean = coord == first || coord == second

    override fun equals(other: Any?): Boolean = other is HexEdge && first == other.first && second == other.second

    override fun hashCode(): Int = 31 * first.hashCode() + second.hashCode()

    override fun toString(): String = "$first|$second"

    companion object {
        /** Edge between [a] and [b]. Throws if they are not adjacent. */
        fun of(
            a: HexCoord,
            b: HexCoord,
        ): HexEdge {
            require(a.isAdjacentTo(b)) { "$a and $b are not adjacent" }
            return if (a <= b) HexEdge(a, b) else HexEdge(b, a)
        }
    }
}
