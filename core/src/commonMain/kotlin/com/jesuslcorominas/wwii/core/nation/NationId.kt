package com.jesuslcorominas.wwii.core.nation

import kotlin.jvm.JvmInline

/**
 * Identifier of a nation (for example `"FR"`). Shared by the map, which groups territories by
 * nation, and by the nation catalog, which describes each nation.
 */
@JvmInline
value class NationId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "NationId must not be blank" }
    }

    override fun toString(): String = value
}
