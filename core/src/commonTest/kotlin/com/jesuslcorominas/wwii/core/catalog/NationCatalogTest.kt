package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NationCatalogTest {
    private val aa = NationId("AA")
    private val bb = NationId("BB")
    private val cc = NationId("CC")

    @Test
    fun answersNationsAndSides() {
        val catalog = TestCatalogs.valid()
        assertEquals(listOf(aa, bb, cc), catalog.nations.map { it.id })
        assertEquals(Side.AXIS, catalog.initialSideOf(aa))
        assertEquals(Side.WESTERN_ALLIES, catalog.sideOf(bb))
        assertEquals(listOf(bb, cc), catalog.nationsOf(Side.WESTERN_ALLIES).map { it.id })
        assertEquals(emptyList(), catalog.nationsOf(Side.USSR))
        assertTrue(catalog.nation(cc)!!.lateEntry)
        assertTrue(catalog.nation(cc)!!.offMap)
        assertFalse(catalog.nation(aa)!!.lateEntry)
        assertNull(catalog.nation(NationId("ZZ")))
        assertFalse(NationId("ZZ") in catalog)
        assertFailsWith<IllegalArgumentException> { catalog.initialSideOf(NationId("ZZ")) }
    }

    @Test
    fun countsCounters() {
        val catalog = TestCatalogs.valid()
        assertEquals(9, catalog.totalCounters)
        assertEquals(3, catalog.countOf(aa, CounterType.LAND))
        assertEquals(0, catalog.countOf(aa, CounterType.NAVAL))
        assertEquals(1, catalog.countOf(aa, CounterType.AIR))
        assertEquals(3, catalog.countersOf(cc).size)
        assertEquals(0, catalog.countOf(NationId("ZZ"), CounterType.LAND))
    }

    @Test
    fun counterIdsAreDeterministicAndUnique() {
        val catalog = TestCatalogs.valid()
        assertEquals(
            listOf("AA-L01", "AA-L02", "AA-L03", "AA-A01", "BB-L01", "BB-L02", "CC-L01", "CC-N01", "CC-N02"),
            catalog.counters.map { it.id.value },
        )
        assertEquals(
            catalog.counters.size,
            catalog.counters
                .map { it.id }
                .toSet()
                .size,
        )
        assertEquals(catalog.counters, TestCatalogs.valid().counters)
        assertEquals("DEU-N12", CounterId.of(NationId("DEU"), CounterType.NAVAL, 12).value)
    }

    @Test
    fun buildAcceptsAValidCatalog() {
        val catalog =
            NationCatalog.build(
                id = "x",
                name = "X",
                nations = listOf(Nation(aa, "Alfa", NationRole.MAJOR, Side.USSR)),
                counters = listOf(CounterEntry(aa, CounterType.LAND, 1)),
            )
        assertEquals(1, catalog.totalCounters)
        assertEquals(listOf(Counter(CounterId("AA-L01"), aa, CounterType.LAND)), catalog.counters)
    }
}
