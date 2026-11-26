package com.jesuslcorominas.wwii.mapeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.mapeditor.model.NationColors
import com.jesuslcorominas.wwii.mapeditor.model.hexAt

private const val MAX_ISSUES_SHOWN = 200

@Composable
fun SidePanel(
    state: EditorState,
    onEditTerritory: (Territory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MapSection(state)
        HorizontalDivider()
        GridSection(state)
        HorizontalDivider()
        ToolSection(state)
        HorizontalDivider()
        TerritorySection(state, onEditTerritory)
        HorizontalDivider()
        IssueSection(state)
        HorizontalDivider()
        InspectorSection(state)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MapSection(state: EditorState) {
    SectionTitle("Mapa")
    CommittedTextField("Id", state.draft.id) { state.setMetadata(it, state.draft.name) }
    CommittedTextField("Nombre", state.draft.name) { state.setMetadata(state.draft.id, it) }
}

/**
 * A text field that edits locally and applies its value on Enter or when it loses focus, so a value
 * half typed is never applied (and a name is one undo step, not one per key). [parse] returns null
 * for values that cannot be applied; the field then goes back to the current value.
 */
@Composable
private fun <T> ApplyOnDoneField(
    label: String,
    value: T,
    show: (T) -> String,
    parse: (String) -> T?,
    modifier: Modifier = Modifier,
    onApply: (T) -> Unit,
) {
    var text by remember { mutableStateOf(show(value)) }
    LaunchedEffect(value) { text = show(value) }

    fun apply() {
        val parsed = parse(text)
        if (parsed == null) {
            text = show(value)
        } else if (parsed != value) {
            onApply(parsed)
        }
    }
    OutlinedTextField(
        text,
        { text = it },
        label = { Text(label) },
        singleLine = true,
        modifier =
            modifier
                .onFocusChanged { if (!it.isFocused) apply() }
                .onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) {
                        apply()
                        true
                    } else {
                        false
                    }
                },
    )
}

@Composable
private fun CommittedTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
) = ApplyOnDoneField(label, value, { it }, { it.trim().ifBlank { null } }, Modifier.fillMaxWidth(), onCommit)

/** Hex sizes under this many image pixels are rejected (they are typos, not a calibration). */
private const val MIN_HEX_SIZE = 4.0

@Composable
private fun NumberField(
    label: String,
    value: Double,
    modifier: Modifier = Modifier,
    min: Double = Double.NEGATIVE_INFINITY,
    onChange: (Double) -> Unit,
) = ApplyOnDoneField(
    label,
    value,
    ::format,
    { it.replace(',', '.').toDoubleOrNull()?.takeIf { v -> v >= min } },
    modifier,
    onChange,
)

private fun format(value: Double): String = "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')

@Composable
private fun GridSection(state: EditorState) {
    SectionTitle("Rejilla (pointy-top, píxeles de la imagen)")
    val c = state.calibration
    var linked by remember { mutableStateOf(true) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("Origen X", c.originX, Modifier.weight(1f)) {
            state.calibration =
                state.calibration.copy(originX = it)
        }
        NumberField("Origen Y", c.originY, Modifier.weight(1f)) {
            state.calibration =
                state.calibration.copy(originY = it)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberField("Ancho", c.hexWidth, Modifier.weight(1f), min = MIN_HEX_SIZE) { w ->
            if (w > 0) {
                val cur = state.calibration
                val h = if (linked) w * cur.hexHeight / cur.hexWidth else cur.hexHeight
                state.calibration = cur.copy(hexWidth = w, hexHeight = h)
            }
        }
        NumberField("Alto", c.hexHeight, Modifier.weight(1f), min = MIN_HEX_SIZE) { h ->
            if (h > 0) {
                val cur = state.calibration
                val w = if (linked) h * cur.hexWidth / cur.hexHeight else cur.hexWidth
                state.calibration = cur.copy(hexWidth = w, hexHeight = h)
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(linked, { linked = it })
        Text("Mantener proporción", style = MaterialTheme.typography.bodySmall)
    }
    Text("Imagen: ${state.calibration.imagePath ?: "ninguna"}", style = MaterialTheme.typography.bodySmall)
    LabeledSlider("Opacidad de la imagen", c.imageOpacity) {
        state.calibration =
            state.calibration.copy(imageOpacity = it)
    }
    LabeledSlider(
        "Opacidad de los datos",
        c.dataOpacity,
    ) { state.calibration = state.calibration.copy(dataOpacity = it) }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.bodySmall)
    Slider(value, onChange, valueRange = 0f..1f)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolSection(state: EditorState) {
    SectionTitle("Herramientas")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Tool.entries.forEach { tool ->
            FilterChip(
                selected = state.tool == tool,
                onClick = { state.selectTool(tool) },
                label = { Text(tool.label) },
            )
        }
    }
    Text(
        "Rueda: zoom · botón derecho o central: desplazar",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TerritorySection(
    state: EditorState,
    onEditTerritory: (Territory?) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { SectionTitle("Territorios (${state.draft.territories.size})") }
        TextButton(onClick = { onEditTerritory(null) }) { Text("Nuevo") }
    }
    state.draft.territories.forEach { t ->
        Row(
            Modifier.fillMaxWidth().clickable { state.selectedTerritory = t.id },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(state.selectedTerritory == t.id, { state.selectedTerritory = t.id })
            Box(Modifier.size(14.dp).background(Color(0xFF000000.toInt() or NationColors.rgb(t.nation, t.kind))))
            Text(
                " ${t.id} · ${t.nation} · ${t.name} (${territoryKindLabels.getValue(t.kind)})",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onEditTerritory(t) }) { Text("Editar") }
            TextButton(onClick = { state.removeTerritory(t.id) }) { Text("Borrar") }
        }
    }
}

@Composable
private fun IssueSection(state: EditorState) {
    val issues = state.issues
    SectionTitle("Problemas del validador (${issues.size})")
    if (issues.isEmpty()) {
        Text("El mapa es válido.", style = MaterialTheme.typography.bodySmall)
        return
    }
    issues.take(MAX_ISSUES_SHOWN).forEach { issue ->
        val selected = issue == state.selectedIssue
        Text(
            issue.message,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth().clickable { state.selectIssue(if (selected) null else issue) },
        )
    }
    if (issues.size > MAX_ISSUES_SHOWN) {
        Text("… y ${issues.size - MAX_ISSUES_SHOWN} más", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InspectorSection(state: EditorState) {
    SectionTitle("Hex bajo el cursor")
    val coord = state.hover
    if (coord == null) {
        Text("—", style = MaterialTheme.typography.bodySmall)
        return
    }
    val hex = state.draft.hexAt(coord)
    val description =
        if (hex == null) {
            "$coord fuera del tablero"
        } else {
            buildList {
                add("$coord ${hex.terrain.name.lowercase()}")
                hex.territory?.let { add("territorio $it") }
                if (hex.mountain) add("montaña")
                if (hex.airport) add("aeropuerto")
                hex.city?.let { add("${it.name} (${it.kind.name.lowercase()})") }
                hex.strait?.let { add("estrecho ${it.shoreA}–${it.shoreB}") }
            }.joinToString(" · ")
        }
    Text(description, style = MaterialTheme.typography.bodySmall)
}
