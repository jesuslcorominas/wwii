package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.map.MapIssue.LandFeature
import com.jesuslcorominas.wwii.core.map.MapIssue.RiverProblem
import com.jesuslcorominas.wwii.core.map.MapIssue.ShoreProblem
import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** One test per validation rule, each with a small invalid JSON map. */
class MapValidationTest {
    private val homeA = """{ "id": "A", "nation": "NA", "kind": "homeland", "name": "A" }"""
    private val homeB = """{ "id": "B", "nation": "NB", "kind": "homeland", "name": "B" }"""
    private val colonyA = """{ "id": "AC", "nation": "NA", "kind": "colony", "name": "A colony" }"""

    private fun land(
        q: Int,
        r: Int,
        territory: String = "A",
        extra: String = "",
    ) = """{ "q": $q, "r": $r, "terrain": "land", "territory": "$territory"$extra }"""

    private fun sea(
        q: Int,
        r: Int,
        extra: String = "",
    ) = """{ "q": $q, "r": $r, "terrain": "sea"$extra }"""

    private fun river(
        a: String,
        b: String,
    ) = """{ "a": $a, "b": $b }"""

    private fun json(
        territories: List<String> = listOf(homeA),
        hexes: List<String>,
        rivers: List<String> = emptyList(),
    ) = """
        {
          "formatVersion": 1, "id": "t", "name": "t",
          "territories": [${territories.joinToString(",")}],
          "hexes": [${hexes.joinToString(",")}],
          "rivers": [${rivers.joinToString(",")}]
        }
        """

    private fun issuesOf(text: String): List<MapIssue> =
        assertFailsWith<InvalidMapException> {
            MapJson.decode(text)
        }.issues

    private fun assertSingleIssue(
        expected: MapIssue,
        text: String,
    ) = assertEquals(listOf(expected), issuesOf(text))

    @Test
    fun minimalMapIsValid() {
        MapJson.decode(json(hexes = listOf(land(0, 0))))
    }

    @Test
    fun duplicateHex() {
        assertSingleIssue(
            MapIssue.DuplicateHex(HexCoord(0, 0)),
            json(hexes = listOf(land(0, 0), sea(0, 0), sea(0, 0))),
        )
    }

    @Test
    fun duplicateTerritory() {
        assertSingleIssue(
            MapIssue.DuplicateTerritory(TerritoryId("A")),
            json(territories = listOf(homeA, homeA), hexes = listOf(land(0, 0))),
        )
    }

    @Test
    fun unknownTerritory() {
        assertSingleIssue(
            MapIssue.UnknownTerritory(HexCoord(1, 0), TerritoryId("X")),
            json(hexes = listOf(land(0, 0), land(1, 0, territory = "X"))),
        )
    }

    @Test
    fun landWithoutTerritory() {
        assertSingleIssue(
            MapIssue.LandWithoutTerritory(HexCoord(1, 0)),
            json(hexes = listOf(land(0, 0), """{ "q": 1, "r": 0, "terrain": "land" }""")),
        )
    }

