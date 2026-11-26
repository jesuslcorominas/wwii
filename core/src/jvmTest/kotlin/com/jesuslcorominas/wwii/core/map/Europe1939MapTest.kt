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
        map.hexes.filter { it.city != null }.associateBy { it.city!!.name }
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
    fun hasTheObjectiveCities() {
        val missing = OBJECTIVE_CITIES.filter { it !in cities }
        assertEquals(emptyList(), missing)
        assertEquals(36, OBJECTIVE_CITIES.size)
    }

    @Test
    fun objectiveCitiesBelongToTheExpectedNations() {
        val expected =
            mapOf(
                "Londres" to "GBR",
                "París" to "FRA",
                "Moscú" to "SUN",
                "Berlín" to "DEU",
                "Roma" to "ITA",
                "Suez" to "GBR",
                "Trípoli" to "ITA",
                "Gibraltar" to "GBR",
                "La Valeta" to "GBR",
                "Argel" to "FRA",
                "Casablanca" to "FRA",
                "Amán" to "GBR",
                "Túnez" to "FRA",
                "Estambul" to "TUR",
            )
        val actual = expected.keys.associateWith { name -> map.territoryAt(cities.getValue(name).coord)?.nation }
        assertEquals(expected.mapValues { NationId(it.value) }, actual)
    }

    @Test
    fun hasTheRequiredStraits() {
        val straits = map.hexes.filter { it.strait != null }
        assertTrue(straits.size >= REQUIRED_STRAITS.size, "straits: ${straits.size}")
        // Each required strait joins land of the expected territories.
        val shorePairs =
            straits.map { hex ->
                val (a, b) = map.straitShores(hex.coord)!!
                setOf(map.territoryAt(a)!!.id.value, map.territoryAt(b)!!.id.value)
            }
        REQUIRED_STRAITS.forEach { (name, shores) ->
            assertTrue(shores in shorePairs, "missing strait $name between $shores")
        }
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
                "Londres",
                "París",
                "Moscú",
                "Berlín",
                "Roma",
                "Stalingrado",
                "Leningrado",
                "Suez",
                "Trípoli",
                "Helsinki",
                "Gibraltar",
                "Kiev",
                "La Valeta",
                "Copenhague",
                "Oslo",
                "Atenas",
                "Sofía",
                "Bucarest",
                "Budapest",
                "Belgrado",
                "Varsovia",
                "Bruselas",
                "Róterdam",
                "Argel",
                "Casablanca",
                "Madrid",
                "Barcelona",
                "Dublín",
                "Ankara",
                "Estambul",
                "Berna",
                "Estocolmo",
                "Amán",
                "Túnez",
                "Járkov",
                "Kursk",
            )

        val REQUIRED_STRAITS =
            listOf(
                "Gibraltar" to setOf("GBR-GIB", "ESP-MAR"),
                "Dardanelos" to setOf("TUR"),
                "Bósforo" to setOf("TUR"),
                "Mesina" to setOf("ITA"),
                "Kerch" to setOf("SUN"),
                "Dover" to setOf("GBR", "FRA"),
                "Øresund" to setOf("DNK", "SWE"),
            )
    }
}
