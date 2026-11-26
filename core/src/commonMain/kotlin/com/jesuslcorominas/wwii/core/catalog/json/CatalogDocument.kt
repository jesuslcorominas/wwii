package com.jesuslcorominas.wwii.core.catalog.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Serialized form of the nation catalog (see docs/formato-catalogo.md). Kept apart from the domain
// model so the file format can evolve on its own.

@Serializable
internal data class CatalogDocument(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val nations: List<NationDto> = emptyList(),
    val counters: List<CounterEntryDto> = emptyList(),
)

@Serializable
internal data class NationDto(
    val id: String,
    val name: String,
    val role: NationRoleDto,
    val side: SideDto,
    val lateEntry: Boolean = false,
    val offMap: Boolean = false,
)

@Serializable
internal data class CounterEntryDto(
    val nation: String,
    val type: CounterTypeDto,
    val count: Int,
)

@Serializable
internal enum class NationRoleDto {
    @SerialName("major")
    MAJOR,

    @SerialName("minor")
    MINOR,

    @SerialName("neutral")
    NEUTRAL,
}

@Serializable
internal enum class SideDto {
    @SerialName("axis")
    AXIS,

    @SerialName("western_allies")
    WESTERN_ALLIES,

    @SerialName("ussr")
    USSR,

    @SerialName("neutral")
    NEUTRAL,
}

@Serializable
internal enum class CounterTypeDto {
    @SerialName("land")
    LAND,

    @SerialName("naval")
    NAVAL,

    @SerialName("air")
    AIR,
}