    @Test
    fun waterWithTerritory() {
        assertSingleIssue(
            MapIssue.WaterWithTerritory(HexCoord(1, 0)),
            json(hexes = listOf(land(0, 0), sea(1, 0, """, "territory": "A""""))),
        )
    }

    @Test
    fun cityAirportAndMountainOnlyOnLand() {
        val water =
            """{ "q": 1, "r": 0, "terrain": "lake", "city": { "name": "X" }, "airport": true, "mountain": true }"""
        assertEquals(
            listOf(
                MapIssue.LandFeatureOnWater(HexCoord(1, 0), LandFeature.CITY),
                MapIssue.LandFeatureOnWater(HexCoord(1, 0), LandFeature.AIRPORT),
                MapIssue.LandFeatureOnWater(HexCoord(1, 0), LandFeature.MOUNTAIN),
            ),
            issuesOf(json(hexes = listOf(land(0, 0), water))),
        )
    }

    @Test
    fun emptyTerritory() {
        assertSingleIssue(
            MapIssue.EmptyTerritory(TerritoryId("B")),
            json(territories = listOf(homeA, homeB), hexes = listOf(land(0, 0))),
        )
    }

    @Test
    fun disconnectedTerritoryMustBeDeclared() {
        val hexes = listOf(land(0, 0), sea(1, 0), land(2, 0), sea(3, 0), land(4, 0))
        assertSingleIssue(MapIssue.DisconnectedTerritory(TerritoryId("A"), 3), json(hexes = hexes))

        val declared = """{ "id": "A", "nation": "NA", "kind": "homeland", "name": "A", "discontiguous": true }"""
        MapJson.decode(json(territories = listOf(declared), hexes = hexes))
    }

    @Test
    fun straitJoinsTheShoresOfATerritory() {
        val hexes = listOf(land(0, 0), sea(1, 0, """, "strait": [[0, 0], [2, 0]]"""), land(2, 0))
        MapJson.decode(json(hexes = hexes))
    }

    @Test
    fun aNationCanOwnSeparateTerritories() {
        val hexes = listOf(land(0, 0), sea(1, 0), land(2, 0, territory = "AC"))
        MapJson.decode(json(territories = listOf(homeA, colonyA), hexes = hexes))
    }

    @Test
    fun riverMustJoinTwoAdjacentLandHexesOnTheMap() {
        val hexes = listOf(land(0, 0), land(1, 0), sea(0, 1))
        assertEquals(
            listOf(
                MapIssue.InvalidRiver(HexCoord(0, 0), HexCoord(2, 0), RiverProblem.NOT_ADJACENT),
                MapIssue.InvalidRiver(HexCoord(1, 0), HexCoord(2, 0), RiverProblem.OFF_MAP),
                MapIssue.InvalidRiver(HexCoord(0, 0), HexCoord(0, 1), RiverProblem.NOT_LAND),
            ),
            issuesOf(
                json(
                    hexes = hexes,
                    rivers = listOf(river("[0, 0]", "[2, 0]"), river("[1, 0]", "[2, 0]"), river("[0, 0]", "[0, 1]")),
                ),
            ),
        )
    }

    @Test
    fun duplicateRiverInEitherOrder() {
        assertSingleIssue(
            MapIssue.DuplicateRiver(HexCoord(1, 0), HexCoord(0, 0)),
            json(
                hexes = listOf(land(0, 0), land(1, 0)),
                rivers = listOf(river("[0, 0]", "[1, 0]"), river("[1, 0]", "[0, 0]")),
            ),
        )
    }

    @Test
    fun straitMustBeASeaHex() {
        val hexes =
            listOf(
                land(0, 0),
                """{ "q": 1, "r": 0, "terrain": "lake", "strait": [[0, 0], [2, 0]] }""",
                land(2, 0),
                land(1, 1),
            )
        assertSingleIssue(MapIssue.StraitNotOnSea(HexCoord(1, 0)), json(hexes = hexes))
    }

    @Test
    fun straitShoresMustBeTwoDistinctAdjacentLandHexesOnTheMap() {
        val hexes =
            listOf(
                land(0, 0),
                sea(1, 0, """, "strait": [[0, 0], [0, 0]]"""),
                sea(1, 1, """, "strait": [[3, 1], [0, 1]]"""),
                sea(0, 1),
                sea(2, 2, """, "strait": [[2, 3], [5, 5]]"""),
            )
        assertEquals(
            listOf(
                MapIssue.InvalidStraitShore(HexCoord(1, 0), HexCoord(0, 0), ShoreProblem.SAME_SHORE),
                MapIssue.InvalidStraitShore(HexCoord(1, 1), HexCoord(3, 1), ShoreProblem.NOT_ADJACENT),
                MapIssue.InvalidStraitShore(HexCoord(1, 1), HexCoord(0, 1), ShoreProblem.NOT_LAND),
                MapIssue.InvalidStraitShore(HexCoord(2, 2), HexCoord(2, 3), ShoreProblem.OFF_MAP),
                MapIssue.InvalidStraitShore(HexCoord(2, 2), HexCoord(5, 5), ShoreProblem.NOT_ADJACENT),
            ),
            issuesOf(json(hexes = hexes)),
        )
    }

    @Test
    fun oneNationalCapitalPerNationInItsHomeland() {
        val capital = """, "city": { "name": "C", "kind": "national_capital" }"""
        assertSingleIssue(
            MapIssue.DuplicateNationalCapital(NationId("NA"), listOf(HexCoord(0, 0), HexCoord(1, 0))),
            json(hexes = listOf(land(0, 0, extra = capital), land(1, 0, extra = capital))),
        )
        assertSingleIssue(
            MapIssue.NationalCapitalOutsideHomeland(HexCoord(2, 0)),
            json(
                territories = listOf(homeA, colonyA),
                hexes = listOf(land(0, 0), sea(1, 0), land(2, 0, territory = "AC", extra = capital)),
            ),
        )
    }

    @Test
    fun oneTerritoryCapitalPerColonyOrPossession() {
        val capital = """, "city": { "name": "C", "kind": "territory_capital" }"""
        assertSingleIssue(
            MapIssue.DuplicateTerritoryCapital(TerritoryId("AC"), listOf(HexCoord(2, 0), HexCoord(3, 0))),
            json(
                territories = listOf(homeA, colonyA),
                hexes =
                    listOf(
                        land(0, 0),
                        sea(1, 0),
                        land(2, 0, territory = "AC", extra = capital),
                        land(3, 0, territory = "AC", extra = capital),
                    ),
            ),
        )
        assertSingleIssue(
            MapIssue.TerritoryCapitalInHomeland(HexCoord(0, 0)),
            json(hexes = listOf(land(0, 0, extra = capital))),
        )
    }

    @Test
    fun reportsEveryIssueAtOnce() {
        val text =
            json(
                territories = listOf(homeA, homeB),
                hexes =
                    listOf(
                        land(0, 0),
                        land(0, 0),
                        sea(1, 0, """, "territory": "A""""),
                        land(2, 0, territory = "X"),
                    ),
                rivers = listOf(river("[0, 0]", "[5, 5]")),
            )
        assertEquals(
            listOf(
                MapIssue.DuplicateHex(HexCoord(0, 0)),
                MapIssue.WaterWithTerritory(HexCoord(1, 0)),
                MapIssue.UnknownTerritory(HexCoord(2, 0), TerritoryId("X")),
                MapIssue.EmptyTerritory(TerritoryId("B")),
                MapIssue.InvalidRiver(HexCoord(0, 0), HexCoord(5, 5), RiverProblem.NOT_ADJACENT),
            ),
            issuesOf(text),
        )
    }

    @Test
    fun buildingInCodeAppliesTheSameRules() {
        val e =
            assertFailsWith<InvalidMapException> {
                HexMap.build("t", "t", emptyList(), listOf(Hex(HexCoord(0, 0), Terrain.LAND)))
            }
        assertEquals(listOf<MapIssue>(MapIssue.LandWithoutTerritory(HexCoord(0, 0))), e.issues)
    }
}
