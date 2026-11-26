package com.jesuslcorominas.wwii.core.map

import com.jesuslcorominas.wwii.core.map.MapIssue.LandFeature
import com.jesuslcorominas.wwii.core.map.MapIssue.RiverProblem
import com.jesuslcorominas.wwii.core.map.MapIssue.ShoreProblem

/** Checks every map rule and returns all the problems found (empty list = valid map). */
internal object MapValidator {
    fun validate(
        territories: List<Territory>,
        hexes: List<Hex>,
        rivers: List<Pair<HexCoord, HexCoord>>,
    ): List<MapIssue> {
        val issues = mutableListOf<MapIssue>()
        val territoryById = checkTerritoryIds(territories, issues)
        val hexByCoord = checkHexCoords(hexes, issues)
        hexByCoord.values.forEach { checkHex(it, territoryById, hexByCoord, issues) }
        checkTerritoryShapes(territories, territoryById, hexByCoord, issues)
        checkRivers(rivers, hexByCoord, issues)
        checkCapitals(territoryById, hexByCoord, issues)
        return issues
    }

    private fun checkTerritoryIds(
        territories: List<Territory>,
        issues: MutableList<MapIssue>,
    ): Map<TerritoryId, Territory> {
        val byId = LinkedHashMap<TerritoryId, Territory>()
        territories.forEach { t ->
            if (byId.containsKey(t.id)) issues += MapIssue.DuplicateTerritory(t.id) else byId[t.id] = t
        }
        return byId
    }

    private fun checkHexCoords(
        hexes: List<Hex>,
        issues: MutableList<MapIssue>,
    ): Map<HexCoord, Hex> {
        val byCoord = LinkedHashMap<HexCoord, Hex>()
        val reported = mutableSetOf<HexCoord>()
        hexes.forEach { h ->
            if (byCoord.containsKey(h.coord)) {
                if (reported.add(h.coord)) issues += MapIssue.DuplicateHex(h.coord)
            } else {
                byCoord[h.coord] = h
            }
        }
        return byCoord
    }

    private fun checkHex(
        hex: Hex,
        territoryById: Map<TerritoryId, Territory>,
        hexByCoord: Map<HexCoord, Hex>,
        issues: MutableList<MapIssue>,
    ) {
        val c = hex.coord
        if (hex.isLand) {
            when {
                hex.territory == null -> issues += MapIssue.LandWithoutTerritory(c)
                hex.territory !in territoryById -> issues += MapIssue.UnknownTerritory(c, hex.territory)
            }
        } else {
            if (hex.territory != null) issues += MapIssue.WaterWithTerritory(c)
            if (hex.city != null) issues += MapIssue.LandFeatureOnWater(c, LandFeature.CITY)
            if (hex.airport) issues += MapIssue.LandFeatureOnWater(c, LandFeature.AIRPORT)
            if (hex.mountain) issues += MapIssue.LandFeatureOnWater(c, LandFeature.MOUNTAIN)
        }
        hex.strait?.let { checkStrait(hex, it, hexByCoord, issues) }
    }

    private fun checkStrait(
        hex: Hex,
        strait: Strait,
        hexByCoord: Map<HexCoord, Hex>,
        issues: MutableList<MapIssue>,
    ) {
        val c = hex.coord
        if (hex.terrain != Terrain.SEA) issues += MapIssue.StraitNotOnSea(c)
        if (strait.shoreA == strait.shoreB) {
            issues += MapIssue.InvalidStraitShore(c, strait.shoreA, ShoreProblem.SAME_SHORE)
        }
        strait.shores.forEach { shore ->
            val shoreHex = hexByCoord[shore]
            val problem =
                when {
                    !c.isAdjacentTo(shore) -> ShoreProblem.NOT_ADJACENT
                    shoreHex == null -> ShoreProblem.OFF_MAP
                    !shoreHex.isLand -> ShoreProblem.NOT_LAND
                    else -> null
                }
            if (problem != null) issues += MapIssue.InvalidStraitShore(c, shore, problem)
        }
    }

