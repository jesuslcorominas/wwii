package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/** A problem found while loading or validating a nation catalog. Each one carries a readable [message]. */
sealed class CatalogIssue {
    abstract val message: String

    override fun toString(): String = message

    data class UnsupportedFormatVersion(
        val found: Int,
        val supported: Int,
    ) : CatalogIssue() {
        override val message get() = "Unsupported catalog formatVersion $found (supported: $supported)"
    }

    data class MalformedDocument(
        val detail: String,
    ) : CatalogIssue() {
        override val message get() = "Malformed catalog document: $detail"
    }

    data object NoNations : CatalogIssue() {
        override val message get() = "The catalog has no nations"
    }

    data class DuplicateNation(
        val nation: NationId,
    ) : CatalogIssue() {
        override val message get() = "Nation $nation is declared more than once"
    }

    data class UnknownNation(
        val nation: NationId,
    ) : CatalogIssue() {
        override val message get() = "Counters reference unknown nation $nation"
    }

    data class DuplicateCounterEntry(
        val nation: NationId,
        val type: CounterType,
    ) : CatalogIssue() {
        override val message get() = "Counters of type ${type.name.lowercase()} for $nation are declared more than once"
    }

    data class NonPositiveCount(
        val nation: NationId,
        val type: CounterType,
        val count: Int,
    ) : CatalogIssue() {
        override val message get() = "Counter count for $nation ${type.name.lowercase()} must be positive, was $count"
    }

    /** A neutral nation must start on [Side.NEUTRAL], and only neutral nations can. */
    data class RoleSideMismatch(
        val nation: NationId,
        val role: NationRole,
        val side: Side,
    ) : CatalogIssue() {
        override val message get() =
            "Nation $nation has role ${role.name.lowercase()} but starts on side ${side.name.lowercase()}"
    }

    data class LateEntryNeutral(
        val nation: NationId,
    ) : CatalogIssue() {
        override val message get() = "Neutral nation $nation cannot have a late entry"
    }

    data class MajorWithoutCounters(
        val nation: NationId,
    ) : CatalogIssue() {
        override val message get() = "Major nation $nation has no counters"
    }
}

/** Thrown when a catalog does not pass validation. Lists every [issues] found, not just the first. */
class InvalidCatalogException(
    val issues: List<CatalogIssue>,
) : IllegalArgumentException(
        "Invalid nation catalog (${issues.size} issue(s)):\n" + issues.joinToString("\n") { "- ${it.message}" },
    )
