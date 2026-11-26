package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.TestCatalogs
import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TestMaps
import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetupMapValidatorTest {
    private val catalog = TestCatalogs.valid()
    private val map = TestMaps.valid()
    private val aa = NationId("AA")
    private val bb = NationId("BB")

    private fun setupWith(
        deployment: List<Deployment> = TestSetups.valid().deployment,
        specials: List<SpecialReinforcement> = TestSetups.valid().specials,
        against: NationCatalog = catalog,
    ): GameSetup {
        val base = TestSetups.valid()
        return GameSetup.build(
            base.id,
            base.name,
            base.catalogId,
            base.turns,
            deployment,
            base.reinforcements,
            specials,
            against,
        )
    }

    @Test
    fun testSetupFitsTestMap() {
        assertTrue(SetupMapValidator.validate(TestSetups.valid(), catalog, map).isEmpty())
    }

    @Test
    fun unknownTerritory() {
        val setup =
            setupWith(
                deployment =
                    TestSetups.valid().deployment.map {
                        if (it.nation == aa) it.copy(territory = TerritoryId("ZZ")) else it
                    },
            )
        assertEquals(
            listOf<SetupMapIssue>(SetupMapIssue.UnknownTerritory(aa, TerritoryId("ZZ"))),
            SetupMapValidator.validate(setup, catalog, map),
        )
    }

    @Test
    fun territoryOfAnotherSide() {
        val setup =
            setupWith(
                deployment =
                    listOf(
                        Deployment(aa, TerritoryId("BC"), CounterCounts(land = 1)),
                        Deployment(bb, TerritoryId("BB"), CounterCounts(land = 2)),
                    ),
            )
        assertEquals(
            listOf<SetupMapIssue>(SetupMapIssue.TerritoryOfAnotherSide(aa, TerritoryId("BC"), bb)),
            SetupMapValidator.validate(setup, catalog, map),
        )
    }

    @Test
    fun navalNeedsCoastAndAirNeedsAirport() {
        // IN is an inland possession of AA: no sea next to it and no city or airport.
        val landlocked =
            MapJson.decode(
                """
                {
                  "formatVersion": 1, "id": "inland", "name": "Inland",
                  "territories": [
                    { "id": "AA", "nation": "AA", "kind": "homeland", "name": "Alfa" },
                    { "id": "IN", "nation": "AA", "kind": "possession", "name": "Inland" },
                    { "id": "BB", "nation": "BB", "kind": "homeland", "name": "Bravo" }
                  ],
                  "hexes": [
                    { "q": 0, "r": 0, "terrain": "land", "territory": "AA" },
                    { "q": 1, "r": 0, "terrain": "land", "territory": "IN" },
                    { "q": 2, "r": 0, "terrain": "land", "territory": "AA" },
                    { "q": 0, "r": 1, "terrain": "land", "territory": "AA" },
                    { "q": 1, "r": 1, "terrain": "land", "territory": "AA" },
                    { "q": 3, "r": 0, "terrain": "land", "territory": "BB" },
                    { "q": 4, "r": 0, "terrain": "sea" }
                  ]
                }
                """,
            )
        // Same catalog, but AA also has one naval counter.
        val withNavy =
            CatalogJson.decode(
                TestCatalogs.VALID.replace(
                    """{ "nation": "AA", "type": "air", "count": 1 },""",
                    """{ "nation": "AA", "type": "air", "count": 1 },
                    { "nation": "AA", "type": "naval", "count": 1 },""",
                ),
            )
        val setup =
            setupWith(
                deployment =
                    listOf(
                        Deployment(aa, TerritoryId("IN"), CounterCounts(land = 1, naval = 1, air = 1)),
                        Deployment(bb, TerritoryId("BB"), CounterCounts(land = 2)),
                    ),
                against = withNavy,
            )
        assertEquals(
            listOf<SetupMapIssue>(
                SetupMapIssue.NoCoast(aa, TerritoryId("IN")),
                SetupMapIssue.NoAirport(aa, TerritoryId("IN")),
            ),
            SetupMapValidator.validate(setup, withNavy, landlocked),
        )
    }

    @Test
    fun neutralsOnlyDeployInTheirOwnTerritories() {
        // Two neutrals, each with its own country on a tiny map.
        val neutrals =
            CatalogJson.decode(
                """
                {
                  "formatVersion": 1, "id": "test-small", "name": "Neutrals",
                  "nations": [
                    { "id": "AA", "name": "Alfa", "role": "neutral", "side": "neutral" },
                    { "id": "BB", "name": "Bravo", "role": "neutral", "side": "neutral" }
                  ],
                  "counters": [ { "nation": "AA", "type": "land", "count": 2 } ]
                }
                """,
            )
        val setup =
            GameSetup.build(
                "n",
                "N",
                "test-small",
                4,
                listOf(
                    Deployment(aa, TerritoryId("AA"), CounterCounts(land = 1)),
                    Deployment(aa, TerritoryId("BB"), CounterCounts(land = 1)),
                ),
                emptyList(),
                emptyList(),
                neutrals,
            )
        assertEquals(
            listOf<SetupMapIssue>(SetupMapIssue.TerritoryOfAnotherSide(aa, TerritoryId("BB"), bb)),
            SetupMapValidator.validate(setup, neutrals, map),
        )
    }
}
