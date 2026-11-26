package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.HexCoord
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HexLayoutTest {
    private val layout = HexLayout(originX = 100.0, originY = 50.0, hexWidth = 40.0, hexHeight = 48.0)

    @Test
    fun centresFollowThePointyTopGrid() {
        assertEquals(Point(100.0, 50.0), layout.center(HexCoord(0, 0)))
        assertEquals(Point(140.0, 50.0), layout.center(HexCoord(1, 0)))
        // Next row: shifted half a hex to the right and 3/4 of the height down.
        assertEquals(Point(120.0, 86.0), layout.center(HexCoord(0, 1)))
        assertEquals(Point(80.0, 86.0), layout.center(HexCoord(-1, 1)))
    }

    @Test
    fun everyCentreMapsBackToItsHex() {
        HexCoord(0, 0).range(6).forEach { c ->
            assertEquals(c, layout.hexAt(layout.center(c)))
        }
    }

    @Test
    fun pointsNearTheCentreStayInTheHexEvenWithXyScaleDifferent() {
        val stretched = HexLayout(10.0, 10.0, hexWidth = 30.0, hexHeight = 50.0)
        HexCoord(2, 3).range(3).forEach { c ->
            val p = stretched.center(c)
            listOf(-8.0, 8.0).forEach { d ->
                assertEquals(c, stretched.hexAt(Point(p.x + d, p.y)))
                assertEquals(c, stretched.hexAt(Point(p.x, p.y + d * 2)))
            }
        }
    }

    @Test
    fun cornersAreOnTheHexOutline() {
        val corners = layout.corners(HexCoord(0, 0))
        assertEquals(6, corners.size)
        // Pointy top: one corner straight above the centre, at half the height.
        assertTrue(corners.any { abs(it.x - 100.0) < 1e-9 && abs(it.y - 26.0) < 1e-9 })
        assertTrue(corners.any { abs(it.x - 120.0) < 1e-9 && abs(it.y - 38.0) < 1e-9 })
    }

    @Test
    fun nearestSideIsTheOneTowardsThePoint() {
        val c = layout.center(HexCoord(0, 0))
        assertEquals(HexCoord(0, 0) to HexCoord(1, 0), layout.nearestSide(Point(c.x + 17, c.y)))
        assertEquals(HexCoord(0, 0) to HexCoord(-1, 0), layout.nearestSide(Point(c.x - 17, c.y)))
        assertEquals(HexCoord(0, 0) to HexCoord(0, 1), layout.nearestSide(Point(c.x + 8, c.y + 15)))
    }

    @Test
    fun sideEndsAreTheTwoSharedCorners() {
        val (p, q) = layout.sideEnds(HexCoord(0, 0), HexCoord(1, 0))
        val shared = layout.corners(HexCoord(1, 0))
        listOf(p, q).forEach { end -> assertTrue(shared.any { it.distanceTo(end) < 1e-9 }) }
        assertTrue(abs(p.x - 120.0) < 1e-9 && abs(q.x - 120.0) < 1e-9)
    }

    @Test
    fun coordsInsideCoverTheImage() {
        val grid = HexLayout.regular(originX = 20.0, originY = 20.0, width = 40.0)
        val coords = grid.coordsInside(width = 400.0, height = 300.0)
        assertTrue(coords.all { grid.center(it).let { p -> p.x in 0.0..400.0 && p.y in 0.0..300.0 } })
        assertTrue(HexCoord(0, 0) in coords)
        // Every pixel well inside the image falls in a listed hex.
        for (x in 30..370 step 17) {
            for (y in 30..270 step 13) {
                assertTrue(grid.hexAt(Point(x.toDouble(), y.toDouble())) in coords, "($x, $y)")
            }
        }
    }
}
