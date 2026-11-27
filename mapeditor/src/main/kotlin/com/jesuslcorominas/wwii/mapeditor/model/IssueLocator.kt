package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.MapIssue
import com.jesuslcorominas.wwii.core.map.TerritoryId

/** The hexes a validation problem is about, to highlight them and centre the view on them. */
object IssueLocator {
    fun hexesOf(
        issue: MapIssue,
        draft: MapDraft,
    ): List<HexCoord> =
        when (issue) {
            is MapIssue.UnsupportedFormatVersion, is MapIssue.MalformedDocument -> emptyList()
            is MapIssue.DuplicateHex -> listOf(issue.coord)
            is MapIssue.UnknownTerritory -> listOf(issue.coord)
            is MapIssue.LandWithoutTerritory -> listOf(issue.coord)
            is MapIssue.WaterWithTerritory -> listOf(issue.coord)
            is MapIssue.LandFeatureOnWater -> listOf(issue.coord)
            is MapIssue.DuplicateTerritory -> territoryHexes(issue.territory, draft)
            is MapIssue.EmptyTerritory -> emptyList()
            is MapIssue.DisconnectedTerritory -> territoryHexes(issue.territory, draft)
            is MapIssue.InvalidRiver -> listOf(issue.a, issue.b)
            is MapIssue.DuplicateRiver -> listOf(issue.a, issue.b)
            is MapIssue.StraitNotOnSea -> listOf(issue.coord)
            is MapIssue.InvalidStraitShore -> listOf(issue.strait, issue.shore)
            is MapIssue.DuplicateNationalCapital -> issue.coords
            is MapIssue.NationalCapitalOutsideHomeland -> listOf(issue.coord)
            is MapIssue.DuplicateTerritoryCapital -> issue.coords
            is MapIssue.TerritoryCapitalInHomeland -> listOf(issue.coord)
            is MapIssue.InvalidCityId -> listOf(issue.coord)
            is MapIssue.DuplicateCityId -> issue.coords
        }

    private fun territoryHexes(
        id: TerritoryId,
        draft: MapDraft,
    ): List<HexCoord> = draft.hexes.filter { it.territory == id }.map { it.coord }
}
