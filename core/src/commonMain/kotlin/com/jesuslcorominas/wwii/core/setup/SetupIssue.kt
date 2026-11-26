package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/** A problem found while loading a setup or validating it against its catalog. */
sealed class SetupIssue {
    abstract val message: String

    override fun toString(): String = message

    data class UnsupportedFormatVersion(
        val found: Int,
        val supported: Int,
    ) : SetupIssue() {
        override val message get() = "Unsupported setup formatVersion $found (supported: $supported)"
    }

    data class MalformedDocument(
        val detail: String,
    ) : SetupIssue() {
        override val message get() = "Malformed setup document: $detail"
    }

    data class CatalogMismatch(
        val expected: String,
        val found: String,
    ) : SetupIssue() {
        override val message get() = "Setup is written for catalog $expected but was validated against $found"
    }

    data class NonPositiveTurns(
        val turns: Int,
    ) : SetupIssue() {
        override val message get() = "The campaign must have at least one turn, has $turns"
    }

    /** [where] says which entry: "reinforcement", or the id of a special. */
    data class TurnOutOfRange(
        val where: String,
        val turn: Int,
        val turns: Int,
    ) : SetupIssue() {
        override val message get() = "Turn $turn of $where is outside 1..$turns"
    }

    data class UnknownNation(
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Setup references unknown nation $nation"
    }

    /** Counts with a negative number, or with nothing at all. [where] says which entry. */
    data class InvalidCounts(
        val where: String,
        val nation: NationId,
        val counts: CounterCounts,
    ) : SetupIssue() {
        override val message get() = "Counts $counts of $nation in $where must be non-negative and not empty"
    }

    data class DuplicateDeployment(
        val nation: NationId,
        val territory: TerritoryId,
    ) : SetupIssue() {
        override val message get() = "Deployment of $nation in $territory is declared more than once"
    }

    data class DuplicateReinforcement(
        val turn: Int,
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Reinforcement of $nation in turn $turn is declared more than once"
    }

    /** Late-entry and off-map nations have nothing on the map when the game starts. */
    data class NotDeployable(
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Nation $nation is late-entry or off the map and cannot deploy at the start"
    }

    /** Neutral nations deploy everything at the start and get no base reinforcements. */
    data class NeutralReinforcement(
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Neutral nation $nation cannot receive base reinforcements"
    }

    /** Deployment, base reinforcements and every special bonus together need more counters than exist. */
    data class ExceedsCatalog(
        val nation: NationId,
        val type: CounterType,
        val required: Long,
        val available: Int,
    ) : SetupIssue() {
        override val message get() =
            "Nation $nation needs $required ${type.name.lowercase()} counter(s) in the worst case, the catalog has $available"
    }

    data class DuplicateSpecial(
        val id: SpecialId,
    ) : SetupIssue() {
        override val message get() = "Special reinforcement $id is declared more than once"
    }

    data class EmptySpecial(
        val id: SpecialId,
    ) : SetupIssue() {
        override val message get() = "Special reinforcement $id has nothing in it"
    }

    data class DeferredWithoutDeployment(
        val id: SpecialId,
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Special $id defers $nation, which deploys nothing at the start"
    }

    /** All the volunteer specials together withhold more from a nation than it deploys. */
    data class WithheldExceedsDeployment(
        val nation: NationId,
        val withheld: CounterCounts,
        val deployed: CounterCounts,
    ) : SetupIssue() {
        override val message get() = "Volunteer specials withhold $withheld from $nation, which only deploys $deployed"
    }

    /** A nation is deferred by more than one entry, in one special or across several. */
    data class DuplicateDeferral(
        val nation: NationId,
    ) : SetupIssue() {
        override val message get() = "Nation $nation is deferred more than once"
    }

    /** A special gives counters to, or moves a nation to, a side that makes no sense for it. */
    data class InvalidSide(
        val id: SpecialId,
        val nation: NationId,
        val side: Side,
    ) : SetupIssue() {
        override val message get() = "Special $id cannot put $nation on side ${side.name.lowercase()}"
    }
}

/** Thrown when a setup does not pass validation. Lists every [issues] found, not just the first. */
class InvalidSetupException(
    val issues: List<SetupIssue>,
) : IllegalArgumentException(
        "Invalid game setup (${issues.size} issue(s)):\n" + issues.joinToString("\n") { "- ${it.message}" },
    )
