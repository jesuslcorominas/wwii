package com.jesuslcorominas.wwii.mapeditor.ui

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.InvalidMapException
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.MapIssue
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.mapeditor.model.Calibration
import com.jesuslcorominas.wwii.mapeditor.model.History
import com.jesuslcorominas.wwii.mapeditor.model.IssueLocator
import com.jesuslcorominas.wwii.mapeditor.model.MapFiles
import com.jesuslcorominas.wwii.mapeditor.model.Point
import com.jesuslcorominas.wwii.mapeditor.model.addTerritory
import com.jesuslcorominas.wwii.mapeditor.model.clearStrait
import com.jesuslcorominas.wwii.mapeditor.model.hexAt
import com.jesuslcorominas.wwii.mapeditor.model.paintTerritory
import com.jesuslcorominas.wwii.mapeditor.model.removeHex
import com.jesuslcorominas.wwii.mapeditor.model.removeTerritory
import com.jesuslcorominas.wwii.mapeditor.model.setCity
import com.jesuslcorominas.wwii.mapeditor.model.setStrait
import com.jesuslcorominas.wwii.mapeditor.model.setTerrain
import com.jesuslcorominas.wwii.mapeditor.model.toggleAirport
import com.jesuslcorominas.wwii.mapeditor.model.toggleMountain
import com.jesuslcorominas.wwii.mapeditor.model.toggleRiver
import com.jesuslcorominas.wwii.mapeditor.model.updateTerritory
import com.jesuslcorominas.wwii.mapeditor.model.withMetadata
import org.jetbrains.skia.Image
import java.io.File
import kotlin.math.exp

enum class Tool(
    val label: String,
) {
    INSPECT("Inspeccionar"),
    LAND("Tierra"),
    SEA("Mar"),
    LAKE("Lago"),
    ERASE("Quitar hex"),
    TERRITORY("Territorio"),
    MOUNTAIN("Montaña"),
    AIRPORT("Aeropuerto"),
    CITY("Ciudad"),
    RIVER("Río"),
    STRAIT("Estrecho"),
    MOVE_GRID("Mover rejilla"),
}

/** A strait being placed: its sea hex, then its first shore. */
data class StraitPick(
    val sea: HexCoord,
    val firstShore: HexCoord? = null,
)

/** Everything the editor window shows. Map edits go through [MapDraft] operations and [History]. */
class EditorState {
    var draft by mutableStateOf(MapDraft(id = "new-map", name = "Mapa nuevo"))
        private set
    var file by mutableStateOf<File?>(null)
        private set
    var dirty by mutableStateOf(false)
        private set
    private var calibrationState by mutableStateOf(Calibration())

    /** Grid and image settings; changing them counts as an unsaved change (they go to the sidecar). */
    var calibration: Calibration
        get() = calibrationState
        set(value) {
            if (value != calibrationState) {
                calibrationState = value
                dirty = true
            }
        }
    var image by mutableStateOf<ImageBitmap?>(null)
        private set

    var tool by mutableStateOf(Tool.INSPECT)
    var selectedTerritory by mutableStateOf<TerritoryId?>(null)
    var hover by mutableStateOf<HexCoord?>(null)
    var straitPick by mutableStateOf<StraitPick?>(null)
        private set
    var cityEditing by mutableStateOf<HexCoord?>(null)
    var status by mutableStateOf("")
    var selectedIssue by mutableStateOf<MapIssue?>(null)
        private set

