package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.jvm.JvmInline

/** Stable identifier of a special reinforcement; the ruleset turns specials on by id. */
@JvmInline
value class SpecialId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "SpecialId must not be blank" }
    }

    override fun toString(): String = value
}

/**
 * An optional rule that changes the base deployment and reinforcements. Specials are data: which ones
 * are active is decided by the ruleset (the advanced ruleset offers them as options), and evaluating
 * their conditions during play belongs to the game engine, not to this model.
 */
data class SpecialReinforcement(
    val id: SpecialId,
    val name: String,
    val effect: SpecialEffect,
)

/** A declarative condition checked during the reinforcement phase. */
sealed interface SpecialCondition {
    /** [side] controls the city with the stable id [cityId] (a slug such as `suez`, the city's id on the map). */
    data class ControlsCity(
        val side: Side,
        val cityId: String,
    ) : SpecialCondition {
        init {
            require(cityId.isNotBlank()) { "City id must not be blank" }
        }
    }
}

/** What a special reinforcement does. */
sealed interface SpecialEffect {
    /**
     * The listed nations do not deploy at the start: their whole initial deployment arrives as a
     * reinforcement in the given turn instead. Moves counters in time; adds none.
     */
    data class DeferredEntry(
        val nations: List<DeferredNation>,
    ) : SpecialEffect

    /**
     * [counts] are withheld from [nation]'s initial deployment and arrive in [turn] fighting for
     * [side], while the nation itself keeps its side. Moves counters; adds none.
     */
    data class Volunteers(
        val nation: NationId,
        val side: Side,
        val turn: Int,
        val counts: CounterCounts,
    ) : SpecialEffect

    /** Extra counters, each bonus given once, the first time its condition holds. */
    data class KeyCityBonus(
        val bonuses: List<CityBonus>,
    ) : SpecialEffect

    /** [nation] changes to [side], with the counters it has, when [condition] holds. Adds none. */
    data class JoinSide(
        val nation: NationId,
        val side: Side,
        val condition: SpecialCondition,
    ) : SpecialEffect
}

/** A nation whose initial deployment is deferred to [turn]. */
data class DeferredNation(
    val nation: NationId,
    val turn: Int,
)

/** [nation] receives [counts] once, the first time [condition] holds. */
data class CityBonus(
    val condition: SpecialCondition.ControlsCity,
    val nation: NationId,
    val counts: CounterCounts,
)
