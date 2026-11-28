package com.jesuslcorominas.wwii.mapeditor.model

import com.jesuslcorominas.wwii.core.map.CityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LevelOfDetailTest {
    @Test
    fun zoomingOutKeepsTheMostImportantNames() {
        assertEquals(CityKind.entries.toSet(), LevelOfDetail.forHexWidth(80.0).labelledCities)
        assertEquals(
            setOf(CityKind.NATIONAL_CAPITAL, CityKind.TERRITORY_CAPITAL),
            LevelOfDetail.forHexWidth(50.0).labelledCities,
        )
        assertEquals(setOf(CityKind.NATIONAL_CAPITAL), LevelOfDetail.forHexWidth(25.0).labelledCities)
        assertEquals(emptySet(), LevelOfDetail.forHexWidth(10.0).labelledCities)
    }

    @Test
    fun smallSymbolsDisappearWhenHexesAreTiny() {
        assertTrue(LevelOfDetail.forHexWidth(20.0).smallSymbols)
        assertTrue(LevelOfDetail.forHexWidth(20.0).plainCityDots)
        assertFalse(LevelOfDetail.forHexWidth(10.0).smallSymbols)
        assertFalse(LevelOfDetail.forHexWidth(10.0).plainCityDots)
    }

    @Test
    fun overlappingLabelsGiveWayToCapitals() {
        val city = LabelBox(0f, 0f, 50f, 10f, CityKind.CITY)
        val capital = LabelBox(40f, 5f, 50f, 10f, CityKind.NATIONAL_CAPITAL)
        val apart = LabelBox(200f, 0f, 50f, 10f, CityKind.CITY)
        val colony = LabelBox(80f, 8f, 30f, 10f, CityKind.TERRITORY_CAPITAL)
        // The plain city overlaps the capital and goes; the colony capital overlaps the national one too.
        assertEquals(listOf(1, 2), placeLabels(listOf(city, capital, apart, colony)))
    }

    @Test
    fun labelsThatOnlyTouchDoNotOverlap() {
        val a = LabelBox(0f, 0f, 50f, 10f, CityKind.CITY)
        val b = LabelBox(50f, 0f, 50f, 10f, CityKind.CITY)
        assertEquals(listOf(0, 1), placeLabels(listOf(a, b)))
    }

    @Test
    fun labelsGrowWithTheZoomBetweenALegibleMinimumAndACap() {
        assertEquals(LevelOfDetail.MIN_LABEL_SP, LevelOfDetail.labelSize(25.0))
        assertEquals(20f, LevelOfDetail.labelSize(100.0))
        assertEquals(LevelOfDetail.MAX_LABEL_SP, LevelOfDetail.labelSize(600.0))
    }

    @Test
    fun theGridFadesWhenZoomingOut() {
        assertEquals(1f, LevelOfDetail.gridAlpha(LevelOfDetail.FULL_GRID_PX))
        assertEquals(1f, LevelOfDetail.gridAlpha(200.0))
        assertEquals(0.5f, LevelOfDetail.gridAlpha(34.0))
        assertEquals(LevelOfDetail.MIN_GRID_ALPHA, LevelOfDetail.gridAlpha(9.0))
    }
}
