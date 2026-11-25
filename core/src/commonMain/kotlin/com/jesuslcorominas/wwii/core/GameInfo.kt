package com.jesuslcorominas.wwii.core

/**
 * Placeholder for the game engine: basic campaign facts shown by the skeleton screen.
 * The working title is provisional.
 */
object GameInfo {
    const val TITLE = "WWII"
    const val START_YEAR = 1939
    const val CAMPAIGN_TURNS = 18

    fun summary(): String = "$TITLE · $START_YEAR · $CAMPAIGN_TURNS turns"
}
