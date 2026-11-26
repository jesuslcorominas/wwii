package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId

/** [counts] of [nation] placed in [territory] when the game starts. */
data class Deployment(
    val nation: NationId,
    val territory: TerritoryId,
    val counts: CounterCounts,
)

/** [counts] that [nation] receives at the start of [turn] (1-based). */
data class Reinforcement(
    val turn: Int,
    val nation: NationId,
    val counts: CounterCounts,
)

/**
 * How a game starts and grows: the initial deployment, the base reinforcement table and the optional
 * special reinforcements, for the nations of one catalog.
 *
 * Every counter of the catalog enters at most once: deployment plus reinforcements plus every bonus
 * of every special never exceed the catalog. A [GameSetup] only exists validated against its catalog:
 * build it with [GameSetup.build] or load it with `SetupJson.decode`.
 */
class GameSetup private constructor(
    val id: String,
    val name: String,
    /** Id of the [NationCatalog] these data are written for. */
    val catalogId: String,
    /** Number of turns of the campaign; reinforcement turns go from 1 to [turns]. */
    val turns: Int,
    val deployment: List<Deployment>,
    val reinforcements: List<Reinforcement>,
    val specials: List<SpecialReinforcement>,
) {
    fun deploymentOf(nation: NationId): List<Deployment> = deployment.filter { it.nation == nation }

    fun reinforcementsAt(turn: Int): List<Reinforcement> = reinforcements.filter { it.turn == turn }

    fun reinforcementsOf(nation: NationId): List<Reinforcement> = reinforcements.filter { it.nation == nation }

    /** Counters [nation] deploys at the start (without specials). */
    fun deployedCountOf(nation: NationId): CounterCounts = CounterCounts.sum(deploymentOf(nation).map { it.counts })

    /** Counters [nation] receives over the whole campaign in the base table (without specials). */
    fun reinforcementCountOf(nation: NationId): CounterCounts =
        CounterCounts.sum(reinforcementsOf(nation).map { it.counts })

    /** Deployment plus base reinforcements of [nation]. */
    fun baseTotalOf(nation: NationId): CounterCounts = deployedCountOf(nation) + reinforcementCountOf(nation)

    fun special(id: SpecialId): SpecialReinforcement? = specials.firstOrNull { it.id == id }

    override fun equals(other: Any?): Boolean =
        other is GameSetup &&
            id == other.id &&
            name == other.name &&
            catalogId == other.catalogId &&
            turns == other.turns &&
            deployment == other.deployment &&
            reinforcements == other.reinforcements &&
            specials == other.specials

    override fun hashCode(): Int = listOf(id, name, catalogId, turns, deployment, reinforcements, specials).hashCode()

    override fun toString(): String =
        "GameSetup($id, $turns turns, ${deployment.size} deployments, ${reinforcements.size} reinforcements, " +
            "${specials.size} specials)"

    companion object {
        /** Validates against [catalog] and builds a setup. Throws [InvalidSetupException] listing every problem. */
        fun build(
            id: String,
            name: String,
            catalogId: String,
            turns: Int,
            deployment: List<Deployment>,
            reinforcements: List<Reinforcement>,
            specials: List<SpecialReinforcement>,
            catalog: NationCatalog,
        ): GameSetup {
            val setup = GameSetup(id, name, catalogId, turns, deployment, reinforcements, specials)
            val issues = SetupValidator.validate(setup, catalog)
            if (issues.isNotEmpty()) throw InvalidSetupException(issues)
            return setup
        }
    }
}
