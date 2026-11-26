package com.jesuslcorominas.wwii.core.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexCoordTest {
    private val origin = HexCoord(0, 0)

    @Test
    fun cubeCoordinateSumsToZero() {
        val c = HexCoord(3, -5)
        assertEquals(2, c.s)
        assertEquals(0, c.q + c.r + c.s)
    }

    @Test
    fun hasSixDistinctNeighboursAtDistanceOne() {
        val center = HexCoord(2, -1)
        val neighbors = center.neighbors()
        assertEquals(6, neighbors.toSet().size)
        neighbors.forEach { assertEquals(1, center.distanceTo(it)) }
    }

    @Test
    fun neighborFollowsDirectionIndex() {
        HexDirections.ALL.forEachIndexed { i, delta ->
            assertEquals(origin + delta, origin.neighbor(i))
            assertEquals(i, HexDirections.between(origin, origin.neighbor(i)))
        }
    }

    @Test
    fun oppositeDirectionsCancel() {
        for (i in 0 until 3) {
            assertEquals(origin, HexDirections.delta(i) + HexDirections.delta(i + 3))
        }
    }

    @Test
    fun rejectsInvalidDirection() {
        assertFailsWith<IllegalArgumentException> { origin.neighbor(6) }
        assertFailsWith<IllegalArgumentException> { origin.neighbor(-1) }
    }

    @Test
    fun directionBetweenNonAdjacentHexesIsNull() {
        assertNull(HexDirections.between(origin, HexCoord(2, 0)))
        assertNull(HexDirections.between(origin, origin))
    }

    @Test
    fun distanceIsSymmetricAndUsesCubeMetric() {
        val a = HexCoord(-2, 3)
        val b = HexCoord(4, -1)
        assertEquals(6, a.distanceTo(b))
        assertEquals(6, b.distanceTo(a))
        assertEquals(0, a.distanceTo(a))
        assertEquals(3, origin.distanceTo(HexCoord(1, 2)))
    }

    @Test
    fun adjacency() {
        assertTrue(origin.isAdjacentTo(HexCoord(0, 1)))
        assertFalse(origin.isAdjacentTo(origin))
        assertFalse(origin.isAdjacentTo(HexCoord(1, 1)))
    }

    @Test
    fun lineToSelfIsSingleHex() {
        assertEquals(listOf(origin), origin.lineTo(origin))
    }

    @Test
    fun lineAlongAnAxis() {
        assertEquals(
            listOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(2, 0), HexCoord(3, 0)),
            origin.lineTo(HexCoord(3, 0)),
        )
    }

    @Test
    fun lineHasDistancePlusOneContiguousHexes() {
        val a = HexCoord(-3, 1)
        val b = HexCoord(4, -2)
        val line = a.lineTo(b)
        assertEquals(a.distanceTo(b) + 1, line.size)
        assertEquals(a, line.first())
        assertEquals(b, line.last())
        line.zipWithNext().forEach { (x, y) -> assertTrue(x.isAdjacentTo(y), "$x -> $y") }
    }

    @Test
    fun lineThroughHexBordersIsDeterministic() {
        // (0,0) -> (2,-1) passes exactly between two hexes; the tie must always break the same way.
        val expected = listOf(HexCoord(0, 0), HexCoord(1, 0), HexCoord(2, -1))
        assertEquals(expected, origin.lineTo(HexCoord(2, -1)))
    }

    @Test
    fun ringHasSixTimesRadiusHexesAtExactDistance() {
        val center = HexCoord(1, 1)
        for (radius in 1..4) {
            val ring = center.ring(radius)
            assertEquals(6 * radius, ring.size)
            assertEquals(ring.size, ring.toSet().size)
            ring.forEach { assertEquals(radius, center.distanceTo(it)) }
            ring.zipWithNext().forEach { (x, y) -> assertTrue(x.isAdjacentTo(y)) }
        }
        assertEquals(listOf(center), center.ring(0))
    }

    @Test
    fun rangeContainsEveryHexUpToRadius() {
        val range = origin.range(2)
        assertEquals(1 + 6 + 12, range.size)
        assertEquals(range.size, range.toSet().size)
        range.forEach { assertTrue(origin.distanceTo(it) <= 2) }
    }

    @Test
    fun negativeRadiusIsRejected() {
        assertFailsWith<IllegalArgumentException> { origin.ring(-1) }
        assertFailsWith<IllegalArgumentException> { origin.range(-1) }
    }
}
