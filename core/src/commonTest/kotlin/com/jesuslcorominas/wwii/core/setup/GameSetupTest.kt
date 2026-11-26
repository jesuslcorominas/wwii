package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameSetupTest {
    private val setup = TestSetups.valid()
    private val aa = NationId("AA")
    private val bb = NationId("BB")
    private val cc = NationId("CC")

    @Test
    fun loadsTheTestSetup() {
        assertEquals("test-small", setup.id)
        assertEquals("test-small", setup.catalogId)
        assertEquals(4, setup.turns)
        assertEquals(3, setup.deployment.size)
        assertEquals(1, setup.reinforcements.size)
        assertEquals(4, setup.specials.size)
    }

    @Test
    fun deploymentQueries() {
        assertEquals(listOf(TerritoryId("BB"), TerritoryId("BC")), setup.deploymentOf(bb).map { it.territory })
        assertEquals(CounterCounts(land = 2, air = 1), setup.deployedCountOf(aa))
        assertEquals(CounterCounts(land = 2), setup.deployedCountOf(bb))
        assertTrue(setup.deployedCountOf(cc).isEmpty)
    }

    @Test
    fun reinforcementQueries() {
        assertEquals(listOf(cc), setup.reinforcementsAt(3).map { it.nation })
        assertTrue(setup.reinforcementsAt(1).isEmpty())
        assertEquals(CounterCounts(land = 1, naval = 2), setup.reinforcementCountOf(cc))
        assertEquals(CounterCounts(land = 1, naval = 2), setup.baseTotalOf(cc))
        assertEquals(CounterCounts(land = 2, air = 1), setup.baseTotalOf(aa))
    }

    @Test
    fun specialsById() {
        val join = setup.special(SpecialId("bravo-joins-axis"))?.effect
        assertEquals(
            SpecialEffect.JoinSide(bb, Side.AXIS, SpecialCondition.ControlsCity(Side.AXIS, "Colony Town")),
            join,
        )
        assertEquals(
            SpecialEffect.DeferredEntry(listOf(DeferredNation(bb, 2))),
            setup.special(SpecialId("late-bravo"))?.effect,
        )
        assertNull(setup.special(SpecialId("nope")))
    }

    @Test
    fun counterCountsArithmetic() {
        val a = CounterCounts(1, 2, 3)
        assertEquals(CounterCounts(2, 4, 6), a + a)
        assertEquals(6, a.total)
        assertEquals(2, a[CounterType.NAVAL])
        assertEquals(CounterCounts(2, 2, 3), CounterCounts.sum(listOf(a, CounterCounts(land = 1))))
        assertTrue(CounterCounts.NONE.isEmpty)
        assertTrue(CounterCounts(air = -1).hasNegative)
        assertEquals("1L/2N/3A", a.toString())
    }
}
