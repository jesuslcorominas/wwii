package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapDraftTest {
    private val invalidDraft =
        MapDraft(
            id = "wip",
            name = "Work in progress",
            territories =
                listOf(
                    Territory(TerritoryId("AA"), NationId("AA"), TerritoryKind.HOMELAND, "Alfa"),
                    Territory(TerritoryId("ZZ"), NationId("ZZ"), TerritoryKind.COLONY, "Empty"),
                ),
            hexes =
                listOf(
                    Hex(HexCoord(0, 0), Terrain.LAND, TerritoryId("AA")),
                    Hex(HexCoord(1, 0), Terrain.LAND),
                    Hex(HexCoord(2, 0), Terrain.SEA, strait = Strait(HexCoord(1, 0), HexCoord(9, 9))),
                ),
            rivers = listOf(HexCoord(1, 0) to HexCoord(0, 0)),
        )

    @Test
    fun encodingTheDraftOfAMapIsEncodingTheMap() {
        val map = TestMaps.valid()
        assertEquals(MapJson.encode(map), MapJson.encode(MapDraft.of(map)))
        assertEquals(MapJson.encode(map, prettyPrint = true), MapJson.encode(MapDraft.of(map), prettyPrint = true))
    }

    @Test
    fun theDraftOfAValidMapHasNoIssuesAndBuildsTheSameMap() {
        val map = TestMaps.valid()
        val draft = MapDraft.of(map)
        assertEquals(emptyList(), draft.issues())
        assertEquals(map, draft.toMap())
        assertEquals(draft, MapJson.decodeDraft(TestMaps.VALID))
    }

    @Test
    fun anInvalidDraftListsItsIssuesInsteadOfThrowing() {
        val issues = invalidDraft.issues()
        assertTrue(MapIssue.LandWithoutTerritory(HexCoord(1, 0)) in issues)
        assertTrue(MapIssue.EmptyTerritory(TerritoryId("ZZ")) in issues)
        val farShore = MapIssue.InvalidStraitShore(HexCoord(2, 0), HexCoord(9, 9), MapIssue.ShoreProblem.NOT_ADJACENT)
        assertTrue(farShore in issues)
        val e = assertFailsWith<InvalidMapException> { invalidDraft.toMap() }
        assertEquals(issues, e.issues)
    }

    @Test
    fun anInvalidDraftSurvivesAJsonRoundTrip() {
        val text = MapJson.encode(invalidDraft, prettyPrint = true)
        val decoded = MapJson.decodeDraft(text)
        assertEquals(invalidDraft, decoded)
        assertEquals(text, MapJson.encode(decoded, prettyPrint = true))
        assertFailsWith<InvalidMapException> { MapJson.decode(text) }
    }

    @Test
    fun decodeDraftStillRejectsWhatIsNotAMap() {
        val wrongVersion = TestMaps.VALID.replace("\"formatVersion\": 1", "\"formatVersion\": 2")
        val e = assertFailsWith<InvalidMapException> { MapJson.decodeDraft(wrongVersion) }
        assertEquals(listOf<MapIssue>(MapIssue.UnsupportedFormatVersion(2, 1)), e.issues)

        val typo = TestMaps.VALID.replace("\"mountain\"", "\"montain\"")
        assertIs<MapIssue.MalformedDocument>(
            assertFailsWith<InvalidMapException> { MapJson.decodeDraft(typo) }.issues.single(),
        )

        val blankTerritory =
            """{"formatVersion":1,"id":"x","name":"x","hexes":[{"q":0,"r":0,"terrain":"land","territory":" "}]}"""
        assertIs<MapIssue.MalformedDocument>(
            assertFailsWith<InvalidMapException> { MapJson.decodeDraft(blankTerritory) }.issues.single(),
        )
    }
}
