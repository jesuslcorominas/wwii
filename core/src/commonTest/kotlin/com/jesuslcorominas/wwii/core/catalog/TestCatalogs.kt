package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson

/**
 * Small invented catalog that fits `TestMaps.VALID` (nations AA and BB own territories there):
 *
 * - AA: major, axis, 3 land + 1 air.
 * - BB: minor, western allies, 2 land.
 * - CC: major, western allies, late entry and off the map, 1 land + 2 naval.
 *
 * [WITH_UNPLACED_NEUTRAL] adds DD, a neutral without counters and without territory on the test map.
 */
object TestCatalogs {
    const val VALID =
        """
        {
          "formatVersion": 1,
          "id": "test-small",
          "name": "Small test catalog",
          "nations": [
            { "id": "AA", "name": "Alfa", "role": "major", "side": "axis" },
            { "id": "BB", "name": "Bravo", "role": "minor", "side": "western_allies" },
            { "id": "CC", "name": "Charlie", "role": "major", "side": "western_allies", "lateEntry": true, "offMap": true }
          ],
          "counters": [
            { "nation": "AA", "type": "land", "count": 3 },
            { "nation": "AA", "type": "air", "count": 1 },
            { "nation": "BB", "type": "land", "count": 2 },
            { "nation": "CC", "type": "naval", "count": 2 },
            { "nation": "CC", "type": "land", "count": 1 }
          ]
        }
        """

    /** [VALID] plus a neutral nation without counters and without territory on the test map. */
    val WITH_UNPLACED_NEUTRAL =
        VALID.replace(
            """"offMap": true }""",
            """"offMap": true },
            { "id": "DD", "name": "Delta", "role": "neutral", "side": "neutral" }""",
        )

    fun valid(): NationCatalog = CatalogJson.decode(VALID)
}
