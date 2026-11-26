package com.jesuslcorominas.wwii.core.map.json

import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.CityKind
import com.jesuslcorominas.wwii.core.map.Hex
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.HexMap
import com.jesuslcorominas.wwii.core.map.InvalidMapException
import com.jesuslcorominas.wwii.core.map.MapIssue
import com.jesuslcorominas.wwii.core.map.Strait
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TerritoryKind
import com.jesuslcorominas.wwii.core.nation.NationId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Reads and writes maps in the JSON format described in `docs/formato-mapa.md`.
 *
 * The engine does not read files: callers (the UI with its resources, the map editor) pass the text.
 */
object MapJson {
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
     * Parses and validates a map. Throws [InvalidMapException] when the text is not a map of a
     * supported version or the map breaks any rule; the exception lists every problem found.
     */
    fun decode(text: String): HexMap {
        val document = parse(text)
        return try {
            HexMap.build(
                id = document.id,
                name = document.name,
                territories = document.territories.map { it.toDomain() },
                hexes = document.hexes.map { it.toDomain() },
                rivers = document.rivers.map { it.a.toCoord("river.a") to it.b.toCoord("river.b") },
            )
        } catch (e: InvalidMapException) {
            throw e
        } catch (e: IllegalArgumentException) {
            throw malformed(e.message ?: "invalid value")
        }
    }

    /** Writes [map] in the current format. Decoding the result gives back an equal map. */
    fun encode(
        map: HexMap,
        prettyPrint: Boolean = false,
    ): String {
        val document =
            MapDocument(
                formatVersion = FORMAT_VERSION,
                id = map.id,
                name = map.name,
                territories = map.territories.map { it.toDto() },
                hexes = map.hexes.map { it.toDto() },
                rivers = map.rivers.map { RiverDto(it.first.toList(), it.second.toList()) },
            )
        return (if (prettyPrint) pretty else compact).encodeToString(MapDocument.serializer(), document)
    }

    private fun parse(text: String): MapDocument {
        try {
            val root = compact.parseToJsonElement(text)
            if (root !is JsonObject) throw malformed("root must be a JSON object")
            val version =
                (root["formatVersion"] as? JsonPrimitive)?.intOrNull
                    ?: throw malformed("missing integer formatVersion")
            if (version != FORMAT_VERSION) {
                throw InvalidMapException(listOf(MapIssue.UnsupportedFormatVersion(version, FORMAT_VERSION)))
            }
            return compact.decodeFromJsonElement(MapDocument.serializer(), root)
        } catch (e: SerializationException) {
            throw malformed(e.message ?: "invalid JSON")
        }
    }

    private fun malformed(detail: String) = InvalidMapException(listOf(MapIssue.MalformedDocument(detail)))

    private fun List<Int>.toCoord(field: String): HexCoord {
        require(size == 2) { "$field must be a [q, r] pair, was $this" }
        return HexCoord(this[0], this[1])
    }

    private fun HexCoord.toList(): List<Int> = listOf(q, r)

    private fun TerritoryDto.toDomain() =
        Territory(
            id = TerritoryId(id),
            nation = NationId(nation),
            kind =
                when (kind) {
                    TerritoryKindDto.HOMELAND -> TerritoryKind.HOMELAND
                    TerritoryKindDto.COLONY -> TerritoryKind.COLONY
                    TerritoryKindDto.POSSESSION -> TerritoryKind.POSSESSION
                },
            name = name,
            discontiguous = discontiguous,
        )

    private fun Territory.toDto() =
        TerritoryDto(
            id = id.value,
            nation = nation.value,
            kind =
                when (kind) {
                    TerritoryKind.HOMELAND -> TerritoryKindDto.HOMELAND
                    TerritoryKind.COLONY -> TerritoryKindDto.COLONY
                    TerritoryKind.POSSESSION -> TerritoryKindDto.POSSESSION
                },
            name = name,
            discontiguous = discontiguous,
        )

    private fun HexDto.toDomain(): Hex {
        val coord = HexCoord(q, r)
        return Hex(
            coord = coord,
            terrain =
                when (terrain) {
                    TerrainDto.LAND -> Terrain.LAND
                    TerrainDto.SEA -> Terrain.SEA
                    TerrainDto.LAKE -> Terrain.LAKE
                },
            territory = territory?.let { TerritoryId(it) },
            mountain = mountain,
            city = city?.toDomain(),
            airport = airport,
            strait =
                strait?.let { shores ->
                    require(shores.size == 2) { "strait at $coord must list exactly two shores" }
                    Strait(shores[0].toCoord("strait shore"), shores[1].toCoord("strait shore"))
                },
        )
    }

    private fun Hex.toDto() =
        HexDto(
            q = coord.q,
            r = coord.r,
            terrain =
                when (terrain) {
                    Terrain.LAND -> TerrainDto.LAND
                    Terrain.SEA -> TerrainDto.SEA
                    Terrain.LAKE -> TerrainDto.LAKE
                },
            territory = territory?.value,
            mountain = mountain,
            city = city?.toDto(),
            airport = airport,
            strait = strait?.let { listOf(it.shoreA.toList(), it.shoreB.toList()) },
        )

    private fun CityDto.toDomain() =
        City(
            name = name,
            kind =
                when (kind) {
                    CityKindDto.CITY -> CityKind.CITY
                    CityKindDto.TERRITORY_CAPITAL -> CityKind.TERRITORY_CAPITAL
                    CityKindDto.NATIONAL_CAPITAL -> CityKind.NATIONAL_CAPITAL
                },
        )

    private fun City.toDto() =
        CityDto(
            name = name,
            kind =
                when (kind) {
                    CityKind.CITY -> CityKindDto.CITY
                    CityKind.TERRITORY_CAPITAL -> CityKindDto.TERRITORY_CAPITAL
                    CityKind.NATIONAL_CAPITAL -> CityKindDto.NATIONAL_CAPITAL
                },
        )
}
