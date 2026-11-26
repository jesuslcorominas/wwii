package com.jesuslcorominas.wwii.mapeditor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.Strait
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.mapeditor.model.Calibration
import com.jesuslcorominas.wwii.mapeditor.model.MapFiles
import com.jesuslcorominas.wwii.mapeditor.model.hexAt
import com.jesuslcorominas.wwii.mapeditor.ui.EditorState
import com.jesuslcorominas.wwii.mapeditor.ui.Tool
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorStateTest {
    private val testMapText = EditorStateTest::class.java.getResource("/test-map.json")!!.readText()

    private fun openTestMap(): Pair<EditorState, File> {
        val file = File(createTempDirectory("mapeditor").toFile(), "map.json").apply { writeText(testMapText) }
        val state = EditorState()
        state.canvasSize = Size(1000f, 800f)
        state.open(file)
        return state to file
    }

    private fun EditorState.screenOf(coord: HexCoord): Offset {
        val p = calibration.layout.center(coord)
        return Offset((p.x * scale).toFloat(), (p.y * scale).toFloat()) + offset
    }

    private fun EditorState.click(coord: HexCoord) {
        startStroke(screenOf(coord))
        endStroke()
    }

    @Test
    fun aStraitIsOneUndoStep() {
        val (state, _) = openTestMap()
        val before = state.draft
        state.selectTool(Tool.STRAIT)
        state.click(HexCoord(1, 2))
        state.click(HexCoord(2, 1))
        state.click(HexCoord(0, 3))
        assertEquals(Strait(HexCoord(2, 1), HexCoord(0, 3)), state.draft.hexAt(HexCoord(1, 2))!!.strait)
        state.undo()
        assertEquals(before, state.draft)
        assertFalse(state.canUndo)
    }

    @Test
    fun aStraitNeedsTwoDifferentShores() {
        val (state, _) = openTestMap()
        state.selectTool(Tool.STRAIT)
        state.click(HexCoord(1, 2))
        state.click(HexCoord(2, 1))
        state.click(HexCoord(2, 1))
        assertNull(state.draft.hexAt(HexCoord(1, 2))!!.strait)
    }

    @Test
    fun aFastDragPaintsEveryHexInBetween() {
        val (state, _) = openTestMap()
        state.selectTool(Tool.SEA)
        state.startStroke(state.screenOf(HexCoord(0, 5)))
        state.strokeAt(state.screenOf(HexCoord(4, 5)))
        state.endStroke()
        (0..4).forEach { q -> assertEquals(Terrain.SEA, state.draft.hexAt(HexCoord(q, 5))?.terrain, "($q,5)") }
        state.undo()
        assertNull(state.draft.hexAt(HexCoord(2, 5)))
    }

    @Test
    fun savingToAnUnwritablePlaceKeepsTheChanges() {
        val (state, _) = openTestMap()
        state.selectTool(Tool.SEA)
        state.click(HexCoord(0, 5))
        val missingDir = File(createTempDirectory("mapeditor").toFile(), "missing/map.json")
        assertFalse(state.save(missingDir))
        assertTrue(state.dirty)
        assertTrue("No se pudo guardar" in state.status)
    }

    @Test
    fun openingAMissingFileReportsItWithoutCrashing() {
        val state = EditorState()
        state.open(File(createTempDirectory("mapeditor").toFile(), "nope.json"))
        assertTrue("No se pudo leer" in state.status)
    }

    @Test
    fun aBrokenCalibrationIsIgnoredWithAWarning() {
        val file = File(createTempDirectory("mapeditor").toFile(), "map.json").apply { writeText(testMapText) }
        Calibration.sidecarOf(file).writeText("""{"hexWidth": 0}""")
        val state = EditorState()
        state.open(file)
        assertEquals(Calibration(), state.calibration)
        assertTrue("calibración ignorada" in state.status)
        assertFalse(state.dirty)
    }

    @Test
    fun calibrationChangesAreSavedNextToTheMap() {
        val (state, file) = openTestMap()
        state.calibration = state.calibration.copy(originX = 123.0)
        assertTrue(state.dirty)
        assertTrue(state.save())
        assertEquals(testMapText, file.readText())
        assertEquals(123.0, Calibration.decode(Calibration.sidecarOf(file).readText()).originX)
        assertEquals(MapFiles.read(file), state.draft)
    }
}