    // View: screen = image * scale + offset.
    var scale by mutableStateOf(1f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set
    var canvasSize by mutableStateOf(Size.Zero)

    private val history = History<MapDraft>()
    private var strokeStart: MapDraft? = null
    private val strokeVisited = mutableSetOf<Any>()

    val issues: List<MapIssue> by derivedStateOf { draft.issues() }
    val issueHexes: Set<HexCoord> by derivedStateOf { issues.flatMap { IssueLocator.hexesOf(it, draft) }.toSet() }
    val highlighted: Set<HexCoord> by derivedStateOf {
        selectedIssue?.let { IssueLocator.hexesOf(it, draft).toSet() }.orEmpty()
    }
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    val title: String get() = (file?.name ?: "Sin guardar") + (if (dirty) " *" else "") + " — Editor del mapa"

    // ---- Files -----------------------------------------------------------------------------

    fun newMap() {
        resetTo(MapDraft(id = "new-map", name = "Mapa nuevo"), null)
        status = "Mapa nuevo"
    }

    fun open(target: File) {
        val loaded =
            try {
                MapFiles.read(target)
            } catch (e: InvalidMapException) {
                status = "No se pudo abrir ${target.name}: " + e.issues.joinToString("; ") { it.message }
                return
            }
        resetTo(loaded, target)
        val sidecar = Calibration.sidecarOf(target)
        if (sidecar.isFile) {
            runCatching { Calibration.decode(sidecar.readText()) }
                .onSuccess { calibration = it }
                .onFailure { status = "Calibración ignorada (${sidecar.name} no es válido)" }
            calibration.imagePath?.let { path -> loadImage(File(path), keepCalibration = true) }
        }
        dirty = false
        status = "Abierto ${target.name}" + issuesSuffix()
        fitView()
    }

    /** Saves to [target] (or the current file). Invalid maps are saved too, with a warning. */
    fun save(target: File? = file): Boolean {
        val destination = target ?: return false
        MapFiles.write(destination, draft)
        Calibration.sidecarOf(destination).writeText(calibration.encode() + "\n")
        file = destination
        dirty = false
        status = "Guardado ${destination.name}" + issuesSuffix()
        return true
    }

    fun loadImage(
        source: File,
        keepCalibration: Boolean = false,
    ) {
        val bitmap =
            runCatching { Image.makeFromEncoded(source.readBytes()).toComposeImageBitmap() }
                .getOrElse {
                    status = "No se pudo cargar la imagen ${source.path}"
                    return
                }
        image = bitmap
        if (!keepCalibration) calibration = calibration.copy(imagePath = source.absolutePath)
        fitView()
    }

    private fun resetTo(
        newDraft: MapDraft,
        newFile: File?,
    ) {
        draft = newDraft
        file = newFile
        dirty = false
        history.clear()
        syncHistory()
        calibrationState = Calibration()
        image = null
        selectedTerritory = newDraft.territories.firstOrNull()?.id
        selectedIssue = null
        straitPick = null
        fitView()
    }

    private fun issuesSuffix(): String = if (issues.isEmpty()) "" else " (${issues.size} problema(s) del validador)"

    // ---- Editing ---------------------------------------------------------------------------

    private fun commit(change: MapDraft.() -> MapDraft) {
        val before = draft
        val after = before.change()
        if (after === before) return
        history.record(before)
        draft = after
        dirty = true
        syncHistory()
    }

    private fun syncHistory() {
        canUndo = history.canUndo
        canRedo = history.canRedo
    }

    fun undo() {
        history.undo(draft)?.let {
            draft = it
            dirty = true
        }
        syncHistory()
    }

    fun redo() {
        history.redo(draft)?.let {
            draft = it
            dirty = true
        }
        syncHistory()
    }

    fun setMetadata(
        id: String,
        name: String,
    ) = commit { withMetadata(id, name) }

    fun setCity(
        coord: HexCoord,
        city: City?,
    ) = commit { setCity(coord, city) }

    fun addTerritory(territory: Territory) {
        commit { addTerritory(territory) }
        selectedTerritory = territory.id
    }

    fun updateTerritory(
        id: TerritoryId,
        territory: Territory,
    ) {
        commit { updateTerritory(id, territory) }
        if (selectedTerritory == id) selectedTerritory = territory.id
    }

    fun removeTerritory(id: TerritoryId) {
        commit { removeTerritory(id) }
        if (selectedTerritory == id) selectedTerritory = draft.territories.firstOrNull()?.id
    }

    fun selectTool(newTool: Tool) {
        tool = newTool
        straitPick = null
        status =
            when (newTool) {
                Tool.TERRITORY -> if (selectedTerritory == null) "Crea o elige un territorio en el panel" else ""
                Tool.STRAIT -> "Clic en un hex de mar y después en sus dos orillas; clic en un estrecho lo quita"
                Tool.RIVER -> "Clic cerca de un lado para poner o quitar el río"
                Tool.MOVE_GRID -> "Arrastra para mover el origen de la rejilla"
                else -> ""
            }
    }

    // ---- Pointer ---------------------------------------------------------------------------

    fun toImage(screen: Offset): Point =
        Point(
            ((screen.x - offset.x) / scale).toDouble(),
            (
                (screen.y - offset.y) /
                    scale
            ).toDouble(),
        )

    fun hexAt(screen: Offset): HexCoord = calibration.layout.hexAt(toImage(screen))

    fun startStroke(screen: Offset) {
        strokeStart = draft
        strokeVisited.clear()
        when (tool) {
            Tool.CITY -> {
                val coord = hexAt(screen)
                if (draft.hexAt(coord)?.isLand ==
                    true
                ) {
                    cityEditing = coord
                } else {
                    status = "Las ciudades van en hexes de tierra"
                }
            }
            Tool.STRAIT -> pickStrait(hexAt(screen))
            else -> strokeAt(screen)
        }
    }

    fun strokeAt(screen: Offset) {
        val coord = hexAt(screen)
        hover = coord
        val change: (MapDraft.() -> MapDraft)? =
            when (tool) {
                Tool.LAND -> {
                    { setTerrain(coord, Terrain.LAND) }
                }
                Tool.SEA -> {
                    { setTerrain(coord, Terrain.SEA) }
                }
                Tool.LAKE -> {
                    { setTerrain(coord, Terrain.LAKE) }
                }
                Tool.ERASE -> {
                    { removeHex(coord) }
                }
                Tool.TERRITORY -> selectedTerritory?.let { id -> { paintTerritory(coord, id) } }
                Tool.MOUNTAIN -> onceFor(coord) { toggleMountain(coord) }
                Tool.AIRPORT -> onceFor(coord) { toggleAirport(coord) }
                Tool.RIVER -> {
                    val (a, b) = calibration.layout.nearestSide(toImage(screen))
                    onceFor(setOf(a, b)) { toggleRiver(a, b) }
                }
                Tool.INSPECT, Tool.CITY, Tool.STRAIT, Tool.MOVE_GRID -> null
            }
        change?.let { draft = draft.it() }
    }

    fun endStroke() {
        val start = strokeStart ?: return
        strokeStart = null
        if (draft !== start) {
            history.record(start)
            dirty = true
            syncHistory()
        }
    }

    /** Toggles act once per hex (or side) in a stroke, so dragging does not flicker them. */
    private fun onceFor(
        key: Any,
        change: MapDraft.() -> MapDraft,
    ): (MapDraft.() -> MapDraft)? = if (strokeVisited.add(key)) change else null

    private fun pickStrait(coord: HexCoord) {
        val pick = straitPick
        val hex = draft.hexAt(coord)
        when {
            pick == null && hex?.strait != null -> {
                commit { clearStrait(coord) }
                status = "Estrecho $coord quitado"
            }
            pick == null && hex?.terrain == Terrain.SEA -> {
                straitPick = StraitPick(coord)
                status = "Estrecho en $coord: clic en la primera orilla"
            }
            pick == null -> status = "Un estrecho es un hex de mar: empieza por el mar"
            !coord.isAdjacentTo(pick.sea) -> status = "La orilla tiene que ser vecina de ${pick.sea}"
            pick.firstShore == null -> {
                straitPick = pick.copy(firstShore = coord)
                status = "Primera orilla $coord: clic en la segunda"
            }
            else -> {
                commit { setStrait(pick.sea, pick.firstShore, coord) }
                straitPick = null
                status = "Estrecho en ${pick.sea} entre ${pick.firstShore} y $coord"
            }
        }
    }

    fun moveGrid(deltaScreen: Offset) {
        calibration =
            calibration.copy(
                originX = calibration.originX + deltaScreen.x / scale,
                originY = calibration.originY + deltaScreen.y / scale,
            )
    }

    fun pan(deltaScreen: Offset) {
        offset += deltaScreen
    }

    fun zoom(
        scrollDelta: Float,
        pivot: Offset,
    ) {
        val newScale = (scale * exp(-scrollDelta * ZOOM_STEP)).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = pivot - (pivot - offset) * (newScale / scale)
        scale = newScale
    }

    // ---- View ------------------------------------------------------------------------------

    fun selectIssue(issue: MapIssue?) {
        selectedIssue = issue
        val coords = issue?.let { IssueLocator.hexesOf(it, draft) }.orEmpty()
        if (coords.isNotEmpty()) centerOn(coords)
    }

    private fun centerOn(coords: List<HexCoord>) {
        val points = coords.map { calibration.layout.center(it) }
        val cx = points.map { it.x }.average().toFloat()
        val cy = points.map { it.y }.average().toFloat()
        offset = Offset(canvasSize.width / 2 - cx * scale, canvasSize.height / 2 - cy * scale)
    }

    /** Fits the image (or the map) in the canvas. */
    fun fitView() {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return
        val img = image
        val (w, h) =
            if (img != null) {
                img.width.toFloat() to img.height.toFloat()
            } else {
                val l = calibration.layout
                (l.hexWidth * DEFAULT_COLUMNS).toFloat() to (l.hexHeight * DEFAULT_ROWS * ROW_FACTOR).toFloat()
            }
        scale = minOf(canvasSize.width / w, canvasSize.height / h).coerceIn(MIN_SCALE, MAX_SCALE)
        offset = Offset((canvasSize.width - w * scale) / 2, (canvasSize.height - h * scale) / 2)
    }

    companion object {
        const val DEFAULT_COLUMNS = 48
        const val DEFAULT_ROWS = 44
        private const val ROW_FACTOR = 0.75
        private const val ZOOM_STEP = 0.15f
        private const val MIN_SCALE = 0.05f
        private const val MAX_SCALE = 20f
    }
}
