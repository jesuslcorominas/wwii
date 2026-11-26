package com.jesuslcorominas.wwii.mapeditor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.jesuslcorominas.wwii.core.map.CityKind
import com.jesuslcorominas.wwii.core.map.Hex
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.mapeditor.model.HexLayout
import com.jesuslcorominas.wwii.mapeditor.model.NationColors
import com.jesuslcorominas.wwii.mapeditor.model.Point

private const val MIN_SCALE_FOR_LABELS = 0.6f
private const val MIN_SCALE_FOR_GRID = 0.12f

/** The reference image, the grid and the map layers, with zoom, pan and the painting tools. */
@Composable
fun MapCanvas(
    state: EditorState,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val layout = state.calibration.layout
    val image = state.image
    val gridCoords =
        remember(layout, image) {
            if (image != null) {
                layout.coordsInside(image.width.toDouble(), image.height.toDouble())
            } else {
                layout.coordsInside(
                    layout.hexWidth * EditorState.DEFAULT_COLUMNS,
                    layout.hexHeight * EditorState.DEFAULT_ROWS * 0.75,
                )
            }
        }
    val gridPath = remember(layout, gridCoords) { outlines(layout, gridCoords) }
    val fills = remember(layout, state.draft) { fillsByColor(layout, state.draft) }

    Canvas(
        modifier
            .background(EditorColors.canvasBackground)
            .onSizeChanged {
                val first = state.canvasSize.width <= 0f
                state.canvasSize = it.toSize()
                if (first) state.fitView()
            }.pointerInput(state) { handlePointer(state) },
    ) {
        withTransform({
            translate(state.offset.x, state.offset.y)
            scale(state.scale, state.scale, pivot = Offset.Zero)
        }) {
            val px = 1f / state.scale // one screen pixel, in image pixels
            image?.let {
                drawImage(it, alpha = state.calibration.imageOpacity)
            }
            val dataAlpha = state.calibration.dataOpacity
            fills.forEach { (color, path) -> drawPath(path, color, alpha = dataAlpha) }
            if (state.scale >= MIN_SCALE_FOR_GRID) {
                drawPath(gridPath, EditorColors.grid, style = Stroke(width = px))
            }
            drawFeatures(state.draft, layout, px)
            drawHighlights(state, layout, px)
        }
        if (state.scale >= MIN_SCALE_FOR_LABELS) {
            state.draft.hexes.filter { it.city != null }.forEach { hex ->
                val p = layout.center(hex.coord)
                val screen = Offset((p.x * state.scale).toFloat(), (p.y * state.scale).toFloat()) + state.offset
                val label = textMeasurer.measure(hex.city!!.name, TextStyle(fontSize = 11.sp, color = EditorColors.ink))
                drawText(
                    label,
                    topLeft = screen + Offset(-label.size.width / 2f, (layout.hexHeight * state.scale / 5).toFloat()),
                )
            }
        }
    }
}

private suspend fun PointerInputScope.handlePointer(state: EditorState) {
    awaitPointerEventScope {
        var panning = false
        var painting = false
        var last = Offset.Zero
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.first()
            val pos = change.position
            when (event.type) {
                PointerEventType.Scroll -> {
                    state.zoom(change.scrollDelta.y, pos)
                    change.consume()
                }
                PointerEventType.Press -> {
                    last = pos
                    if (event.buttons.isSecondaryPressed || event.buttons.isTertiaryPressed) {
                        panning = true
                    } else if (event.buttons.isPrimaryPressed) {
                        painting = true
                        if (state.tool != Tool.MOVE_GRID) state.startStroke(pos)
                    }
                }
                PointerEventType.Move -> {
                    val delta = pos - last
                    last = pos
                    when {
                        panning -> state.pan(delta)
                        painting && state.tool == Tool.MOVE_GRID -> state.moveGrid(delta)
                        painting && state.tool != Tool.CITY && state.tool != Tool.STRAIT -> state.strokeAt(pos)
                        else -> state.hover = state.hexAt(pos)
                    }
                }
                PointerEventType.Release -> {
                    if (painting) state.endStroke()
                    panning = false
                    painting = false
                }
                PointerEventType.Exit -> state.hover = null
                else -> Unit
            }
        }
    }
}

private fun hexPath(
    layout: HexLayout,
    coord: HexCoord,
    path: Path = Path(),
): Path {
    val corners = layout.corners(coord)
    path.moveTo(corners[0].x.toFloat(), corners[0].y.toFloat())
    corners.drop(1).forEach { path.lineTo(it.x.toFloat(), it.y.toFloat()) }
    path.close()
    return path
}

