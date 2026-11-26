package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.City
import com.jesuslcorominas.wwii.core.map.CityKind
import com.jesuslcorominas.wwii.core.map.HexCoord
import com.jesuslcorominas.wwii.core.map.InvalidMapException
import com.jesuslcorominas.wwii.core.map.MapDraft
import com.jesuslcorominas.wwii.core.map.MapIssue
import com.jesuslcorominas.wwii.core.map.Terrain
import com.jesuslcorominas.wwii.core.map.Territory
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.map.TerritoryKind
import com.jesuslcorominas.wwii.core.map.json.MapJson
import com.jesuslcorominas.wwii.core.nation.NationId
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditorSupportTest {
    private val testMapText = javaClass.getResource("/test-map.json")!!.readText()

    private fun tempFile(name: String): File = File(createTempDirectory(), name)

    private fun createTempDirectory(): File =
        kotlin.io.path
            .createTempDirectory("mapeditor")
            .toFile()
            .apply { deleteOnExit() }

    @Test
    fun openingAndSavingAnUntouchedMapGivesTheSameText() {
        val file = tempFile("map.json").apply { writeText(testMapText) }
        val draft = MapFiles.read(file)
        assertEquals(emptyList(), draft.issues())
        MapFiles.write(file, draft)
        assertEquals(testMapText, file.readText())
    }

    @Test
    fun aPaintedMapSavedAndReopenedIsEqual() {
        val bb = TerritoryId("BB")
        val painted =
            MapFiles
                .read(tempFile("a.json").apply { writeText(testMapText) })
                .setTerrain(HexCoord(6, 0), Terrain.SEA)
                .paintTerritory(HexCoord(5, 0), bb)
                .setCity(HexCoord(5, 0), City("New Town", CityKind.CITY))
                .toggleRiver(HexCoord(4, 0), HexCoord(5, 0))
                .toggleMountain(HexCoord(3, 0))
                .setStrait(HexCoord(1, 2), HexCoord(0, 3), HexCoord(1, 1))
                .addTerritory(Territory(TerritoryId("CC"), NationId("CC"), TerritoryKind.POSSESSION, "Charlie"))
        val file = tempFile("b.json")
        MapFiles.write(file, painted)
        val reopened = MapFiles.read(file)
        assertEquals(painted, reopened)
        assertEquals(painted.issues(), reopened.issues())
        assertEquals(MapFiles.text(painted), MapFiles.text(reopened))
    }

    @Test
    fun anInvalidMapCanBeSavedAndReopened() {
        val wip =
            MapDraft("wip", "Work in progress")
                .setTerrain(HexCoord(0, 0), Terrain.LAND)
                .addTerritory(Territory(TerritoryId("AA"), NationId("AA"), TerritoryKind.HOMELAND, "Alfa"))
        assertTrue(wip.issues().isNotEmpty())
        val file = tempFile("wip.json")
        MapFiles.write(file, wip)
        assertEquals(wip, MapFiles.read(file))
        assertTrue("\"formatVersion\": ${MapJson.FORMAT_VERSION}" in file.readText())
    }

    @Test
    fun aFileThatIsNotAMapIsRejected() {
        val file = tempFile("bad.json").apply { writeText("{\"formatVersion\": 1, \"id\": \"x\"") }
        val e = assertFailsWith<InvalidMapException> { MapFiles.read(file) }
        assertTrue(e.issues.single() is MapIssue.MalformedDocument)
    }

    @Test
    fun historyUndoesAndRedoes() {
        val history = History<Int>()
        history.record(1)
        history.record(2)
        assertEquals(2, history.undo(3))
        assertEquals(1, history.undo(2))
        assertNull(history.undo(1))
        assertEquals(2, history.redo(1))
        history.record(2)
        assertNull(history.redo(5))
    }

    @Test
    fun historyKeepsOnlyTheLastSteps() {
        val history = History<Int>(limit = 2)
        (1..5).forEach { history.record(it) }
        assertEquals(5, history.undo(6))
        assertEquals(4, history.undo(5))
        assertNull(history.undo(4))
    }

    @Test
    fun calibrationLivesInASidecarFile() {
        val map = tempFile("europe.json")
        val sidecar = Calibration.sidecarOf(map)
        assertEquals("europe.editor.json", sidecar.name)
        assertEquals(map.parentFile, sidecar.parentFile)
        val calibration = Calibration(imagePath = "/tmp/ref.png", originX = 12.5, hexWidth = 31.0, hexHeight = 36.0)
        assertEquals(calibration, Calibration.decode(calibration.encode()))
        assertEquals(31.0, calibration.layout.hexWidth)
    }

    @Test
    fun nationColoursAreStableAndDistinguishKinds() {
        val fr = NationId("FR")
        assertEquals(NationColors.rgb(fr, TerritoryKind.HOMELAND), NationColors.rgb(fr, TerritoryKind.HOMELAND))
        assertNotEquals(NationColors.rgb(fr, TerritoryKind.HOMELAND), NationColors.rgb(fr, TerritoryKind.COLONY))
        assertNotEquals(
            NationColors.rgb(fr, TerritoryKind.HOMELAND),
            NationColors.rgb(NationId("DE"), TerritoryKind.HOMELAND),
        )
    }

    @Test
    fun issuesPointAtTheirHexes() {
        val aa = TerritoryId("AA")
        val draft =
            MapDraft("m", "m")
                .addTerritory(Territory(aa, NationId("AA"), TerritoryKind.HOMELAND, "Alfa"))
                .paintTerritory(HexCoord(0, 0), aa)
                .paintTerritory(HexCoord(3, 0), aa)
                .setTerrain(HexCoord(5, 0), Terrain.LAND)
        val located = draft.issues().associateWith { IssueLocator.hexesOf(it, draft) }
        assertEquals(listOf(HexCoord(5, 0)), located[MapIssue.LandWithoutTerritory(HexCoord(5, 0))])
        assertEquals(listOf(HexCoord(0, 0), HexCoord(3, 0)), located[MapIssue.DisconnectedTerritory(aa, 2)])
    }
}
