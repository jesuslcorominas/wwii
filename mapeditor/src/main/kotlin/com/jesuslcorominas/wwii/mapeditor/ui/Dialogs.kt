package com.jesuslcorominas.wwii.mapeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.CityId
import com.jesuslcorominas.wwii.core.map.CityKind
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TerritoryKind
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.mapeditor.model.proposedCityId

private val cityKindLabels =
    mapOf(
        CityKind.CITY to "Ciudad",
        CityKind.TERRITORY_CAPITAL to "Capital de territorio",
        CityKind.NATIONAL_CAPITAL to "Capital de nación",
    )

val territoryKindLabels =
    mapOf(
        TerritoryKind.HOMELAND to "País",
        TerritoryKind.COLONY to "Colonia",
        TerritoryKind.POSSESSION to "Posesión",
    )

@Composable
private fun <T> RadioGroup(
    options: Map<T, String>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column {
        options.forEach { (value, label) ->
            Row(
                Modifier.selectable(selected = value == selected, onClick = { onSelect(value) }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = { onSelect(value) })
                Text(label)
            }
        }
    }
}

/**
 * Sets, changes or removes the city of a land hex. The id is proposed from the name until it is
 * edited by hand, and must be a slug not used by another city ([takenIds]).
 */
@Composable
fun CityDialog(
    coord: HexCoord,
    current: City?,
    takenIds: Set<CityId>,
    onDismiss: () -> Unit,
    onSave: (City?) -> Unit,
) {
    var name by remember { mutableStateOf(current?.name.orEmpty()) }
    var id by remember { mutableStateOf(current?.id?.value.orEmpty()) }
    var idEdited by remember { mutableStateOf(current != null) }
    var kind by remember { mutableStateOf(current?.kind ?: CityKind.CITY) }
    val cityId = id.trim().takeIf { it.isNotBlank() }?.let { CityId(it) }
    val idTaken = cityId != null && cityId in takenIds && cityId != current?.id
    val idMalformed = cityId != null && !cityId.isWellFormed
    val valid = name.isNotBlank() && cityId != null && !idTaken && !idMalformed
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ciudad en $coord") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    name,
                    {
                        name = it
                        if (!idEdited) id = proposedCityId(it)
                    },
                    label = { Text("Nombre") },
                    singleLine = true,
                )
                OutlinedTextField(
                    id,
                    {
                        id = it
                        idEdited = true
                    },
                    label = { Text("Id") },
                    singleLine = true,
                    isError = idTaken || idMalformed,
                    supportingText = {
                        when {
                            idTaken -> Text("Ya existe una ciudad con ese id")
                            idMalformed -> Text("Solo minúsculas sin acentos, cifras y guiones")
                        }
                    },
                )
                RadioGroup(cityKindLabels, kind) { kind = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(City(cityId!!, name.trim(), kind)) }, enabled = valid) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (current != null) TextButton(onClick = { onSave(null) }) { Text("Quitar ciudad") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

/** Creates ([current] null) or edits a territory. Ids must be unique among [takenIds]. */
@Composable
fun TerritoryDialog(
    current: Territory?,
    takenIds: Set<TerritoryId>,
    onDismiss: () -> Unit,
    onSave: (Territory) -> Unit,
) {
    var id by remember { mutableStateOf(current?.id?.value.orEmpty()) }
    var nation by remember { mutableStateOf(current?.nation?.value.orEmpty()) }
    var name by remember { mutableStateOf(current?.name.orEmpty()) }
    var kind by remember { mutableStateOf(current?.kind ?: TerritoryKind.HOMELAND) }
    var discontiguous by remember { mutableStateOf(current?.discontiguous ?: false) }
    val idTaken = id.isNotBlank() && TerritoryId(id.trim()) in takenIds && id.trim() != current?.id?.value
    val valid = id.isNotBlank() && nation.isNotBlank() && name.isNotBlank() && !idTaken
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Nuevo territorio" else "Territorio ${current.id}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    id,
                    { id = it },
                    label = { Text("Id") },
                    singleLine = true,
                    isError = idTaken,
                    supportingText = { if (idTaken) Text("Ya existe un territorio con ese id") },
                )
                OutlinedTextField(nation, { nation = it }, label = { Text("Nación (p. ej. FR)") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                RadioGroup(territoryKindLabels, kind) { kind = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(discontiguous, { discontiguous = it })
                    Text("Discontiguo (islas o exclaves)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(Territory(TerritoryId(id.trim()), NationId(nation.trim()), kind, name.trim(), discontiguous))
                },
                enabled = valid,
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Asks what to do with unsaved changes before [onDiscard] replaces them. */
@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Cambios sin guardar") },
        text = { Text("El mapa tiene cambios sin guardar.") },
        confirmButton = { TextButton(onClick = onSave) { Text("Guardar") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text("Descartar") }
                TextButton(onClick = onCancel) { Text("Cancelar") }
            }
        },
    )
}
