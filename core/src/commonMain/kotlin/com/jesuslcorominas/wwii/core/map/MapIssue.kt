package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.nation.NationId

/** A problem found while loading or validating a map. Each one carries a readable [message]. */
sealed class MapIssue {
    abstract val message: String

    override fun toString(): String = message

    data class UnsupportedFormatVersion(
        val found: Int,
        val supported: Int,
    ) : MapIssue() {
        override val message get() = "Unsupported map formatVersion $found (supported: $supported)"
    }

    data class MalformedDocument(
        val detail: String,
    ) : MapIssue() {
        override val message get() = "Malformed map document: $detail"
    }

    data class DuplicateHex(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "Hex $coord is declared more than once"
    }

    data class DuplicateTerritory(
        val territory: TerritoryId,
    ) : MapIssue() {
        override val message get() = "Territory $territory is declared more than once"
    }

    data class UnknownTerritory(
        val coord: HexCoord,
        val territory: TerritoryId,
    ) : MapIssue() {
        override val message get() = "Hex $coord references unknown territory $territory"
    }

    data class LandWithoutTerritory(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "Land hex $coord has no territory"
    }

    data class WaterWithTerritory(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "Water hex $coord must not have a territory"
    }

    enum class LandFeature { CITY, AIRPORT, MOUNTAIN }

    data class LandFeatureOnWater(
        val coord: HexCoord,
        val feature: LandFeature,
    ) : MapIssue() {
        override val message get() = "Water hex $coord cannot have ${feature.name.lowercase()}"
    }

    data class EmptyTerritory(
        val territory: TerritoryId,
    ) : MapIssue() {
        override val message get() = "Territory $territory has no hexes"
    }

    data class DisconnectedTerritory(
        val territory: TerritoryId,
        val parts: Int,
    ) : MapIssue() {
        override val message get() =
            "Territory $territory is split in $parts parts; declare it discontiguous if that is intended"
    }

    enum class RiverProblem { NOT_ADJACENT, OFF_MAP, NOT_LAND }

    data class InvalidRiver(
        val a: HexCoord,
        val b: HexCoord,
        val problem: RiverProblem,
    ) : MapIssue() {
        override val message get() = "River between $a and $b is invalid: ${problem.name.lowercase()}"
    }

    data class DuplicateRiver(
        val a: HexCoord,
        val b: HexCoord,
    ) : MapIssue() {
        override val message get() = "River between $a and $b is declared more than once"
    }

    data class StraitNotOnSea(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "Strait $coord must be a sea hex"
    }

    enum class ShoreProblem { SAME_SHORE, OFF_MAP, NOT_LAND, NOT_ADJACENT }

    data class InvalidStraitShore(
        val strait: HexCoord,
        val shore: HexCoord,
        val problem: ShoreProblem,
    ) : MapIssue() {
        override val message get() = "Strait $strait has an invalid shore $shore: ${problem.name.lowercase()}"
    }

    data class DuplicateNationalCapital(
        val nation: NationId,
        val coords: List<HexCoord>,
    ) : MapIssue() {
        override val message get() = "Nation $nation has more than one national capital: $coords"
    }

    data class NationalCapitalOutsideHomeland(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "National capital $coord is not in a homeland territory"
    }

    data class DuplicateTerritoryCapital(
        val territory: TerritoryId,
        val coords: List<HexCoord>,
    ) : MapIssue() {
        override val message get() = "Territory $territory has more than one territory capital: $coords"
    }

    data class TerritoryCapitalInHomeland(
        val coord: HexCoord,
    ) : MapIssue() {
        override val message get() = "Territory capital $coord is in a homeland; use a national capital"
    }
}

/** Thrown when a map does not pass validation. Lists every [issues] found, not just the first. */
class InvalidMapException(
    val issues: List<MapIssue>,
) : IllegalArgumentException(
        "Invalid map (${issues.size} issue(s)):\n" + issues.joinToString("\n") { "- ${it.message}" },
    )
