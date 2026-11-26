package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.NationRole
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/** Checks a setup against its catalog and returns every problem found (empty when it is valid). */
internal object SetupValidator {
    private const val REINFORCEMENT = "reinforcement"
    private const val DEPLOYMENT = "deployment"

    fun validate(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> {
        val issues = mutableListOf<SetupIssue>()
        if (setup.catalogId != catalog.id) issues += SetupIssue.CatalogMismatch(setup.catalogId, catalog.id)
        if (setup.turns <= 0) issues += SetupIssue.NonPositiveTurns(setup.turns)

        issues += unknownNations(setup, catalog)
        issues += deploymentIssues(setup, catalog)
        issues += reinforcementIssues(setup, catalog)
        issues += specialIssues(setup, catalog)
        issues += catalogLimits(setup, catalog)
        return issues
    }

    private fun referencedNations(setup: GameSetup): List<NationId> =
        setup.deployment.map { it.nation } +
            setup.reinforcements.map { it.nation } +
            setup.specials.flatMap { special ->
                when (val effect = special.effect) {
                    is SpecialEffect.DeferredEntry -> effect.nations.map { it.nation }
                    is SpecialEffect.Volunteers -> listOf(effect.nation)
                    is SpecialEffect.KeyCityBonus -> effect.bonuses.map { it.nation }
                    is SpecialEffect.JoinSide -> listOf(effect.nation)
                }
            }

    private fun unknownNations(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> =
        referencedNations(setup).distinct().filter { it !in catalog }.map { SetupIssue.UnknownNation(it) }

    private fun countsIssue(
        where: String,
        nation: NationId,
        counts: CounterCounts,
    ): SetupIssue? = if (counts.hasNegative || counts.isEmpty) SetupIssue.InvalidCounts(where, nation, counts) else null

    private fun turnIssue(
        where: String,
        turn: Int,
        turns: Int,
    ): SetupIssue? = if (turns > 0 && turn !in 1..turns) SetupIssue.TurnOutOfRange(where, turn, turns) else null

    private fun deploymentIssues(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> {
        val issues = mutableListOf<SetupIssue>()
        setup.deployment.mapNotNullTo(issues) { countsIssue(DEPLOYMENT, it.nation, it.counts) }
        setup.deployment
            .groupingBy { it.nation to it.territory }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { (nation, territory) -> issues += SetupIssue.DuplicateDeployment(nation, territory) }
        setup.deployment
            .map { it.nation }
            .distinct()
            .filter { catalog.nation(it)?.let { n -> n.lateEntry || n.offMap } == true }
            .forEach { issues += SetupIssue.NotDeployable(it) }
        return issues
    }

    private fun reinforcementIssues(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> {
        val issues = mutableListOf<SetupIssue>()
        setup.reinforcements.forEach {
            turnIssue(REINFORCEMENT, it.turn, setup.turns)?.let(issues::add)
            countsIssue(REINFORCEMENT, it.nation, it.counts)?.let(issues::add)
        }
        setup.reinforcements
            .groupingBy { it.turn to it.nation }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { (turn, nation) -> issues += SetupIssue.DuplicateReinforcement(turn, nation) }
        setup.reinforcements
            .map { it.nation }
            .distinct()
            .filter { catalog.nation(it)?.role == NationRole.NEUTRAL }
            .forEach { issues += SetupIssue.NeutralReinforcement(it) }
        return issues
    }

    private fun specialIssues(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> {
        val issues = mutableListOf<SetupIssue>()
        setup.specials
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { issues += SetupIssue.DuplicateSpecial(it) }

        for (special in setup.specials) {
            val id = special.id
            val where = "special $id"
            when (val effect = special.effect) {
                is SpecialEffect.DeferredEntry -> {
                    if (effect.nations.isEmpty()) issues += SetupIssue.EmptySpecial(id)
                    for (deferred in effect.nations) {
                        turnIssue(where, deferred.turn, setup.turns)?.let(issues::add)
                        if (deferred.nation in catalog && setup.deployedCountOf(deferred.nation).isEmpty) {
                            issues += SetupIssue.DeferredWithoutDeployment(id, deferred.nation)
                        }
                    }
                }

                is SpecialEffect.Volunteers -> {
                    turnIssue(where, effect.turn, setup.turns)?.let(issues::add)
                    countsIssue(where, effect.nation, effect.counts)?.let(issues::add)
                    sideIssue(id, effect.nation, effect.side, catalog)?.let(issues::add)
                    val deployed = setup.deployedCountOf(effect.nation)
                    if (!effect.counts.hasNegative && CounterType.entries.any { effect.counts[it] > deployed[it] }) {
                        issues += SetupIssue.WithheldExceedsDeployment(id, effect.nation, effect.counts, deployed)
                    }
                }

                is SpecialEffect.KeyCityBonus -> {
                    if (effect.bonuses.isEmpty()) issues += SetupIssue.EmptySpecial(id)
                    for (bonus in effect.bonuses) {
                        countsIssue(where, bonus.nation, bonus.counts)?.let(issues::add)
                        val nationSide = catalog.nation(bonus.nation)?.initialSide
                        if (nationSide != null && (nationSide != bonus.condition.side || nationSide == Side.NEUTRAL)) {
                            issues += SetupIssue.InvalidSide(id, bonus.nation, bonus.condition.side)
                        }
                    }
                }

                is SpecialEffect.JoinSide ->
                    sideIssue(id, effect.nation, effect.side, catalog)?.let(issues::add)
            }
        }
        return issues
    }

    /** A nation can only be moved to a fighting side other than the one it starts on. */
    private fun sideIssue(
        id: SpecialId,
        nation: NationId,
        side: Side,
        catalog: NationCatalog,
    ): SetupIssue? {
        val initial = catalog.nation(nation)?.initialSide ?: return null
        val invalid = side == Side.NEUTRAL || side == initial
        return if (invalid) SetupIssue.InvalidSide(id, nation, side) else null
    }

    /** Worst case: base deployment and reinforcements plus every bonus of every special. */
    private fun catalogLimits(
        setup: GameSetup,
        catalog: NationCatalog,
    ): List<SetupIssue> {
        val bonuses =
            setup.specials
                .map { it.effect }
                .filterIsInstance<SpecialEffect.KeyCityBonus>()
                .flatMap { it.bonuses }
        val issues = mutableListOf<SetupIssue>()
        for (nation in catalog.nations) {
            val required =
                setup.baseTotalOf(nation.id) +
                    CounterCounts.sum(bonuses.filter { it.nation == nation.id }.map { it.counts })
            for (type in CounterType.entries) {
                val available = catalog.countOf(nation.id, type)
                if (required[type] > available) {
                    issues += SetupIssue.ExceedsCatalog(nation.id, type, required[type], available)
                }
            }
        }
        return issues
    }
}
