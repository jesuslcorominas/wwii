package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/**
 * The board as data: a sparse set of hexes (anything not listed is off the board), the territories
 * they belong to, and the hex sides crossed by rivers.
 *
 * A [HexMap] only exists validated: build it with [HexMap.build] or load it with `MapJson.decode`.
 */
class HexMap private constructor(
    val id: String,
    val name: String,
    val territories: List<Territory>,
    hexes: List<Hex>,
    val rivers: Set<HexEdge>,
) {
    private val hexByCoord: Map<HexCoord, Hex> = hexes.associateBy { it.coord }
    private val territoryById: Map<TerritoryId, Territory> = territories.associateBy { it.id }
    private val coordByCity: Map<CityId, HexCoord> =
        hexes.mapNotNull { hex -> hex.city?.let { it.id to hex.coord } }.toMap()
    private val coordsByTerritory: Map<TerritoryId, List<HexCoord>> =
        hexes.filter { it.territory != null }.groupBy({ it.territory!! }, { it.coord })

    /** Every hex in declaration order. */
    val hexes: Collection<Hex> get() = hexByCoord.values

    operator fun contains(coord: HexCoord): Boolean = coord in hexByCoord

    fun hex(coord: HexCoord): Hex? = hexByCoord[coord]

    /** Hex of the city with this id, or null if the map has no such city. */
    fun cityHex(id: CityId): HexCoord? = coordByCity[id]

    /** Neighbours of [coord] that are on the board, in direction order. */
    fun neighborsInMap(coord: HexCoord): List<Hex> = coord.neighbors().mapNotNull { hexByCoord[it] }

    /** A land hex is coastal when at least one on-board neighbour is sea (lakes do not count). */
    fun isCoastal(coord: HexCoord): Boolean =
        hexByCoord[coord]?.isLand == true && neighborsInMap(coord).any { it.terrain == Terrain.SEA }

    fun hasRiver(
        a: HexCoord,
        b: HexCoord,
    ): Boolean = a.isAdjacentTo(b) && HexEdge.of(a, b) in rivers

    fun isStrait(coord: HexCoord): Boolean = hexByCoord[coord]?.strait != null

    /** The two land shores joined by the strait at [coord], or null if it is not a strait. */
    fun straitShores(coord: HexCoord): Pair<HexCoord, HexCoord>? {
        val strait = hexByCoord[coord]?.strait ?: return null
        return strait.shoreA to strait.shoreB
    }

    fun territory(id: TerritoryId): Territory? = territoryById[id]

    /** Territory the hex at [coord] belongs to, or null for water or off-board coordinates. */
    fun territoryAt(coord: HexCoord): Territory? = hexByCoord[coord]?.territory?.let { territoryById[it] }

    fun hexesOf(territory: TerritoryId): List<HexCoord> = coordsByTerritory[territory].orEmpty()

    /** Nations that own at least one territory, in declaration order. */
    fun nations(): List<NationId> = territories.map { it.nation }.distinct()

    fun territoriesOf(nation: NationId): List<Territory> = territories.filter { it.nation == nation }

    /**
     * Territories grouped by side. The map does not know sides: [sideOf] comes from the nation
     * catalog or from the game state.
     */
    fun territoriesBySide(sideOf: (NationId) -> Side): Map<Side, List<Territory>> =
        territories.groupBy { sideOf(it.nation) }

    override fun equals(other: Any?): Boolean =
        other is HexMap &&
            id == other.id &&
            name == other.name &&
            territories == other.territories &&
            hexByCoord == other.hexByCoord &&
            rivers == other.rivers

    override fun hashCode(): Int = listOf(id, name, territories, hexByCoord, rivers).hashCode()

    override fun toString(): String = "HexMap($id, ${hexByCoord.size} hexes, ${territories.size} territories)"

    companion object {
        /**
         * Validates and builds a map. Throws [InvalidMapException] listing every problem found.
         * [rivers] are pairs of adjacent land hexes.
         */
        fun build(
            id: String,
            name: String,
            territories: List<Territory>,
            hexes: List<Hex>,
            rivers: List<Pair<HexCoord, HexCoord>> = emptyList(),
        ): HexMap {
            val issues = MapValidator.validate(territories, hexes, rivers)
            if (issues.isNotEmpty()) throw InvalidMapException(issues)
            return HexMap(id, name, territories, hexes, rivers.map { (a, b) -> HexEdge.of(a, b) }.toSet())
        }
    }
}
