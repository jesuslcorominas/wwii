package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side

/** Weight of a nation in the game. */
enum class NationRole {
    /** Main power of a side (it has a player behind it). */
    MAJOR,

    /** Smaller nation fighting for a side from the start. */
    MINOR,

    /** Nation outside the war until something brings it in. */
    NEUTRAL,
}

/**
 * A nation of the catalog.
 *
 * @property initialSide side at the start of a game. It can change during play (neutrals entering the
 *   war), which is the game state's business, not the catalog's.
 * @property lateEntry the nation is not in play when the game starts; when it enters is decided by
 *   the ruleset and the reinforcement table.
 * @property offMap the nation owns no territory on the map (it enters through its allies').
 */
data class Nation(
    val id: NationId,
    val name: String,
    val role: NationRole,
    val initialSide: Side,
    val lateEntry: Boolean = false,
    val offMap: Boolean = false,
)