private fun outlines(
    layout: HexLayout,
    coords: List<HexCoord>,
): Path {
    val path = Path()
    coords.forEach { hexPath(layout, it, path) }
    return path
}

private fun fillsByColor(
    layout: HexLayout,
    draft: MapDraft,
): Map<Color, Path> {
    val territories = draft.territories.associateBy { it.id }
    return draft.hexes
        .groupBy { hex -> fillColor(hex, territories[hex.territory]) }
        .mapValues { (_, hexes) -> outlines(layout, hexes.map { it.coord }) }
}

private fun fillColor(
    hex: Hex,
    territory: Territory?,
): Color =
    when (hex.terrain) {
        Terrain.SEA -> EditorColors.sea
        Terrain.LAKE -> EditorColors.lake
        Terrain.LAND ->
            territory?.let { Color(0xFF000000.toInt() or NationColors.rgb(it.nation, it.kind)) } ?: EditorColors.land
    }

private fun DrawScope.drawFeatures(
    draft: MapDraft,
    layout: HexLayout,
    px: Float,
) {
    val r = (minOf(layout.hexWidth, layout.hexHeight) / 2).toFloat()
    draft.hexes.forEach { hex ->
        val c = layout.center(hex.coord).toOffset()
        if (hex.mountain) {
            val m =
                Path().apply {
                    moveTo(c.x - r * 0.45f, c.y + r * 0.25f)
                    lineTo(c.x, c.y - r * 0.45f)
                    lineTo(c.x + r * 0.45f, c.y + r * 0.25f)
                    close()
                }
            drawPath(m, EditorColors.mountain)
        }
        if (hex.airport && hex.city == null) {
            drawCircle(
                EditorColors.ink,
                radius = r * 0.18f,
                center = c + Offset(r * 0.45f, -r * 0.4f),
                style =
                    Stroke(
                        2 * px,
                    ),
            )
        }
        hex.city?.let { city ->
            val size = r * if (city.kind == CityKind.CITY) 0.18f else 0.28f
            when (city.kind) {
                CityKind.NATIONAL_CAPITAL -> {
                    drawRect(EditorColors.ink, topLeft = c - Offset(size, size), size = Size(size * 2, size * 2))
                    drawCircle(EditorColors.objective, radius = size * 0.55f, center = c)
                }
                CityKind.TERRITORY_CAPITAL -> {
                    drawCircle(EditorColors.ink, radius = size, center = c)
                    drawCircle(EditorColors.objective, radius = size * 0.55f, center = c)
                }
                CityKind.CITY -> drawCircle(EditorColors.ink, radius = size, center = c)
            }
        }
        hex.strait?.let { strait ->
            val a = layout.center(strait.shoreA).toOffset()
            val b = layout.center(strait.shoreB).toOffset()
            drawLine(
                EditorColors.ink,
                a,
                b,
                strokeWidth = 3 * px,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6 * px, 4 * px)),
            )
            drawCircle(EditorColors.ink, radius = r * 0.15f, center = c, style = Stroke(2 * px))
        }
    }
    draft.rivers.forEach { (a, b) ->
        if (a.isAdjacentTo(b)) {
            val (p, q) = layout.sideEnds(a, b)
            drawLine(EditorColors.river, p.toOffset(), q.toOffset(), strokeWidth = maxOf(4 * px, r * 0.2f))
        }
    }
}

private fun DrawScope.drawHighlights(
    state: EditorState,
    layout: HexLayout,
    px: Float,
) {
    state.issueHexes.forEach { drawPath(hexPath(layout, it), EditorColors.issue, style = Stroke(2 * px)) }
    state.highlighted.forEach { drawPath(hexPath(layout, it), EditorColors.selection, style = Stroke(4 * px)) }
    state.straitPick?.let { pick ->
        drawPath(hexPath(layout, pick.sea), EditorColors.selection, style = Stroke(3 * px))
        pick.firstShore?.let { drawPath(hexPath(layout, it), EditorColors.selection, style = Stroke(3 * px)) }
    }
    state.hover?.let { drawPath(hexPath(layout, it), EditorColors.hover, style = Stroke(2 * px)) }
    // Origin marker: centre of hex (0, 0).
    val o = layout.center(HexCoord(0, 0)).toOffset()
    drawCircle(EditorColors.selection, radius = 4 * px, center = o)
}

private fun Point.toOffset(): Offset = Offset(x.toFloat(), y.toFloat())
