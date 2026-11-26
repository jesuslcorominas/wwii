package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/**
 * The nations of a theatre, the side each one starts on and the counters each one owns.
 *
 * It is data, not a global: every map or scenario can come with its own catalog. A [NationCatalog]
 * only exists validated: build it with [NationCatalog.build] or load it with `CatalogJson.decode`.
 */
class NationCatalog private constructor(
    val id: String,
    val name: String,
    val nations: List<Nation>,
    /** Counter counts in declaration order, one entry per nation and type. */
    val entries: List<CounterEntry>,
) {
    private val nationById: Map<NationId, Nation> = nations.associateBy { it.id }
    private val countByKey: Map<Pair<NationId, CounterType>, Int> =
        entries.associate { (it.nation to it.type) to it.count }

    /**
     * Every single counter, ordered by nation (catalog order), then type ([CounterType] order), then
     * number. Ids are deterministic: `DEU-L01`, `DEU-L02`, …
     */
    val counters: List<Counter> =
        nations.flatMap { nation ->
            CounterType.entries.flatMap { type ->
                (1..countOf(nation.id, type)).map { n -> Counter(CounterId.of(nation.id, type, n), nation.id, type) }
            }
        }

    val totalCounters: Int get() = counters.size

    operator fun contains(nation: NationId): Boolean = nation in nationById

    fun nation(id: NationId): Nation? = nationById[id]

    /** Side [nation] starts on. Throws [IllegalArgumentException] if the nation is not in the catalog. */
    fun initialSideOf(nation: NationId): Side =
        requireNotNull(nationById[nation]) { "Nation $nation is not in catalog $id" }.initialSide

    /** [initialSideOf] as a function, ready for `HexMap.territoriesBySide(catalog.sideOf)`. */
    val sideOf: (NationId) -> Side = ::initialSideOf

    /** Nations starting on [side], in catalog order. */
    fun nationsOf(side: Side): List<Nation> = nations.filter { it.initialSide == side }

    fun countOf(
        nation: NationId,
        type: CounterType,
    ): Int = countByKey[nation to type] ?: 0

    fun countersOf(nation: NationId): List<Counter> = counters.filter { it.nation == nation }

    override fun equals(other: Any?): Boolean =
        other is NationCatalog &&
            id == other.id &&
            name == other.name &&
            nations == other.nations &&
            entries == other.entries

    override fun hashCode(): Int = listOf(id, name, nations, entries).hashCode()

    override fun toString(): String = "NationCatalog($id, ${nations.size} nations, $totalCounters counters)"

    companion object {
        /** Validates and builds a catalog. Throws [InvalidCatalogException] listing every problem found. */
        fun build(
            id: String,
            name: String,
            nations: List<Nation>,
            counters: List<CounterEntry>,
        ): NationCatalog {
            val issues = CatalogValidator.validate(nations, counters)
            if (issues.isNotEmpty()) throw InvalidCatalogException(issues)
            return NationCatalog(id, name, nations, counters)
        }
    }
}
