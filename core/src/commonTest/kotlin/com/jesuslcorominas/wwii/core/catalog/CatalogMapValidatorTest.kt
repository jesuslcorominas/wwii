package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TestMaps
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogMapValidatorTest {
    private val map = TestMaps.valid()

    @Test
    fun matchingCatalogAndMapHaveNoIssues() {
        assertEquals(emptyList(), CatalogMapValidator.validate(TestCatalogs.valid(), map))
    }

    @Test
    fun catalogNationWithoutTerritory() {
        val catalog = CatalogJson.decode(TestCatalogs.WITH_UNPLACED_NEUTRAL)
        assertEquals(
            listOf<CatalogMapIssue>(CatalogMapIssue.NationWithoutTerritory(NationId("DD"))),
            CatalogMapValidator.validate(catalog, map),
        )
    }

    @Test
    fun mapNationMissingFromCatalog() {
        val catalog =
            CatalogJson.decode(
                TestCatalogs.VALID
                    .replace("""{ "id": "BB", "name": "Bravo", "role": "minor", "side": "western_allies" },""", "")
                    .replace("""{ "nation": "BB", "type": "land", "count": 2 },""", ""),
            )
        assertEquals(
            listOf<CatalogMapIssue>(CatalogMapIssue.NationNotInCatalog(NationId("BB"))),
            CatalogMapValidator.validate(catalog, map),
        )
    }

    @Test
    fun offMapNationWithTerritory() {
        val catalog =
            CatalogJson.decode(
                TestCatalogs.VALID.replace(
                    """"side": "western_allies" }""",
                    """"side": "western_allies", "offMap": true }""",
                ),
            )
        assertEquals(
            listOf<CatalogMapIssue>(CatalogMapIssue.OffMapNationWithTerritory(NationId("BB"))),
            CatalogMapValidator.validate(catalog, map),
        )
    }

    @Test
    fun catalogGivesTheMapItsSides() {
        val bySide = map.territoriesBySide(TestCatalogs.valid().sideOf)
        assertEquals(listOf(TerritoryId("AA")), bySide[Side.AXIS]?.map { it.id })
        assertEquals(listOf(TerritoryId("BB"), TerritoryId("BC")), bySide[Side.WESTERN_ALLIES]?.map { it.id })
    }
}
