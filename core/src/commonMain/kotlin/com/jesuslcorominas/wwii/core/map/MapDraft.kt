package com.jesuslcorominas.wwii.core.map

/**
 * The contents of a map before validation: what a map file holds and what the map editor works on.
 * Unlike [HexMap], a draft can break the map rules (a half-digitized map usually does); [issues]
 * lists them and [toMap] gives the validated map.
 *
 * Editing and loading format only: the engine and the game consume [HexMap], never a draft. Nothing
 * in `core` outside the `map` package depends on this type.
 */
data class MapDraft(
    val id: String,
    val name: String,
    val territories: List<Territory> = emptyList(),
    val hexes: List<Hex> = emptyList(),
    /** Pairs of hexes whose shared side is crossed by a river, in declaration order. */
    val rivers: List<Pair<HexCoord, HexCoord>> = emptyList(),
) {
    /** Every rule this draft breaks; empty when [toMap] would succeed. */
    fun issues(): List<MapIssue> = MapValidator.validate(territories, hexes, rivers)

    /** Validates and builds the map. Throws [InvalidMapException] listing every problem found. */
    fun toMap(): HexMap = HexMap.build(id, name, territories, hexes, rivers)

    companion object {
        /** The draft of a valid [map], keeping its declaration order. */
        fun of(map: HexMap): MapDraft =
            MapDraft(
                id = map.id,
                name = map.name,
                territories = map.territories,
                hexes = map.hexes.toList(),
                rivers = map.rivers.map { it.first to it.second },
            )
    }
}
