package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HexMapTest {
    private val map = TestMaps.valid()

    @Test
    fun offBoardCoordinatesAreNotInTheMap() {
        assertTrue(HexCoord(0, 0) in map)
        assertFalse(HexCoord(-1, 0) in map)
        assertNull(map.hex(HexCoord(6, 0)))
    }

    @Test
    fun cornerHexOnlyHasOnBoardNeighbours() {
        val neighbors = map.neighborsInMap(HexCoord(0, 0)).map { it.coord }
        assertEquals(listOf(HexCoord(1, 0), HexCoord(0, 1)), neighbors)
    }

    @Test
    fun edgeHexOnTheFarSideHasOnlyOnBoardNeighbours() {
        val neighbors = map.neighborsInMap(HexCoord(5, 3)).map { it.coord }.toSet()
        assertEquals(setOf(HexCoord(5, 2), HexCoord(4, 3)), neighbors)
    }

    @Test
    fun interiorHexHasSixNeighbours() {
        assertEquals(6, map.neighborsInMap(HexCoord(2, 1)).size)
    }

    @Test
    fun coastIsLandNextToSea() {
        assertTrue(map.isCoastal(HexCoord(0, 1)))
        assertTrue(map.isCoastal(HexCoord(4, 1)))
        assertTrue(map.isCoastal(HexCoord(0, 3)))
    }

    @Test
    fun mapBorderAndLakesDoNotMakeCoast() {
        // (0,0) touches only off-board coordinates and land; (1,0) touches the lake.
        assertFalse(map.isCoastal(HexCoord(0, 0)))
        assertFalse(map.isCoastal(HexCoord(1, 0)))
    }

    @Test
    fun waterAndOffBoardHexesAreNeverCoastal() {
        assertFalse(map.isCoastal(HexCoord(5, 0)))
        assertFalse(map.isCoastal(HexCoord(1, 1)))
        assertFalse(map.isCoastal(HexCoord(9, 9)))
    }

    @Test
    fun riverIsOnTheSharedSideOnly() {
        assertTrue(map.hasRiver(HexCoord(2, 0), HexCoord(3, 0)))
        assertTrue(map.hasRiver(HexCoord(3, 0), HexCoord(2, 0)))
        assertFalse(map.hasRiver(HexCoord(2, 0), HexCoord(2, 1)))
        assertFalse(map.hasRiver(HexCoord(2, 0), HexCoord(4, 0)))
        assertEquals(setOf(HexEdge.of(HexCoord(2, 0), HexCoord(3, 0))), map.rivers)
    }

    @Test
    fun straitIsASeaHexWithTwoShores() {
        assertTrue(map.isStrait(HexCoord(2, 2)))
        assertEquals(HexCoord(2, 1) to HexCoord(2, 3), map.straitShores(HexCoord(2, 2)))
        assertFalse(map.isStrait(HexCoord(3, 2)))
        assertNull(map.straitShores(HexCoord(3, 2)))
        assertNull(map.straitShores(HexCoord(9, 9)))
        assertEquals(Terrain.SEA, map.hex(HexCoord(2, 2))?.terrain)
    }

    @Test
    fun citiesHaveAirports() {
        assertTrue(map.hex(HexCoord(0, 0))!!.hasAirport)
        assertTrue(map.hex(HexCoord(4, 1))!!.hasAirport)
        assertTrue(map.hex(HexCoord(3, 1))!!.hasAirport)
        assertFalse(map.hex(HexCoord(2, 0))!!.hasAirport)
    }

    @Test
    fun territoriesAreGroupedByNation() {
        assertEquals(listOf(NationId("AA"), NationId("BB")), map.nations())
        assertEquals(
            listOf(TerritoryId("BB"), TerritoryId("BC")),
            map.territoriesOf(NationId("BB")).map { it.id },
        )
        assertEquals(TerritoryKind.COLONY, map.territory(TerritoryId("BC"))?.kind)
        assertEquals(TerritoryId("AA"), map.territoryAt(HexCoord(0, 3))?.id)
        assertNull(map.territoryAt(HexCoord(5, 0)))
    }

    @Test
    fun hexesOfTerritory() {
        assertEquals(listOf(HexCoord(2, 3), HexCoord(3, 3)), map.hexesOf(TerritoryId("BC")))
        assertEquals(6, map.hexesOf(TerritoryId("AA")).size)
        assertEquals(emptyList(), map.hexesOf(TerritoryId("ZZ")))
    }

    @Test
    fun territoriesAreGroupedBySideWithAnExternalLookup() {
        val sides = mapOf(NationId("AA") to Side.AXIS, NationId("BB") to Side.WESTERN_ALLIES)
        val bySide = map.territoriesBySide { sides.getValue(it) }
        assertEquals(listOf(TerritoryId("AA")), bySide.getValue(Side.AXIS).map { it.id })
        assertEquals(
            listOf(TerritoryId("BB"), TerritoryId("BC")),
            bySide.getValue(Side.WESTERN_ALLIES).map { it.id },
        )
        assertFalse(Side.USSR in bySide)
    }
}
