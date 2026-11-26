package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integrity of the real catalog in `data/catalogs/europe-1939.json`. */
class Europe1939CatalogTest {
    private val catalog: NationCatalog by lazy {
        val dataDir = requireNotNull(System.getProperty("wwii.dataDir")) { "wwii.dataDir is not set" }
        CatalogJson.decode(File(dataDir, "catalogs/europe-1939.json").readText())
    }

    private fun ids(side: Side) = catalog.nationsOf(side).map { it.id.value }

    @Test
    fun loadsWithoutIssues() {
        assertEquals("europe-1939", catalog.id)
        assertEquals(23, catalog.nations.size)
    }

    @Test
    fun nationsStartOnTheirSides() {
        assertEquals(listOf("DEU", "ITA", "FIN", "ROU", "HUN", "BGR"), ids(Side.AXIS))
        assertEquals(listOf("GBR", "FRA", "USA", "NOR", "NLD", "BEL", "GRC", "DNK"), ids(Side.WESTERN_ALLIES))
        assertEquals(listOf("SUN", "POL", "YUG"), ids(Side.USSR))
        assertEquals(listOf("ESP", "TUR", "SWE", "CHE", "IRL", "PRT"), ids(Side.NEUTRAL))
    }

    @Test
    fun nationCodesMatchTheCounterDesign() {
        // DESIGN.md (Fichas): every nation with counters, plus DNK and IRL without them; PRT added by #5.
        val withCounters = catalog.nations.filter { catalog.countersOf(it.id).isNotEmpty() }.map { it.id.value }
        assertEquals(
            listOf(
                "DEU",
                "ITA",
                "FIN",
                "ROU",
                "HUN",
                "BGR",
                "GBR",
                "FRA",
                "USA",
                "NOR",
                "NLD",
                "BEL",
                "GRC",
                "SUN",
                "POL",
                "YUG",
                "ESP",
                "TUR",
                "SWE",
                "CHE",
            ).sorted(),
            withCounters.sorted(),
        )
        val without = catalog.nations.map { it.id.value } - withCounters.toSet()
        assertEquals(listOf("DNK", "IRL", "PRT"), without.sorted())
    }

    @Test
    fun totalsPerSide() {
        assertEquals(152, catalog.totalCounters)
        val bySide = catalog.counters.groupingBy { catalog.initialSideOf(it.nation) }.eachCount()
        assertEquals(mapOf(Side.AXIS to 61, Side.WESTERN_ALLIES to 46, Side.USSR to 34, Side.NEUTRAL to 11), bySide)
    }

    @Test
    fun countersPerNation() {
        val expected =
            mapOf(
                "DEU" to Triple(30, 3, 6),
                "ITA" to Triple(11, 3, 2),
                "GBR" to Triple(8, 4, 4),
                "FRA" to Triple(6, 1, 1),
                "USA" to Triple(10, 3, 5),
                "SUN" to Triple(26, 1, 4),
                "SWE" to Triple(2, 1, 0),
                "FIN" to Triple(2, 0, 0),
                "ROU" to Triple(2, 0, 0),
                "POL" to Triple(2, 0, 0),
                "ESP" to Triple(3, 0, 0),
                "TUR" to Triple(3, 0, 0),
                "CHE" to Triple(2, 0, 0),
            )
        for ((code, counts) in expected) {
            val id = NationId(code)
            val actual =
                Triple(
                    catalog.countOf(id, CounterType.LAND),
                    catalog.countOf(id, CounterType.NAVAL),
                    catalog.countOf(id, CounterType.AIR),
                )
            assertEquals(counts, actual, "counters of $code")
        }
        for (code in listOf("HUN", "BGR", "NOR", "NLD", "BEL", "GRC", "YUG")) {
            assertEquals(1, catalog.countersOf(NationId(code)).size, "counters of $code")
        }
    }

    @Test
    fun onlyTheUnitedStatesEntersLateAndOffTheMap() {
        assertEquals(listOf("USA"), catalog.nations.filter { it.lateEntry }.map { it.id.value })
        assertEquals(listOf("USA"), catalog.nations.filter { it.offMap }.map { it.id.value })
    }

    @Test
    fun counterIdsAreUnique() {
        assertEquals(
            catalog.counters.size,
            catalog.counters
                .map { it.id }
                .toSet()
                .size,
        )
        assertTrue(catalog.counters.first().id == CounterId("DEU-L01"))
    }
}