    private fun checkTerritoryShapes(
        territories: List<Territory>,
        territoryById: Map<TerritoryId, Territory>,
        hexByCoord: Map<HexCoord, Hex>,
        issues: MutableList<MapIssue>,
    ) {
        val hexesByTerritory =
            hexByCoord.values
                .filter { it.isLand && it.territory != null }
                .groupBy({ it.territory!! }, { it.coord })
        val straitLinks = straitLinks(hexByCoord)
        territories.distinctBy { it.id }.forEach { t ->
            val coords = hexesByTerritory[t.id].orEmpty()
            if (coords.isEmpty()) {
                issues += MapIssue.EmptyTerritory(t.id)
            } else if (!territoryById.getValue(t.id).discontiguous) {
                val parts = countParts(coords.toSet(), straitLinks)
                if (parts > 1) issues += MapIssue.DisconnectedTerritory(t.id, parts)
            }
        }
    }

    /** For each land hex, the opposite shores it reaches through straits. */
    private fun straitLinks(hexByCoord: Map<HexCoord, Hex>): Map<HexCoord, Set<HexCoord>> {
        val links = mutableMapOf<HexCoord, MutableSet<HexCoord>>()
        hexByCoord.values.mapNotNull { it.strait }.forEach { s ->
            links.getOrPut(s.shoreA) { mutableSetOf() } += s.shoreB
            links.getOrPut(s.shoreB) { mutableSetOf() } += s.shoreA
        }
        return links
    }

    private fun countParts(
        coords: Set<HexCoord>,
        straitLinks: Map<HexCoord, Set<HexCoord>>,
    ): Int {
        val visited = mutableSetOf<HexCoord>()
        var parts = 0
        coords.sorted().forEach { start ->
            if (visited.add(start)) {
                parts++
                val queue = ArrayDeque(listOf(start))
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    val next = current.neighbors() + straitLinks[current].orEmpty()
                    next.filter { it in coords && visited.add(it) }.forEach { queue.addLast(it) }
                }
            }
        }
        return parts
    }

    private fun checkRivers(
        rivers: List<Pair<HexCoord, HexCoord>>,
        hexByCoord: Map<HexCoord, Hex>,
        issues: MutableList<MapIssue>,
    ) {
        val seen = mutableSetOf<HexEdge>()
        rivers.forEach { (a, b) ->
            val problem =
                when {
                    !a.isAdjacentTo(b) -> RiverProblem.NOT_ADJACENT
                    a !in hexByCoord || b !in hexByCoord -> RiverProblem.OFF_MAP
                    !hexByCoord.getValue(a).isLand || !hexByCoord.getValue(b).isLand -> RiverProblem.NOT_LAND
                    else -> null
                }
            when {
                problem != null -> issues += MapIssue.InvalidRiver(a, b, problem)
                !seen.add(HexEdge.of(a, b)) -> issues += MapIssue.DuplicateRiver(a, b)
            }
        }
    }

    private fun checkCapitals(
        territoryById: Map<TerritoryId, Territory>,
        hexByCoord: Map<HexCoord, Hex>,
        issues: MutableList<MapIssue>,
    ) {
        val capitals =
            hexByCoord.values.mapNotNull { hex ->
                val territory = hex.territory?.let { territoryById[it] }
                val kind = hex.city?.kind
                if (territory == null ||
                    kind == null ||
                    kind == CityKind.CITY
                ) {
                    null
                } else {
                    Triple(hex.coord, kind, territory)
                }
            }
        val national = capitals.filter { it.second == CityKind.NATIONAL_CAPITAL }
        val territorial = capitals.filter { it.second == CityKind.TERRITORY_CAPITAL }

        national
            .filter { it.third.kind != TerritoryKind.HOMELAND }
            .forEach { issues += MapIssue.NationalCapitalOutsideHomeland(it.first) }
        national
            .groupBy({ it.third.nation }, { it.first })
            .filterValues { it.size > 1 }
            .forEach { (nation, coords) -> issues += MapIssue.DuplicateNationalCapital(nation, coords) }

        territorial
            .filter { it.third.kind == TerritoryKind.HOMELAND }
            .forEach { issues += MapIssue.TerritoryCapitalInHomeland(it.first) }
        territorial
            .groupBy({ it.third.id }, { it.first })
            .filterValues { it.size > 1 }
            .forEach { (territory, coords) -> issues += MapIssue.DuplicateTerritoryCapital(territory, coords) }
    }
}
