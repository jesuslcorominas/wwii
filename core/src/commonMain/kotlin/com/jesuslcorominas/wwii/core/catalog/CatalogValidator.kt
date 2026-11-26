package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.Side

/** Checks every catalog rule and returns all the problems found (empty when the catalog is valid). */
internal object CatalogValidator {
    fun validate(
        nations: List<Nation>,
        counters: List<CounterEntry>,
    ): List<CatalogIssue> {
        val issues = mutableListOf<CatalogIssue>()
        if (nations.isEmpty()) issues += CatalogIssue.NoNations

        nations
            .groupingBy { it.id }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { issues += CatalogIssue.DuplicateNation(it) }

        for (nation in nations.distinctBy { it.id }) {
            val isNeutral = nation.role == NationRole.NEUTRAL
            if (isNeutral != (nation.initialSide == Side.NEUTRAL)) {
                issues += CatalogIssue.RoleSideMismatch(nation.id, nation.role, nation.initialSide)
            }
            if (isNeutral && nation.lateEntry) issues += CatalogIssue.LateEntryNeutral(nation.id)
        }

        val known = nations.map { it.id }.toSet()
        counters
            .map { it.nation }
            .distinct()
            .filter { it !in known }
            .forEach { issues += CatalogIssue.UnknownNation(it) }

        counters
            .groupingBy { it.nation to it.type }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { (nation, type) -> issues += CatalogIssue.DuplicateCounterEntry(nation, type) }

        counters
            .filter { it.count <= 0 }
            .forEach { issues += CatalogIssue.NonPositiveCount(it.nation, it.type, it.count) }

        val withCounters = counters.filter { it.count > 0 }.map { it.nation }.toSet()
        nations
            .distinctBy { it.id }
            .filter { it.role == NationRole.MAJOR && it.id !in withCounters }
            .forEach { issues += CatalogIssue.MajorWithoutCounters(it.id) }

        return issues
    }
}
