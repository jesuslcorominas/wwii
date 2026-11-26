package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.NationRole
import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.setup.json.SetupJson
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Integrity of the real setup in `data/setup/europe-1939.json`; the figures are explained in docs/diseno-refuerzos.md. */
class Europe1939SetupTest {
    private val dataDir: File by lazy {
        File(requireNotNull(System.getProperty("wwii.dataDir")) { "wwii.dataDir is not set" })
    }

    private val catalog: NationCatalog by lazy {
        CatalogJson.decode(File(dataDir, "catalogs/europe-1939.json").readText())
    }

    private val setup: GameSetup by lazy {
        SetupJson.decode(File(dataDir, "setup/europe-1939.json").readText(), catalog)
    }

    private fun counts(nation: String) = setup.baseTotalOf(NationId(nation))

    @Test
    fun loadsWithoutIssues() {
        assertEquals("europe-1939", setup.id)
        assertEquals("europe-1939", setup.catalogId)
        assertEquals(18, setup.turns)
    }

    @Test
    fun roundTrip() {
        assertEquals(setup, SetupJson.decode(SetupJson.encode(setup), catalog))
    }

    @Test
    fun majorPowersDeployment() {
        val expected =
            mapOf(
                "DEU" to CounterCounts(12, 2, 3),
                "ITA" to CounterCounts(5, 3, 1),
                "GBR" to CounterCounts(3, 3, 2),
                "FRA" to CounterCounts(5, 1, 1),
                "USA" to CounterCounts.NONE,
                "SUN" to CounterCounts(10, 1, 1),
            )
        expected.forEach { (nation, counts) -> assertEquals(counts, setup.deployedCountOf(NationId(nation)), nation) }
    }

    @Test
    fun minorsAndNeutralsDeployEverythingAndGetNoReinforcements() {
        catalog.nations
            .filter { it.role != NationRole.MAJOR }
            .forEach { nation ->
                val all =
                    CounterCounts(
                        land = catalog.countOf(nation.id, CounterType.LAND),
                        naval = catalog.countOf(nation.id, CounterType.NAVAL),
                        air = catalog.countOf(nation.id, CounterType.AIR),
                    )
                assertEquals(all, setup.deployedCountOf(nation.id), nation.id.value)
                assertTrue(setup.reinforcementsOf(nation.id).isEmpty(), nation.id.value)
            }
    }

    @Test
    fun baseTotalsLeaveReservesForTheKeyCityBonuses() {
        assertEquals(CounterCounts(28, 3, 6), counts("DEU"))
        assertEquals(CounterCounts(11, 3, 2), counts("ITA"))
        assertEquals(CounterCounts(7, 4, 4), counts("GBR"))
        assertEquals(CounterCounts(6, 1, 1), counts("FRA"))
        assertEquals(CounterCounts(10, 3, 5), counts("USA"))
        assertEquals(CounterCounts(25, 1, 4), counts("SUN"))
    }

    @Test
    fun everyCounterIsUsedWithAllSpecialsActive() {
        val bonuses =
            setup.specials
                .map { it.effect }
                .filterIsInstance<SpecialEffect.KeyCityBonus>()
                .flatMap { it.bonuses }
        for (nation in catalog.nations) {
            val bonus = CounterCounts.sum(bonuses.filter { it.nation == nation.id }.map { it.counts })
            val total = setup.baseTotalOf(nation.id) + bonus
            CounterType.entries.forEach { type ->
                assertEquals(catalog.countOf(nation.id, type), total[type], "${nation.id} $type")
            }
        }
    }

    @Test
    fun unitedStatesArriveFromTurnEight() {
        val turns = setup.reinforcementsOf(NationId("USA")).map { it.turn }
        assertEquals(8, turns.min())
    }

    @Test
    fun specials() {
        assertEquals(
            listOf("axis-minors-late-entry", "spanish-volunteers", "key-cities", "spain-joins-axis"),
            setup.specials.map { it.id.value },
        )
    }

    @Test
    fun fitsTheRealMapWhenItExists() {
        val mapFile = File(dataDir, "maps/europe-1939.json")
        if (!mapFile.exists()) return
        val map = MapJson.decode(mapFile.readText())
        assertEquals(emptyList(), SetupMapValidator.validate(setup, catalog, map))
    }
}
