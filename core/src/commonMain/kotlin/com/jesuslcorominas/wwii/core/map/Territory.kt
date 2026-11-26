package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.jvm.JvmInline

@JvmInline
value class TerritoryId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "TerritoryId must not be blank" }
    }

    override fun toString(): String = value
}

enum class TerritoryKind {
    /** The nation's own country (metropolis). */
    HOMELAND,
    COLONY,
    POSSESSION,
}

/**
 * A unit of land that is conquered as a whole and belongs to a [nation]. A nation can own several
 * territories (homeland, colonies, possessions). A territory whose hexes are not connected (islands,
 * exclaves) must say so with [discontiguous].
 */
data class Territory(
    val id: TerritoryId,
    val nation: NationId,
    val kind: TerritoryKind,
    val name: String,
    val discontiguous: Boolean = false,
)
