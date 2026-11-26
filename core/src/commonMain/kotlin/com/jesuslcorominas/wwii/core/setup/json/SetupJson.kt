package com.jesuslcorominas.wwii.core.setup.json

import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import com.jesuslcorominas.wwii.core.setup.CityBonus
import com.jesuslcorominas.wwii.core.setup.CounterCounts
import com.jesuslcorominas.wwii.core.setup.DeferredNation
import com.jesuslcorominas.wwii.core.setup.Deployment
import com.jesuslcorominas.wwii.core.setup.GameSetup
import com.jesuslcorominas.wwii.core.setup.InvalidSetupException
import com.jesuslcorominas.wwii.core.setup.Reinforcement
import com.jesuslcorominas.wwii.core.setup.SetupIssue
import com.jesuslcorominas.wwii.core.setup.SpecialCondition
import com.jesuslcorominas.wwii.core.setup.SpecialEffect
import com.jesuslcorominas.wwii.core.setup.SpecialId
import com.jesuslcorominas.wwii.core.setup.SpecialReinforcement
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Reads and writes game setups (initial deployment, reinforcement table and special reinforcements) in
 * the JSON format described in `docs/formato-despliegue.md`.
 *
 * The engine does not read files: callers pass the text, and the catalog the setup is validated against.
 */
object SetupJson {
    /** Version written by [encode] and the only one [decode] accepts. */
    const val FORMAT_VERSION = 1

    @OptIn(ExperimentalSerializationApi::class)
    private val compact =
        Json {
            encodeDefaults = false
            explicitNulls = false
        }

    @OptIn(ExperimentalSerializationApi::class)
    private val pretty =
        Json(compact) {
            prettyPrint = true
            prettyPrintIndent = "  "
        }

    /**
     * Parses a setup and validates it against [catalog]. Throws [InvalidSetupException] when the text is
     * not a setup of a supported version or the setup breaks any rule; the exception lists every problem.
     */
    fun decode(
        text: String,
        catalog: NationCatalog,
    ): GameSetup {
        val document = parse(text)
        return try {
            GameSetup.build(
                id = document.id,
                name = document.name,
                catalogId = document.catalog,
                turns = document.turns,
                deployment = document.deployment.map { it.toDomain() },
                reinforcements = document.reinforcements.map { it.toDomain() },
                specials = document.specials.map { it.toDomain() },
                catalog = catalog,
            )
        } catch (e: InvalidSetupException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw malformed(e.message ?: "invalid value")
        }
    }

    /** Writes [setup] in the current format. Decoding the result gives back an equal setup. */
    fun encode(
        setup: GameSetup,
        prettyPrint: Boolean = false,
    ): String {
        val document =
            SetupDocument(
                formatVersion = FORMAT_VERSION,
                id = setup.id,
                name = setup.name,
                catalog = setup.catalogId,
                turns = setup.turns,
                deployment = setup.deployment.map { it.toDto() },
                reinforcements = setup.reinforcements.map { it.toDto() },
                specials = setup.specials.map { it.toDto() },
            )
        return (if (prettyPrint) pretty else compact).encodeToString(SetupDocument.serializer(), document)
    }

    private fun parse(text: String): SetupDocument {
        try {
            val root = compact.parseToJsonElement(text)
            if (root !is JsonObject) throw malformed("root must be a JSON object")
            val version =
                (root["formatVersion"] as? JsonPrimitive)?.intOrNull
                    ?: throw malformed("missing integer formatVersion")
            if (version != FORMAT_VERSION) {
                throw InvalidSetupException(listOf(SetupIssue.UnsupportedFormatVersion(version, FORMAT_VERSION)))
            }
            return compact.decodeFromJsonElement(SetupDocument.serializer(), root)
        } catch (e: SerializationException) {
            throw malformed(e.message ?: "invalid JSON")
        }
    }

    private fun malformed(detail: String) = InvalidSetupException(listOf(SetupIssue.MalformedDocument(detail)))

    private fun DeploymentDto.toDomain() =
        Deployment(NationId(nation), TerritoryId(territory), CounterCounts(land, naval, air))

    private fun Deployment.toDto() = DeploymentDto(nation.value, territory.value, counts.land, counts.naval, counts.air)

    private fun ReinforcementDto.toDomain() = Reinforcement(turn, NationId(nation), CounterCounts(land, naval, air))

    private fun Reinforcement.toDto() = ReinforcementDto(turn, nation.value, counts.land, counts.naval, counts.air)

    private fun SpecialDto.toDomain(): SpecialReinforcement {
        val effect =
            when (this) {
                is DeferredEntryDto ->
                    SpecialEffect.DeferredEntry(
                        nations.map { DeferredNation(NationId(it.nation), it.turn) },
                    )
                is VolunteersDto ->
                    SpecialEffect.Volunteers(NationId(nation), side.toDomain(), turn, CounterCounts(land, naval, air))
                is KeyCityBonusDto ->
                    SpecialEffect.KeyCityBonus(
                        bonuses.map {
                            CityBonus(
                                SpecialCondition.ControlsCity(it.side.toDomain(), it.city),
                                NationId(it.nation),
                                CounterCounts(it.land, it.naval, it.air),
                            )
                        },
                    )
                is JoinSideDto -> SpecialEffect.JoinSide(NationId(nation), side.toDomain(), condition.toDomain())
            }
        return SpecialReinforcement(SpecialId(id), name, effect)
    }

    private fun SpecialReinforcement.toDto(): SpecialDto =
        when (val effect = effect) {
            is SpecialEffect.DeferredEntry ->
                DeferredEntryDto(id.value, name, effect.nations.map { DeferredNationDto(it.nation.value, it.turn) })
            is SpecialEffect.Volunteers ->
                VolunteersDto(
                    id = id.value,
                    name = name,
                    nation = effect.nation.value,
                    side = effect.side.toDto(),
                    turn = effect.turn,
                    land = effect.counts.land,
                    naval = effect.counts.naval,
                    air = effect.counts.air,
                )
            is SpecialEffect.KeyCityBonus ->
                KeyCityBonusDto(
                    id.value,
                    name,
                    effect.bonuses.map {
                        CityBonusDto(
                            city = it.condition.city,
                            side = it.condition.side.toDto(),
                            nation = it.nation.value,
                            land = it.counts.land,
                            naval = it.counts.naval,
                            air = it.counts.air,
                        )
                    },
                )
            is SpecialEffect.JoinSide ->
                JoinSideDto(id.value, name, effect.nation.value, effect.side.toDto(), effect.condition.toDto())
        }

    private fun ConditionDto.toDomain(): SpecialCondition =
        when (this) {
            is ControlsCityDto -> SpecialCondition.ControlsCity(side.toDomain(), city)
        }

    private fun SpecialCondition.toDto(): ConditionDto =
        when (this) {
            is SpecialCondition.ControlsCity -> ControlsCityDto(side.toDto(), city)
        }

    private fun SetupSideDto.toDomain() =
        when (this) {
            SetupSideDto.AXIS -> Side.AXIS
            SetupSideDto.WESTERN_ALLIES -> Side.WESTERN_ALLIES
            SetupSideDto.USSR -> Side.USSR
            SetupSideDto.NEUTRAL -> Side.NEUTRAL
        }

    private fun Side.toDto() =
        when (this) {
            Side.AXIS -> SetupSideDto.AXIS
            Side.WESTERN_ALLIES -> SetupSideDto.WESTERN_ALLIES
            Side.USSR -> SetupSideDto.USSR
            Side.NEUTRAL -> SetupSideDto.NEUTRAL
        }
}
