package com.jesuslcorominas.wwii.core.nation

/**
 * The side a nation fights for. It is not part of the map: it depends on the ruleset and changes
 * during a game (neutrals entering the war), so whoever knows it passes a `(NationId) -> Side`
 * lookup to the map when grouping by side.
 */
enum class Side {
    AXIS,
    WESTERN_ALLIES,
    USSR,
    NEUTRAL,
}
