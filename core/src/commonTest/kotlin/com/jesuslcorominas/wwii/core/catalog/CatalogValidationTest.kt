package com.jesuslcorominas.wwii.core.catalog

import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogValidationTest {
    private val aa = NationId("AA")
    private val bb = NationId("BB")
    private val major = Nation(aa, "Alfa", NationRole.MAJOR, Side.AXIS)
    private val neutral = Nation(bb, "Bravo", NationRole.NEUTRAL, Side.NEUTRAL)
    private val aaLand = CounterEntry(aa, CounterType.LAND, 2)

    private fun issuesOf(
        nations: List<Nation>,
        counters: List<CounterEntry>,
    ): List<CatalogIssue> =
        assertFailsWith<InvalidCatalogException> { NationCatalog.build("x", "X", nations, counters) }.issues

    @Test
    fun noNations() {
        assertEquals(listOf<CatalogIssue>(CatalogIssue.NoNations), issuesOf(emptyList(), emptyList()))
    }

    @Test
    fun duplicateNation() {
        assertEquals(
            listOf<CatalogIssue>(CatalogIssue.DuplicateNation(aa)),
            issuesOf(listOf(major, major.copy(name = "Otra")), listOf(aaLand)),
        )
    }

    @Test
    fun countersOfUnknownNation() {
        val zz = NationId("ZZ")
        assertEquals(
            listOf<CatalogIssue>(CatalogIssue.UnknownNation(zz)),
            issuesOf(listOf(major), listOf(aaLand, CounterEntry(zz, CounterType.AIR, 1))),
        )
    }

    @Test
    fun duplicateCounterEntry() {
        assertEquals(
            listOf<CatalogIssue>(CatalogIssue.DuplicateCounterEntry(aa, CounterType.LAND)),
            issuesOf(listOf(major), listOf(aaLand, aaLand.copy(count = 5))),
        )
    }

    @Test
    fun nonPositiveCount() {
        assertEquals(
            listOf<CatalogIssue>(
                CatalogIssue.NonPositiveCount(aa, CounterType.NAVAL, 0),
                CatalogIssue.NonPositiveCount(aa, CounterType.AIR, -1),
            ),
            issuesOf(
                listOf(major),
                listOf(aaLand, CounterEntry(aa, CounterType.NAVAL, 0), CounterEntry(aa, CounterType.AIR, -1)),
            ),
        )
    }

    @Test
    fun neutralRoleMustStartNeutralAndOnlyNeutralsCan() {
        assertEquals(
            listOf<CatalogIssue>(
                CatalogIssue.RoleSideMismatch(aa, NationRole.MAJOR, Side.NEUTRAL),
                CatalogIssue.RoleSideMismatch(bb, NationRole.NEUTRAL, Side.USSR),
            ),
            issuesOf(
                listOf(major.copy(initialSide = Side.NEUTRAL), neutral.copy(initialSide = Side.USSR)),
                listOf(aaLand),
            ),
        )
    }

    @Test
    fun neutralCannotHaveLateEntry() {
        assertEquals(
            listOf<CatalogIssue>(CatalogIssue.LateEntryNeutral(bb)),
            issuesOf(listOf(major, neutral.copy(lateEntry = true)), listOf(aaLand)),
        )
    }

    @Test
    fun majorNeedsCounters() {
        assertEquals(
            listOf<CatalogIssue>(CatalogIssue.MajorWithoutCounters(aa)),
            issuesOf(listOf(major, neutral), emptyList()),
        )
    }

    @Test
    fun minorsAndNeutralsMayHaveNoCounters() {
        val minor = Nation(NationId("CC"), "Charlie", NationRole.MINOR, Side.WESTERN_ALLIES)
        val catalog = NationCatalog.build("x", "X", listOf(major, neutral, minor), listOf(aaLand))
        assertEquals(2, catalog.totalCounters)
    }

    @Test
    fun reportsEveryProblemAtOnce() {
        val zz = NationId("ZZ")
        val e =
            assertFailsWith<InvalidCatalogException> {
                NationCatalog.build(
                    "x",
                    "X",
                    listOf(major, major, neutral.copy(initialSide = Side.AXIS)),
                    listOf(CounterEntry(zz, CounterType.LAND, 0)),
                )
            }
        assertEquals(
            listOf(
                CatalogIssue.DuplicateNation(aa),
                CatalogIssue.RoleSideMismatch(bb, NationRole.NEUTRAL, Side.AXIS),
                CatalogIssue.UnknownNation(zz),
                CatalogIssue.NonPositiveCount(zz, CounterType.LAND, 0),
                CatalogIssue.MajorWithoutCounters(aa),
            ),
            e.issues,
        )
        assertTrue("5 issue(s)" in e.message!!)
        assertTrue("Nation AA is declared more than once" in e.message!!)
    }
}
