package com.jesuslcorominas.wwii.core.catalog.json

import com.jesuslcorominas.wwii.core.catalog.CatalogIssue
import com.jesuslcorominas.wwii.core.catalog.CounterEntry
import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.catalog.InvalidCatalogException
import com.jesuslcorominas.wwii.core.catalog.Nation
import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.NationRole
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Reads and writes nation catalogs in the JSON format described in `docs/formato-catalogo.md`.
 *
 * The engine does not read files: callers pass the text.
 */
object CatalogJson {
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
     * Parses and validates a catalog. Throws [InvalidCatalogException] when the text is not a catalog
     * of a supported version or the catalog breaks any rule; the exception lists every problem found.
     */
    fun decode(text: String): NationCatalog {
        val document = parse(text)
        return try {
            NationCatalog.build(
                id = document.id,
                name = document.name,
                nations = document.nations.map { it.toDomain() },
                counters = document.counters.map { it.toDomain() },
            )
        } catch (e: InvalidCatalogException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw malformed(e.message ?: "invalid value")
        }
    }

    /** Writes [catalog] in the current format. Decoding the result gives back an equal catalog. */
    fun encode(
        catalog: NationCatalog,
        prettyPrint: Boolean = false,
    ): String {
        val document =
            CatalogDocument(
                formatVersion = FORMAT_VERSION,
                id = catalog.id,
                name = catalog.name,
                nations = catalog.nations.map { it.toDto() },
                counters = catalog.entries.map { it.toDto() },
            )
        return (if (prettyPrint) pretty else compact).encodeToString(CatalogDocument.serializer(), document)
    }

    private fun parse(text: String): CatalogDocument {
        try {
            val root = compact.parseToJsonElement(text)
            if (root !is JsonObject) throw malformed("root must be a JSON object")
            val version =
                (root["formatVersion"] as? JsonPrimitive)?.intOrNull
                    ?: throw malformed("missing integer formatVersion")
            if (version != FORMAT_VERSION) {
                throw InvalidCatalogException(
                    listOf(CatalogIssue.UnsupportedFormatVersion(version, FORMAT_VERSION)),
                )
            }
            return compact.decodeFromJsonElement(CatalogDocument.serializer(), root)
        } catch (e: SerializationException) {
            throw malformed(e.message ?: "invalid JSON")
        }
    }

    private fun malformed(detail: String) = InvalidCatalogException(listOf(CatalogIssue.MalformedDocument(detail)))

    private fun NationDto.toDomain() =
        Nation(
            id = NationId(id),
            name = name,
            role =
                when (role) {
                    NationRoleDto.MAJOR -> NationRole.MAJOR
                    NationRoleDto.MINOR -> NationRole.MINOR
                    NationRoleDto.NEUTRAL -> NationRole.NEUTRAL
                },
            initialSide =
                when (side) {
                    SideDto.AXIS -> Side.AXIS
                    SideDto.WESTERN_ALLIES -> Side.WESTERN_ALLIES
                    SideDto.USSR -> Side.USSR
                    SideDto.NEUTRAL -> Side.NEUTRAL
                },
            lateEntry = lateEntry,
            offMap = offMap,
        )

    private fun Nation.toDto() =
        NationDto(
            id = id.value,
            name = name,
            role =
                when (role) {
                    NationRole.MAJOR -> NationRoleDto.MAJOR
                    NationRole.MINOR -> NationRoleDto.MINOR
                    NationRole.NEUTRAL -> NationRoleDto.NEUTRAL
                },
            side =
                when (initialSide) {
                    Side.AXIS -> SideDto.AXIS
                    Side.WESTERN_ALLIES -> SideDto.WESTERN_ALLIES
                    Side.USSR -> SideDto.USSR
                    Side.NEUTRAL -> SideDto.NEUTRAL
                },
            lateEntry = lateEntry,
            offMap = offMap,
        )

    private fun CounterEntryDto.toDomain() = CounterEntry(NationId(nation), type.toDomain(), count)

    private fun CounterEntry.toDto() = CounterEntryDto(nation.value, type.toDto(), count)

    private fun CounterTypeDto.toDomain() =
        when (this) {
            CounterTypeDto.LAND -> CounterType.LAND
            CounterTypeDto.NAVAL -> CounterType.NAVAL
            CounterTypeDto.AIR -> CounterType.AIR
        }

    private fun CounterType.toDto() =
        when (this) {
            CounterType.LAND -> CounterTypeDto.LAND
            CounterType.NAVAL -> CounterTypeDto.NAVAL
            CounterType.AIR -> CounterTypeDto.AIR
        }
}
