package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType

/** How many counters of each type: what a nation deploys in a territory or receives in a turn. */
data class CounterCounts(
    val land: Int = 0,
    val naval: Int = 0,
    val air: Int = 0,
) {
    operator fun get(type: CounterType): Int =
        when (type) {
            CounterType.LAND -> land
            CounterType.NAVAL -> naval
            CounterType.AIR -> air
        }

    operator fun plus(other: CounterCounts): CounterCounts =
        CounterCounts(land + other.land, naval + other.naval, air + other.air)

    val total: Int get() = land + naval + air

    val isEmpty: Boolean get() = land == 0 && naval == 0 && air == 0

    val hasNegative: Boolean get() = land < 0 || naval < 0 || air < 0

    override fun toString(): String = "${land}L/${naval}N/${air}A"

    companion object {
        val NONE = CounterCounts()

        fun sum(counts: Iterable<CounterCounts>): CounterCounts = counts.fold(NONE, CounterCounts::plus)
    }
}
