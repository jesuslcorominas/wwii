package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.TestCatalogs
import com.jesuslcorominas.wwii.core.setup.json.SetupJson

/**
 * Small invented setup for `TestCatalogs.VALID` that fits `TestMaps.VALID` (territories AA, BB and BC
 * are provisional ids of the test map, not of the real one):
 *
 * - AA (major, axis): deploys 2 land + 1 air in AA; keeps 1 land for the key-city bonus.
 * - BB (minor, western allies): deploys 1 land in BB and 1 in BC.
 * - CC (major, late entry, off map): 1 land + 2 naval in turn 3.
 * - One special of each kind.
 */
object TestSetups {
    const val VALID =
        """
        {
          "formatVersion": 1,
          "id": "test-small",
          "name": "Small test setup",
          "catalog": "test-small",
          "turns": 4,
          "deployment": [
            { "nation": "AA", "territory": "AA", "land": 2, "air": 1 },
            { "nation": "BB", "territory": "BB", "land": 1 },
            { "nation": "BB", "territory": "BC", "land": 1 }
          ],
          "reinforcements": [
            { "turn": 3, "nation": "CC", "land": 1, "naval": 2 }
          ],
          "specials": [
            {
              "type": "deferred_entry", "id": "late-bravo", "name": "Bravo enters late",
              "nations": [ { "nation": "BB", "turn": 2 } ]
            },
            {
              "type": "volunteers", "id": "bravo-volunteers", "name": "Bravo volunteers",
              "nation": "BB", "side": "axis", "turn": 3, "land": 1
            },
            {
              "type": "key_city_bonus", "id": "key-cities", "name": "Key cities",
              "bonuses": [ { "cityId": "bravo-city", "side": "axis", "nation": "AA", "land": 1 } ]
            },
            {
              "type": "join_side", "id": "bravo-joins-axis", "name": "Bravo joins the Axis",
              "nation": "BB", "side": "axis",
              "condition": { "type": "controls_city", "side": "axis", "cityId": "colony-town" }
            }
          ]
        }
        """

    fun valid(): GameSetup = SetupJson.decode(VALID, TestCatalogs.valid())
}
