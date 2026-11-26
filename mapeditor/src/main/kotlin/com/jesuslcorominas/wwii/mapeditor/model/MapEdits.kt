package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.Hex
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.HexEdge
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.Strait
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId

/*
 * Editing operations on a map draft. Each one returns a new draft, or the same instance when it
 * changes nothing (so a stroke that changes nothing leaves no undo step). Hexes and rivers keep
 * their declaration order, and new ones go at the end, so saving an untouched map gives the same
 * file.
 *
 * Operations only fix what they own: the hex they touch and the rivers on its sides. Anything else
 * that becomes invalid (a strait whose shore turned to sea, say) is left for the validator to report.
 */

fun MapDraft.hexAt(coord: HexCoord): Hex? = hexes.firstOrNull { it.coord == coord }

fun MapDraft.hasRiver(
    a: HexCoord,
    b: HexCoord,
): Boolean = a.isAdjacentTo(b) && rivers.any { (x, y) -> sameSide(x, y, a, b) }

fun MapDraft.withMetadata(
    id: String,
    name: String,
): MapDraft = if (id == this.id && name == this.name) this else copy(id = id, name = name)

/** Puts [coord] on the board with [terrain]. Water drops every land feature and the rivers around it. */
fun MapDraft.setTerrain(
    coord: HexCoord,
    terrain: Terrain,
): MapDraft {
    val current = hexAt(coord) ?: return copy(hexes = hexes + Hex(coord, terrain))
    val updated =
        when (terrain) {
            Terrain.LAND -> current.copy(terrain = terrain, strait = null)
            Terrain.SEA -> Hex(coord, terrain, strait = current.strait)
            Terrain.LAKE -> Hex(coord, terrain)
        }
    val draft = replaceHex(updated)
    return if (terrain == Terrain.LAND) draft else draft.dropRiversAround(coord)
}

/** Takes [coord] off the board, with the rivers on its sides. */
fun MapDraft.removeHex(coord: HexCoord): MapDraft {
    if (hexAt(coord) == null) return this
    return copy(hexes = hexes.filter { it.coord != coord }).dropRiversAround(coord)
}

/** Makes [coord] a land hex of [territory], keeping its land features if it was land already. */
fun MapDraft.paintTerritory(
    coord: HexCoord,
    territory: TerritoryId,
): MapDraft {
    val current = hexAt(coord) ?: return copy(hexes = hexes + Hex(coord, Terrain.LAND, territory))
    if (current.isLand) return replaceHex(current.copy(territory = territory))
    return replaceHex(Hex(coord, Terrain.LAND, territory))
}

fun MapDraft.toggleMountain(coord: HexCoord): MapDraft = updateLand(coord) { it.copy(mountain = !it.mountain) }

fun MapDraft.toggleAirport(coord: HexCoord): MapDraft = updateLand(coord) { it.copy(airport = !it.airport) }

/** Sets or removes (null) the city of a land hex. */
fun MapDraft.setCity(
    coord: HexCoord,
    city: City?,
): MapDraft = updateLand(coord) { it.copy(city = city) }

/** Adds or removes the river on the side between the adjacent hexes [a] and [b]. */
fun MapDraft.toggleRiver(
    a: HexCoord,
    b: HexCoord,
): MapDraft {
    if (!a.isAdjacentTo(b)) return this
    val edge = HexEdge.of(a, b)
    return if (hasRiver(a, b)) {
        copy(rivers = rivers.filterNot { (x, y) -> sameSide(x, y, a, b) })
    } else {
        copy(rivers = rivers + (edge.first to edge.second))
    }
}

/** Makes the sea hex [coord] a strait between [shoreA] and [shoreB]. Ignored on other terrain. */
fun MapDraft.setStrait(
    coord: HexCoord,
    shoreA: HexCoord,
    shoreB: HexCoord,
): MapDraft {
    val current = hexAt(coord)?.takeIf { it.terrain == Terrain.SEA } ?: return this
    return replaceHex(current.copy(strait = Strait(shoreA, shoreB)))
}

fun MapDraft.clearStrait(coord: HexCoord): MapDraft {
    val current = hexAt(coord)?.takeIf { it.strait != null } ?: return this
    return replaceHex(current.copy(strait = null))
}

fun MapDraft.addTerritory(territory: Territory): MapDraft = copy(territories = territories + territory)

/** Replaces the territory [id] with [territory]; if the id changes, its hexes follow. */
fun MapDraft.updateTerritory(
    id: TerritoryId,
    territory: Territory,
): MapDraft {
    if (territories.none { it.id == id }) return this
    val renamed =
        if (territory.id == id) {
            hexes
        } else {
            hexes.map { if (it.territory == id) it.copy(territory = territory.id) else it }
        }
    return copy(territories = territories.map { if (it.id == id) territory else it }, hexes = renamed)
}

/** Deletes the territory [id]; its hexes stay as land without territory. */
fun MapDraft.removeTerritory(id: TerritoryId): MapDraft {
    if (territories.none { it.id == id }) return this
    return copy(
        territories = territories.filter { it.id != id },
        hexes = hexes.map { if (it.territory == id) it.copy(territory = null) else it },
    )
}

private fun MapDraft.updateLand(
    coord: HexCoord,
    change: (Hex) -> Hex,
): MapDraft {
    val current = hexAt(coord)?.takeIf { it.isLand } ?: return this
    return replaceHex(change(current))
}

private fun MapDraft.replaceHex(hex: Hex): MapDraft {
    if (hexAt(hex.coord) == hex) return this
    return copy(hexes = hexes.map { if (it.coord == hex.coord) hex else it })
}

private fun MapDraft.dropRiversAround(coord: HexCoord): MapDraft {
    val kept = rivers.filterNot { (a, b) -> a == coord || b == coord }
    return if (kept.size == rivers.size) this else copy(rivers = kept)
}

// A loaded draft can hold rivers between hexes that are not adjacent; they never match a side.
private fun sameSide(
    x: HexCoord,
    y: HexCoord,
    a: HexCoord,
    b: HexCoord,
): Boolean = (x == a && y == b) || (x == b && y == a)
