package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.map.json.MapJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MapJsonTest {
    @Test
    fun loadsTheTestMap() {
        val map = MapJson.decode(TestMaps.VALID)
        assertEquals("test-small", map.id)
        assertEquals("Small test map", map.name)
        assertEquals(24, map.hexes.size)
        assertEquals(3, map.territories.size)
        val capital = map.hex(HexCoord(0, 0))!!
        assertEquals(City(CityId("alfa-city"), "Alfa City", CityKind.NATIONAL_CAPITAL), capital.city)
        assertEquals(CityKind.CITY, map.hex(HexCoord(4, 1))?.city?.kind)
        assertEquals(HexCoord(4, 1), map.cityHex(CityId("bravo-port")))
        assertEquals(null, map.cityHex(CityId("nowhere")))
        assertTrue(map.hex(HexCoord(1, 0))!!.mountain)
        assertEquals(Terrain.LAKE, map.hex(HexCoord(1, 1))?.terrain)
        assertTrue(map.territory(TerritoryId("AA"))!!.discontiguous)
    }

    @Test
    fun encodeThenDecodeGivesAnEqualMap() {
        val map = TestMaps.valid()
        assertEquals(map, MapJson.decode(MapJson.encode(map)))
        assertEquals(map, MapJson.decode(MapJson.encode(map, prettyPrint = true)))
    }

    @Test
    fun encodingIsStable() {
        val map = TestMaps.valid()
        val once = MapJson.encode(map)
        assertEquals(once, MapJson.encode(MapJson.decode(once)))
    }

    @Test
    fun encodeOmitsDefaultValuesAndWritesTheFormatVersion() {
        val text = MapJson.encode(TestMaps.valid())
        assertTrue(text.startsWith("{\"formatVersion\":1,"))
        assertTrue("{\"q\":5,\"r\":0,\"terrain\":\"sea\"}" in text)
        assertFalse("\"mountain\":false" in text)
        assertFalse("null" in text)
        assertTrue("\"strait\":[[2,1],[2,3]]" in text)
        assertTrue("{\"a\":[2,0],\"b\":[3,0]}" in text)
    }

    @Test
    fun rejectsUnknownFormatVersion() {
        val text = TestMaps.VALID.replace("\"formatVersion\": 1", "\"formatVersion\": 2")
        val e = assertFailsWith<InvalidMapException> { MapJson.decode(text) }
        assertEquals(listOf<MapIssue>(MapIssue.UnsupportedFormatVersion(2, 1)), e.issues)
        assertTrue("formatVersion 2" in e.message!!)
    }

    @Test
    fun rejectsMissingFormatVersion() {
        val text = TestMaps.VALID.replace("\"formatVersion\": 1,", "")
        assertMalformed(text)
    }

    @Test
    fun rejectsInvalidJson() {
        assertMalformed("{ not json")
        assertMalformed("[]")
    }

    @Test
    fun rejectsUnknownKeys() {
        assertMalformed(TestMaps.VALID.replace("\"mountain\": true", "\"montain\": true"))
    }

    @Test
    fun rejectsUnknownEnumValues() {
        assertMalformed(TestMaps.VALID.replace("\"terrain\": \"lake\"", "\"terrain\": \"swamp\""))
    }

    @Test
    fun rejectsMalformedCoordinates() {
        assertMalformed(TestMaps.VALID.replace("{ \"a\": [2, 0], \"b\": [3, 0] }", "{ \"a\": [2], \"b\": [3, 0] }"))
        assertMalformed(TestMaps.VALID.replace("\"strait\": [[2, 1], [2, 3]]", "\"strait\": [[2, 1]]"))
    }

    @Test
    fun rejectsBlankIdentifiers() {
        assertMalformed(TestMaps.VALID.replace("\"nation\": \"AA\"", "\"nation\": \" \""))
    }

    private fun assertMalformed(text: String) {
        val e = assertFailsWith<InvalidMapException> { MapJson.decode(text) }
        assertEquals(1, e.issues.size)
        assertIs<MapIssue.MalformedDocument>(e.issues.single())
    }
}
