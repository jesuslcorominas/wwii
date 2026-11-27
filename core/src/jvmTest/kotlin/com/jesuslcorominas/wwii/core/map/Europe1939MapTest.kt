package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.catalog.CatalogMapValidator
import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integrity of the real map in `data/maps/europe-1939.json`, alone and with the real catalog. */
class Europe1939MapTest {
    private val dataDir: File by lazy {
        File(requireNotNull(System.getProperty("wwii.dataDir")) { "wwii.dataDir is not set" })
    }

    private val mapText: String by lazy { File(dataDir, "maps/europe-1939.json").readText() }

    // MapJson.decode throws InvalidMapException with every issue if any map rule is broken.
    private val map: HexMap by lazy { MapJson.decode(mapText) }

    private val catalog: NationCatalog by lazy {
        CatalogJson.decode(File(dataDir, "catalogs/europe-1939.json").readText())
    }

    private val cities: Map<String, Hex> by lazy {
        map.hexes.filter { it.city != null }.associateBy { it.city!!.id.value }
    }

    @Test
    fun passesEveryMapRule() {
        assertEquals("europe-1939", map.id)
        assertEquals(TERRITORY_IDS.size, map.territories.size)
    }

    @Test
    fun territoryIdsAreStable() {
        // Other data (initial deployment, scenarios) refers to these ids.
        assertEquals(TERRITORY_IDS, map.territories.map { it.id.value })
    }

    @Test
    fun fitsTheCatalog() {
        assertEquals(emptyList(), CatalogMapValidator.validate(catalog, map))
    }

    @Test
    fun everyTerritoryHasACity() {
        val withoutCity =
            map.territories.map { it.id }.filter { territory ->
                map.hexesOf(territory).none { map.hex(it)?.city != null }
            }
        assertEquals(emptyList(), withoutCity)
    }

    @Test
    fun everyNationOnTheMapHasItsCapital() {
        val homelands = map.territories.filter { it.kind == TerritoryKind.HOMELAND }
        val withoutCapital =
            homelands.filter { territory ->
                map.hexesOf(territory.id).none { map.hex(it)?.city?.kind == CityKind.NATIONAL_CAPITAL }
            }
        assertEquals(emptyList(), withoutCapital.map { it.id.value })
    }

    @Test
    fun cityIdsUsedByOtherDataExist() {
        // Referenced by data/setup/europe-1939.json and, later, by the rule sets.
        listOf("suez", "stalingrad", "rome", "bucharest", "gibraltar").forEach {
            assertTrue(map.cityHex(CityId(it)) != null, "missing city $it")
        }
    }

    @Test
    fun hasTheObjectiveCities() {
        val missing = OBJECTIVE_CITIES.filter { it !in cities }
        assertEquals(emptyList(), missing)
        assertEquals(36, OBJECTIVE_CITIES.size)
    }

    @Test
    fun objectiveCitiesBelongToTheExpectedNations() {
        val expected =
            mapOf(
                "london" to "GBR",
                "paris" to "FRA",
                "moscow" to "SUN",
                "berlin" to "DEU",
                "rome" to "ITA",
                "suez" to "GBR",
                "tripoli" to "ITA",
                "gibraltar" to "GBR",
                "valletta" to "GBR",
                "algiers" to "FRA",
                "casablanca" to "FRA",
                "amman" to "GBR",
                "tunis" to "FRA",
                "istanbul" to "TUR",
            )
        val actual = expected.keys.associateWith { name -> map.territoryAt(cities.getValue(name).coord)?.nation }
        assertEquals(expected.mapValues { NationId(it.value) }, actual)
    }

    @Test
    fun hasTheRequiredStraits() {
        val shores =
            REQUIRED_STRAITS.mapValues { (_, sea) ->
                map.straitShores(sea)?.let { (a, b) ->
                    setOf(map.territoryAt(a)!!.id.value, map.territoryAt(b)!!.id.value)
                }
            }
        assertEquals(REQUIRED_STRAITS.keys.associateWith { STRAIT_SHORES.getValue(it) }, shores)
    }

    @Test
    fun isInTheEditorCanonicalForm() {
        // Opening and saving the map in :mapeditor must not change the file.
        assertEquals(mapText, MapJson.encode(MapJson.decodeDraft(mapText), prettyPrint = true) + "\n")
    }

    private companion object {
        val TERRITORY_IDS =
            listOf(
                "DEU",
                "ITA",
                "FIN",
                "ROU",
                "HUN",
                "BGR",
                "GBR",
                "FRA",
                "NOR",
                "NLD",
                "BEL",
                "GRC",
                "DNK",
                "SUN",
                "POL",
                "YUG",
                "ESP",
                "TUR",
                "SWE",
                "CHE",
                "IRL",
                "PRT",
                "DEU-SVK",
                "ITA-LBY",
                "ITA-ALB",
                "ITA-DOD",
                "GBR-EGY",
                "GBR-PSE",
                "GBR-JOR",
                "GBR-CYP",
                "GBR-MLT",
                "GBR-GIB",
                "FRA-DZA",
                "FRA-TUN",
                "FRA-MAR",
                "FRA-SYR",
                "ESP-MAR",
                "DNK-ISL",
                "SUN-BAL",
            )

        val OBJECTIVE_CITIES =
            listOf(
                "london",
                "paris",
                "moscow",
                "berlin",
                "rome",
                "stalingrad",
                "leningrad",
                "suez",
                "tripoli",
                "helsinki",
                "gibraltar",
                "kiev",
                "valletta",
                "copenhagen",
                "oslo",
                "athens",
                "sofia",
                "bucharest",
                "budapest",
                "belgrade",
                "warsaw",
                "brussels",
                "rotterdam",
                "algiers",
                "casablanca",
                "madrid",
                "barcelona",
                "dublin",
                "ankara",
                "istanbul",
                "bern",
                "stockholm",
                "amman",
                "tunis",
                "kharkov",
                "kursk",
            )

        // Sea hex of each strait (axial coordinates, see tools/mapgen/europe1939.py) and its shores.
        val REQUIRED_STRAITS =
            mapOf(
                "Gibraltar" to HexCoord(-16, 38),
                "Dardanelos" to HexCoord(11, 35),
                "Bósforo" to HexCoord(13, 34),
                "Mesina" to HexCoord(1, 38),
                "Kerch" to HexCoord(21, 28),
                "Dover" to HexCoord(1, 22),
                "Øresund" to HexCoord(10, 18),
                "Bonifacio" to HexCoord(-2, 34),
            )

        val STRAIT_SHORES =
            mapOf(
                "Gibraltar" to setOf("GBR-GIB", "ESP-MAR"),
                "Dardanelos" to setOf("TUR"),
                "Bósforo" to setOf("TUR"),
                "Mesina" to setOf("ITA"),
                "Kerch" to setOf("SUN"),
                "Dover" to setOf("GBR", "FRA"),
                "Øresund" to setOf("DNK", "SWE"),
                "Bonifacio" to setOf("FRA", "ITA"),
            )
    }
}
