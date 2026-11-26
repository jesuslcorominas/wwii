package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.map.HexMap
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId

/** A mismatch between a setup and the map it is played on. */
sealed class SetupMapIssue {
    abstract val message: String

    override fun toString(): String = message

    data class UnknownTerritory(
        val nation: NationId,
        val territory: TerritoryId,
    ) : SetupMapIssue() {
        override val message get() = "Deployment of $nation references unknown territory $territory"
    }

    /** A nation deploys in a territory of a nation that does not start on its side. */
    data class TerritoryOfAnotherSide(
        val nation: NationId,
        val territory: TerritoryId,
        val owner: NationId,
    ) : SetupMapIssue() {
        override val message get() = "Nation $nation deploys in $territory, which belongs to $owner of another side"
    }

    data class NoCoast(
        val nation: NationId,
        val territory: TerritoryId,
    ) : SetupMapIssue() {
        override val message get() = "Nation $nation deploys naval counters in $territory, which has no coast"
    }

    data class NoAirport(
        val nation: NationId,
        val territory: TerritoryId,
    ) : SetupMapIssue() {
        override val message get() = "Nation $nation deploys air counters in $territory, which has no airport"
    }
}

/**
 * Checks that a setup fits a map: every deployment territory exists and belongs to a nation of the
 * same starting side, naval counters go to territories with coast and air counters to territories
 * with an airport.
 *
 * The cities of the specials are referenced by a stable city id that the map model does not have
 * yet; checking them belongs here once it does.
 *
 * The setup is valid against its catalog on its own; this is the cross check a game or scenario
 * runs before using setup, catalog and map together.
 */
object SetupMapValidator {
    fun validate(
        setup: GameSetup,
        catalog: NationCatalog,
        map: HexMap,
    ): List<SetupMapIssue> {
        val issues = mutableListOf<SetupMapIssue>()
        for (deployment in setup.deployment) {
            val nation = deployment.nation
            val territory = map.territory(deployment.territory)
            if (territory == null) {
                issues += SetupMapIssue.UnknownTerritory(nation, deployment.territory)
                continue
            }
            val ownSide = catalog.nation(nation)?.initialSide
            val ownerSide = catalog.nation(territory.nation)?.initialSide
            if (ownSide != null && ownerSide != null && ownSide != ownerSide) {
                issues += SetupMapIssue.TerritoryOfAnotherSide(nation, territory.id, territory.nation)
            }
            val coords = map.hexesOf(territory.id)
            if (deployment.counts.naval > 0 && coords.none { map.isCoastal(it) }) {
                issues += SetupMapIssue.NoCoast(nation, territory.id)
            }
            if (deployment.counts.air > 0 && coords.none { map.hex(it)?.hasAirport == true }) {
                issues += SetupMapIssue.NoAirport(nation, territory.id)
            }
        }
        return issues
    }
}
