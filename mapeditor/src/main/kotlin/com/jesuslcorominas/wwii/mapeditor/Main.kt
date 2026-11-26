package com.jesuslcorominas.wwii.mapeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.mapeditor.model.hexAt
import com.jesuslcorominas.wwii.mapeditor.ui.CityDialog
import com.jesuslcorominas.wwii.mapeditor.ui.EditorState
import com.jesuslcorominas.wwii.mapeditor.ui.EditorTheme
import com.jesuslcorominas.wwii.mapeditor.ui.MapCanvas
import com.jesuslcorominas.wwii.mapeditor.ui.SidePanel
import com.jesuslcorominas.wwii.mapeditor.ui.TerritoryDialog
import com.jesuslcorominas.wwii.mapeditor.ui.UnsavedChangesDialog
import java.awt.FileDialog
import java.io.File

private val isMac = System.getProperty("os.name").orEmpty().startsWith("Mac")

private fun shortcut(
    key: Key,
    shift: Boolean = false,
) = KeyShortcut(key, meta = isMac, ctrl = !isMac, shift = shift)

/** Internal map digitizing tool. Optional argument: a map JSON file to open. */
fun main(args: Array<String>) =
    application {
        val state = remember { EditorState().also { s -> args.firstOrNull()?.let { s.open(File(it)) } } }
        var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

        fun guarded(action: () -> Unit) {
            if (state.dirty) pendingAction = action else action()
        }

        Window(
            onCloseRequest = { guarded(::exitApplication) },
            title = state.title,
            state = rememberWindowState(size = DpSize(1400.dp, 900.dp)),
        ) {
            EditorTheme {
                EditorMenu(state, ::guarded)
                EditorWindowContent(state)
                pendingAction?.let { action ->
                    UnsavedChangesDialog(
                        onSave = {
                            pendingAction = null
                            if (saveOrAsk(state)) action()
                        },
                        onDiscard = {
                            pendingAction = null
                            action()
                        },
                        onCancel = { pendingAction = null },
                    )
                }
            }
        }
    }

@androidx.compose.runtime.Composable
private fun FrameWindowScope.EditorMenu(
    state: EditorState,
    guarded: (() -> Unit) -> Unit,
) {
    MenuBar {
        Menu("Archivo") {
            Item("Nuevo", shortcut = shortcut(Key.N)) { guarded { state.newMap() } }
            Item("Abrir…", shortcut = shortcut(Key.O)) {
                guarded { chooseFile(window, "Abrir mapa", FileDialog.LOAD, "*.json")?.let { state.open(it) } }
            }
            Item("Guardar", shortcut = shortcut(Key.S)) { saveOrAsk(state) }
            Item("Guardar como…", shortcut = shortcut(Key.S, shift = true)) { saveAs(state) }
            Separator()
            Item("Cargar imagen de referencia…", shortcut = shortcut(Key.I)) {
                chooseFile(window, "Imagen de referencia", FileDialog.LOAD, null)?.let { state.loadImage(it) }
            }
        }
        Menu("Edición") {
            Item("Deshacer", enabled = state.canUndo, shortcut = shortcut(Key.Z)) { state.undo() }
            Item("Rehacer", enabled = state.canRedo, shortcut = shortcut(Key.Z, shift = true)) { state.redo() }
        }
        Menu("Vista") {
            Item("Encajar", shortcut = shortcut(Key.Zero)) { state.fitView() }
        }
    }
}

@androidx.compose.runtime.Composable
private fun EditorWindowContent(state: EditorState) {
    var territoryEditing by remember { mutableStateOf<Territory?>(null) }
    var territoryDialogOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            MapCanvas(state, Modifier.weight(1f).fillMaxHeight())
            SidePanel(
                state,
                onEditTerritory = {
                    territoryEditing = it
                    territoryDialogOpen = true
                },
                modifier = Modifier.width(380.dp).fillMaxHeight(),
            )
        }
        Text(
            state.status.ifEmpty { " " },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }

    state.cityEditing?.let { coord ->
        CityDialog(
            coord = coord,
            current = state.draft.hexAt(coord)?.city,
            onDismiss = { state.cityEditing = null },
            onSave = {
                state.setCity(coord, it)
                state.cityEditing = null
            },
        )
    }
    if (territoryDialogOpen) {
        val editing = territoryEditing
        TerritoryDialog(
            current = editing,
            takenIds =
                state.draft.territories
                    .map { it.id }
                    .toSet(),
            onDismiss = { territoryDialogOpen = false },
            onSave = {
                if (editing == null) state.addTerritory(it) else state.updateTerritory(editing.id, it)
                territoryDialogOpen = false
            },
        )
    }
}

private fun saveOrAsk(state: EditorState): Boolean = if (state.file != null) state.save() else saveAs(state)

private fun saveAs(state: EditorState): Boolean {
    val target = chooseFile(null, "Guardar mapa", FileDialog.SAVE, "${state.draft.id}.json") ?: return false
    val withExtension = if (target.name.endsWith(".json")) target else File(target.parentFile, target.name + ".json")
    return state.save(withExtension)
}

private fun chooseFile(
    parent: java.awt.Frame?,
    title: String,
    mode: Int,
    fileName: String?,
): File? {
    val dialog = FileDialog(parent, title, mode)
    fileName?.let { dialog.file = it }
    dialog.isVisible = true
    val name = dialog.file ?: return null
    return File(dialog.directory, name)
}
