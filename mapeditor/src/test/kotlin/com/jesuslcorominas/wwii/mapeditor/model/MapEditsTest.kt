package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.CityId
import com.jesuslcorominas.wwii.core.map.CityKind
import com.jesuslcorominas.wwii.core.map.Hex
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.MapIssue
import com.jesuslcorominas.wwii.core.map.Strait
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TerritoryKind
import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapEditsTest {
    private val aa = TerritoryId("AA")
    private val alfa = Territory(aa, NationId("AA"), TerritoryKind.HOMELAND, "Alfa")
    private val empty = MapDraft("m", "Map")
    private val c00 = HexCoord(0, 0)
    private val c10 = HexCoord(1, 0)
    private val c20 = HexCoord(2, 0)

    private fun land() =
        empty
            .addTerritory(alfa)
            .paintTerritory(c00, aa)
            .paintTerritory(c10, aa)

    @Test
    fun setTerrainAddsNewHexesAtTheEnd() {
        val d = empty.setTerrain(c10, Terrain.SEA).setTerrain(c00, Terrain.LAKE)
        assertEquals(listOf(Hex(c10, Terrain.SEA), Hex(c00, Terrain.LAKE)), d.hexes)
    }

    @Test
    fun turningLandIntoWaterDropsLandFeaturesAndRivers() {
        val d =
            land()
                .toggleMountain(c00)
                .toggleAirport(c00)
                .setCity(c00, City(CityId("x"), "X"))
                .toggleRiver(c00, c10)
                .setTerrain(c00, Terrain.SEA)
        assertEquals(Hex(c00, Terrain.SEA), d.hexAt(c00))
        assertTrue(d.rivers.isEmpty())
    }

    @Test
    fun turningWaterIntoLandKeepsNoStrait() {
        val d =
            land()
                .setTerrain(c20, Terrain.SEA)
                .setStrait(c20, c10, HexCoord(2, -1))
                .setTerrain(c20, Terrain.LAND)
        assertNull(d.hexAt(c20)!!.strait)
        assertEquals(Terrain.LAND, d.hexAt(c20)!!.terrain)
    }

    @Test
    fun aNoOpReturnsTheSameDraft() {
        val d = land()
        assertSame(d, d.setTerrain(c00, Terrain.LAND))
        assertSame(d, d.paintTerritory(c00, aa))
        assertSame(d, d.toggleMountain(HexCoord(9, 9)))
        assertSame(d, d.removeHex(HexCoord(9, 9)))
        assertSame(d, d.toggleRiver(c00, c20))
        assertSame(d, d.withMetadata("m", "Map"))
    }

    @Test
    fun paintingATerritoryMakesLandAndKeepsLandFeatures() {
        val bb = TerritoryId("BB")
        val d =
            land()
                .toggleMountain(c00)
                .addTerritory(alfa.copy(id = bb, name = "B"))
                .paintTerritory(c00, bb)
                .setTerrain(c20, Terrain.SEA)
                .paintTerritory(c20, bb)
        assertEquals(Hex(c00, Terrain.LAND, bb, mountain = true), d.hexAt(c00))
        assertEquals(Hex(c20, Terrain.LAND, bb), d.hexAt(c20))
    }

    @Test
    fun landFeaturesAreIgnoredOnWater() {
        val d = empty.setTerrain(c00, Terrain.SEA)
        assertSame(d, d.toggleMountain(c00))
        assertSame(d, d.toggleAirport(c00))
        assertSame(d, d.setCity(c00, City(CityId("x"), "X")))
    }

    @Test
    fun citiesAreSetAndRemoved() {
        val d = land().setCity(c00, City(CityId("alfa"), "Alfa", CityKind.NATIONAL_CAPITAL))
        assertEquals(City(CityId("alfa"), "Alfa", CityKind.NATIONAL_CAPITAL), d.hexAt(c00)!!.city)
        assertEquals(setOf(CityId("alfa")), d.cityIds())
        assertNull(d.setCity(c00, null).hexAt(c00)!!.city)
    }

    @Test
    fun riversToggleWhicheverWayTheSideIsNamed() {
        val d = land().toggleRiver(c10, c00)
        assertEquals(listOf(c00 to c10), d.rivers)
        assertTrue(d.hasRiver(c10, c00))
        assertFalse(d.toggleRiver(c00, c10).hasRiver(c00, c10))
    }

    @Test
    fun removingAHexDropsItsRivers() {
        val d = land().toggleRiver(c00, c10).removeHex(c10)
        assertNull(d.hexAt(c10))
        assertTrue(d.rivers.isEmpty())
    }

    @Test
    fun straitsOnlyGoOnSea() {
        val shoreB = HexCoord(2, -1)
        val d = land().setTerrain(c20, Terrain.SEA)
        assertSame(d, d.setStrait(c00, c10, shoreB))
        val withStrait = d.setStrait(c20, c10, shoreB)
        assertEquals(Strait(c10, shoreB), withStrait.hexAt(c20)!!.strait)
        assertNull(withStrait.clearStrait(c20).hexAt(c20)!!.strait)
    }

    @Test
    fun renamingATerritoryMovesItsHexes() {
        val zz = TerritoryId("ZZ")
        val d = land().updateTerritory(aa, alfa.copy(id = zz, name = "Zulu"))
        assertEquals(listOf(alfa.copy(id = zz, name = "Zulu")), d.territories)
        assertTrue(d.hexes.all { it.territory == zz })
        assertEquals(emptyList(), d.issues())
    }

    @Test
    fun removingATerritoryLeavesLandWithoutTerritory() {
        val d = land().removeTerritory(aa)
        assertTrue(d.territories.isEmpty())
        assertTrue(MapIssue.LandWithoutTerritory(c00) in d.issues())
    }

    @Test
    fun brokenRiversInALoadedDraftDoNotBreakEditing() {
        val d = land().copy(rivers = listOf(c00 to c20))
        assertFalse(d.hasRiver(c00, c10))
        assertEquals(listOf(c00 to c20, c00 to c10), d.toggleRiver(c00, c10).rivers)
    }

    @Test
    fun proposedCityIdIsAnAsciiSlugOfTheName() {
        assertEquals("brest-litovsk", proposedCityId("Brest-Litovsk"))
        assertEquals("lodz", proposedCityId("Łódź"))
        assertEquals("la-coruna", proposedCityId("La Coruña"))
        assertEquals("deir-ez-zor", proposedCityId(" Deir ez-Zor "))
        assertEquals("", proposedCityId("¿?"))
        assertEquals("oresund", proposedCityId("Øresund"))
        assertEquals("strasse", proposedCityId("Straße"))
        assertEquals("aeroskobing", proposedCityId("Ærøskøbing"))
        assertTrue(CityId(proposedCityId("Göteborg")).isWellFormed)
    }
}
