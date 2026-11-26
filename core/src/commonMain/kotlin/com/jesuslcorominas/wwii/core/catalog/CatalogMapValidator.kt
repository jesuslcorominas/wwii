package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.map.HexMap
import com.jesuslcorominas.wwii.core.nation.NationId

/** A mismatch between a nation catalog and the map it is played on. */
sealed class CatalogMapIssue {
    abstract val message: String

    override fun toString(): String = message

    /** A nation of the catalog that should be on the map owns no territory there. */
    data class NationWithoutTerritory(
        val nation: NationId,
    ) : CatalogMapIssue() {
        override val message get() = "Nation $nation has no territory on the map"
    }

    /** The map has territories of a nation the catalog does not know. */
    data class NationNotInCatalog(
        val nation: NationId,
    ) : CatalogMapIssue() {
        override val message get() = "Map nation $nation is not in the catalog"
    }

    /** A nation declared off the map owns territories on it. */
    data class OffMapNationWithTerritory(
        val nation: NationId,
    ) : CatalogMapIssue() {
        override val message get() = "Nation $nation is declared off the map but owns territories on it"
    }
}

/**
 * Checks that a catalog and a map fit together: every nation that is not `offMap` owns at least one
 * territory, every nation on the map is in the catalog, and off-map nations own nothing.
 *
 * Both are valid on their own; this is the cross check a game or scenario runs before using them
 * together.
 */
object CatalogMapValidator {
    fun validate(
        catalog: NationCatalog,
        map: HexMap,
    ): List<CatalogMapIssue> {
        val mapNations = map.nations().toSet()
        val issues = mutableListOf<CatalogMapIssue>()
        for (nation in catalog.nations) {
            val onMap = nation.id in mapNations
            when {
                nation.offMap && onMap -> issues += CatalogMapIssue.OffMapNationWithTerritory(nation.id)
                !nation.offMap && !onMap -> issues += CatalogMapIssue.NationWithoutTerritory(nation.id)
            }
        }
        map
            .nations()
            .filter { it !in catalog }
            .forEach { issues += CatalogMapIssue.NationNotInCatalog(it) }
        return issues
    }
}
