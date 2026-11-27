package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.map.CityId
import com.jesuslcorominas.wwii.core.map.HexMap
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

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

    /** A nation deploys in a territory of a nation that does not start on its side (or of another neutral). */
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

    /** A special reinforcement refers to a city id that the map does not have. */
    data class UnknownCity(
        val special: SpecialId,
        val cityId: String,
    ) : SetupMapIssue() {
        override val message get() = "Special $special references unknown city $cityId"
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
 * with an airport, and every city a special reinforcement refers to exists on the map.
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
            // Neutrals are nobody's allies: a neutral only deploys in its own territories.
            val foreign =
                ownSide != null &&
                    ownerSide != null &&
                    (ownSide != ownerSide || (ownSide == Side.NEUTRAL && nation != territory.nation))
            if (foreign) {
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
        for (special in setup.specials) {
            citiesOf(special.effect)
                .distinct()
                .filter { map.cityHex(CityId(it)) == null }
                .forEach { issues += SetupMapIssue.UnknownCity(special.id, it) }
        }
        return issues
    }

    private fun citiesOf(effect: SpecialEffect): List<String> =
        when (effect) {
            is SpecialEffect.KeyCityBonus -> effect.bonuses.map { it.condition.cityId }
            is SpecialEffect.JoinSide -> listOfNotNull((effect.condition as? SpecialCondition.ControlsCity)?.cityId)
            is SpecialEffect.DeferredEntry, is SpecialEffect.Volunteers -> emptyList()
        }
}
