package com.jesuslcorominas.wwii.core.map

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HexEdgeTest {
    private val a = HexCoord(0, 0)
    private val b = HexCoord(1, -1)

    @Test
    fun edgeIsIndependentOfOrder() {
        assertEquals(HexEdge.of(a, b), HexEdge.of(b, a))
        assertEquals(HexEdge.of(a, b).hashCode(), HexEdge.of(b, a).hashCode())
        assertEquals(b, HexEdge.of(a, b).first)
    }

    @Test
    fun edgeTouchesBothHexes() {
        val edge = HexEdge.of(a, b)
        assertTrue(edge.touches(a))
        assertTrue(edge.touches(b))
        assertFalse(edge.touches(HexCoord(5, 5)))
    }

    @Test
    fun differentSidesOfAHexAreDifferentEdges() {
        assertEquals(
            6,
            a
                .neighbors()
                .map { HexEdge.of(a, it) }
                .toSet()
                .size,
        )
    }

    @Test
    fun nonAdjacentHexesHaveNoEdge() {
        assertFailsWith<IllegalArgumentException> { HexEdge.of(a, HexCoord(2, 0)) }
        assertFailsWith<IllegalArgumentException> { HexEdge.of(a, a) }
    }
}
