package com.jesuslcorominas.wwii.core.map.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Serialized form of the map (see docs/formato-mapa.md). Kept apart from the domain model so the
// file format can evolve on its own. Coordinates are axial [q, r] pairs.

@Serializable
internal data class MapDocument(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val territories: List<TerritoryDto> = emptyList(),
    val hexes: List<HexDto> = emptyList(),
    val rivers: List<RiverDto> = emptyList(),
)

@Serializable
internal data class TerritoryDto(
    val id: String,
    val nation: String,
    val kind: TerritoryKindDto,
    val name: String,
    val discontiguous: Boolean = false,
)

@Serializable
internal data class HexDto(
    val q: Int,
    val r: Int,
    val terrain: TerrainDto,
    val territory: String? = null,
    val mountain: Boolean = false,
    val city: CityDto? = null,
    val airport: Boolean = false,
    val strait: List<List<Int>>? = null,
)

@Serializable
internal data class CityDto(
    val id: String,
    val name: String,
    val kind: CityKindDto = CityKindDto.CITY,
)

@Serializable
internal data class RiverDto(
    val a: List<Int>,
    val b: List<Int>,
)

@Serializable
internal enum class TerrainDto {
    @SerialName("land")
    LAND,

    @SerialName("sea")
    SEA,

    @SerialName("lake")
    LAKE,
}

@Serializable
internal enum class TerritoryKindDto {
    @SerialName("homeland")
    HOMELAND,

    @SerialName("colony")
    COLONY,

    @SerialName("possession")
    POSSESSION,
}

@Serializable
internal enum class CityKindDto {
    @SerialName("city")
    CITY,

    @SerialName("territory_capital")
    TERRITORY_CAPITAL,

    @SerialName("national_capital")
    NATIONAL_CAPITAL,
}
