package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import kotlin.jvm.JvmInline

/**
 * Kind of counter. Every counter of a type is identical: combat and movement factors come from the
 * ruleset, not from the counter.
 */
enum class CounterType(
    /** One-letter code used in [CounterId]s. */
    val code: Char,
) {
    LAND('L'),
    NAVAL('N'),
    AIR('A'),
}

/** Identifier of a single counter, for example `DEU-L01`. */
@JvmInline
value class CounterId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "CounterId must not be blank" }
    }

    override fun toString(): String = value

    companion object {
        /** The id of the [number]-th (1-based) counter of [type] of [nation]: `DEU-L01`. */
        fun of(
            nation: NationId,
            type: CounterType,
            number: Int,
        ): CounterId = CounterId("${nation.value}-${type.code}${number.toString().padStart(2, '0')}")
    }
}

/** One physical counter of a nation. */
data class Counter(
    val id: CounterId,
    val nation: NationId,
    val type: CounterType,
)

/** How many counters of [type] a [nation] has. */
data class CounterEntry(
    val nation: NationId,
    val type: CounterType,
    val count: Int,
)
