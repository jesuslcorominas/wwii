package com.jesuslcorominas.wwii.core.map

import kotlin.jvm.JvmInline

enum class Terrain {
    LAND,
    SEA,

    /** Impassable and not navigable. */
    LAKE,
}

enum class CityKind {
    CITY,
    TERRITORY_CAPITAL,
    NATIONAL_CAPITAL,
}

/**
 * Stable identifier of a city, unique in its map: a slug of lowercase ASCII letters and digits with
 * single hyphens (`suez`, `brest-litovsk`). Other data (setup, rule sets) refers to cities by it, so it
 * does not change when the display [City.name] does.
 */
@JvmInline
value class CityId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "CityId must not be blank" }
    }

    /** Whether the id follows the slug format; [HexMap] only accepts well-formed ids. */
    val isWellFormed: Boolean get() = SLUG.matches(value)

    override fun toString(): String = value

    private companion object {
        val SLUG = Regex("[a-z0-9]+(-[a-z0-9]+)*")
    }
}

data class City(
    val id: CityId,
    val name: String,
    val kind: CityKind = CityKind.CITY,
)

/**
 * A strait: a sea hex that connects two land shores. Ships cross it like any other sea hex and can
 * stand on it; whether land units can cross from one shore to the other depends on the ruleset.
 */
data class Strait(
    val shoreA: HexCoord,
    val shoreB: HexCoord,
) {
    val shores: Set<HexCoord> get() = setOf(shoreA, shoreB)
}

/**
 * One hex of the board. Only land hexes have a [territory], a [city], an [airport] or a
 * [mountain]; only sea hexes can be a [strait]. [HexMap] guarantees it.
 */
data class Hex(
    val coord: HexCoord,
    val terrain: Terrain,
    val territory: TerritoryId? = null,
    val mountain: Boolean = false,
    val city: City? = null,
    val airport: Boolean = false,
    val strait: Strait? = null,
) {
    val isLand: Boolean get() = terrain == Terrain.LAND

    /** Every city or capital has an airport; [airport] marks airports without a city. */
    val hasAirport: Boolean get() = airport || city != null
}
