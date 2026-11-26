package com.jesuslcorominas.wwii.core.setup.json

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Serialized form of a game setup (see docs/formato-despliegue.md). Kept apart from the domain model so
// the file format can evolve on its own. Polymorphic entries use the "type" key.

@Serializable
internal data class SetupDocument(
    val formatVersion: Int,
    val id: String,
    val name: String,
    val catalog: String,
    val turns: Int,
    val deployment: List<DeploymentDto> = emptyList(),
    val reinforcements: List<ReinforcementDto> = emptyList(),
    val specials: List<SpecialDto> = emptyList(),
)

@Serializable
internal data class DeploymentDto(
    val nation: String,
    val territory: String,
    val land: Int = 0,
    val naval: Int = 0,
    val air: Int = 0,
)

@Serializable
internal data class ReinforcementDto(
    val turn: Int,
    val nation: String,
    val land: Int = 0,
    val naval: Int = 0,
    val air: Int = 0,
)

@Serializable
internal sealed class SpecialDto {
    abstract val id: String
    abstract val name: String
}

@Serializable
@SerialName("deferred_entry")
internal data class DeferredEntryDto(
    override val id: String,
    override val name: String,
    val nations: List<DeferredNationDto>,
) : SpecialDto()

@Serializable
internal data class DeferredNationDto(
    val nation: String,
    val turn: Int,
)

@Serializable
@SerialName("volunteers")
internal data class VolunteersDto(
    override val id: String,
    override val name: String,
    val nation: String,
    val side: SetupSideDto,
    val turn: Int,
    val land: Int = 0,
    val naval: Int = 0,
    val air: Int = 0,
) : SpecialDto()

@Serializable
@SerialName("key_city_bonus")
internal data class KeyCityBonusDto(
    override val id: String,
    override val name: String,
    val bonuses: List<CityBonusDto>,
) : SpecialDto()

@Serializable
internal data class CityBonusDto(
    val cityId: String,
    val side: SetupSideDto,
    val nation: String,
    val land: Int = 0,
    val naval: Int = 0,
    val air: Int = 0,
)

@Serializable
@SerialName("join_side")
internal data class JoinSideDto(
    override val id: String,
    override val name: String,
    val nation: String,
    val side: SetupSideDto,
    val condition: ConditionDto,
) : SpecialDto()

@Serializable
internal sealed class ConditionDto

@Serializable
@SerialName("controls_city")
internal data class ControlsCityDto(
    val side: SetupSideDto,
    val cityId: String,
) : ConditionDto()

@Serializable
internal enum class SetupSideDto {
    @SerialName("axis")
    AXIS,

    @SerialName("western_allies")
    WESTERN_ALLIES,

    @SerialName("ussr")
    USSR,

    @SerialName("neutral")
    NEUTRAL,
}
