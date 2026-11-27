package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.map.json.MapJson

/**
 * Small invented test map (axial coordinates, q to the right, r down the list):
 *
 * ```
 * r=0   AA*  AA^  AA  ~ BB  BB*  sea
 * r=1   AA   lake AA      BB+ BB.  sea
 * r=2   sea  sea  STR     sea sea  sea
 * r=3   AA   sea  BC*     BC  sea  sea
 * ```
 *
 * `*` capital, `^` mountain, `~` river between (2,0) and (3,0), `+` airport without city, `.` city.
 * AA is discontiguous (island at (0,3)); BC is a colony of BB joined to AA by the strait at (2,2).
 */
object TestMaps {
    const val VALID =
        """
        {
          "formatVersion": 1,
          "id": "test-small",
          "name": "Small test map",
          "territories": [
            { "id": "AA", "nation": "AA", "kind": "homeland", "name": "Alfa", "discontiguous": true },
            { "id": "BB", "nation": "BB", "kind": "homeland", "name": "Bravo" },
            { "id": "BC", "nation": "BB", "kind": "colony", "name": "Bravo colony" }
          ],
          "hexes": [
            { "q": 0, "r": 0, "terrain": "land", "territory": "AA", "city": { "id": "alfa-city", "name": "Alfa City", "kind": "national_capital" } },
            { "q": 1, "r": 0, "terrain": "land", "territory": "AA", "mountain": true },
            { "q": 2, "r": 0, "terrain": "land", "territory": "AA" },
            { "q": 3, "r": 0, "terrain": "land", "territory": "BB" },
            { "q": 4, "r": 0, "terrain": "land", "territory": "BB", "city": { "id": "bravo-city", "name": "Bravo City", "kind": "national_capital" } },
            { "q": 5, "r": 0, "terrain": "sea" },
            { "q": 0, "r": 1, "terrain": "land", "territory": "AA" },
            { "q": 1, "r": 1, "terrain": "lake" },
            { "q": 2, "r": 1, "terrain": "land", "territory": "AA" },
            { "q": 3, "r": 1, "terrain": "land", "territory": "BB", "airport": true },
            { "q": 4, "r": 1, "terrain": "land", "territory": "BB", "city": { "id": "bravo-port", "name": "Bravo Port" } },
            { "q": 5, "r": 1, "terrain": "sea" },
            { "q": 0, "r": 2, "terrain": "sea" },
            { "q": 1, "r": 2, "terrain": "sea" },
            { "q": 2, "r": 2, "terrain": "sea", "strait": [[2, 1], [2, 3]] },
            { "q": 3, "r": 2, "terrain": "sea" },
            { "q": 4, "r": 2, "terrain": "sea" },
            { "q": 5, "r": 2, "terrain": "sea" },
            { "q": 0, "r": 3, "terrain": "land", "territory": "AA" },
            { "q": 1, "r": 3, "terrain": "sea" },
            { "q": 2, "r": 3, "terrain": "land", "territory": "BC", "city": { "id": "colony-town", "name": "Colony Town", "kind": "territory_capital" } },
            { "q": 3, "r": 3, "terrain": "land", "territory": "BC" },
            { "q": 4, "r": 3, "terrain": "sea" },
            { "q": 5, "r": 3, "terrain": "sea" }
          ],
          "rivers": [
            { "a": [2, 0], "b": [3, 0] }
          ]
        }
        """

    /** Loads [VALID]. */
    fun valid(): HexMap = MapJson.decode(VALID)
}
