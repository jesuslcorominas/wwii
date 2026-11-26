package com.jesuslcorominas.wwii.core.setup

import com.jesuslcorominas.wwii.core.catalog.CounterType
import com.jesuslcorominas.wwii.core.catalog.NationCatalog
import com.jesuslcorominas.wwii.core.catalog.TestCatalogs
import com.jesuslcorominas.wwii.core.catalog.json.CatalogJson
import com.jesuslcorominas.wwii.core.map.TerritoryId
import com.jesuslcorominas.wwii.core.nation.NationId
import com.jesuslcorominas.wwii.core.nation.Side
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SetupValidationTest {
    private val base = TestSetups.valid()
    private val catalog = TestCatalogs.valid()
    private val aa = NationId("AA")
    private val bb = NationId("BB")
    private val cc = NationId("CC")
    private val zz = NationId("ZZ")
    private val territoryAa = TerritoryId("AA")

    private fun issuesOf(
        catalogId: String = base.catalogId,
        turns: Int = base.turns,
        deployment: List<Deployment> = base.deployment,
        reinforcements: List<Reinforcement> = base.reinforcements,
        specials: List<SpecialReinforcement> = base.specials,
        against: NationCatalog = catalog,
    ): List<SetupIssue> =
        assertFailsWith<InvalidSetupException> {
            GameSetup.build("x", "X", catalogId, turns, deployment, reinforcements, specials, against)
        }.issues

    private fun special(
        id: String,
        effect: SpecialEffect,
    ) = SpecialReinforcement(SpecialId(id), id, effect)

    @Test
    fun catalogMismatch() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.CatalogMismatch("other", "test-small")),
            issuesOf(catalogId = "other"),
        )
    }

    @Test
    fun nonPositiveTurnsSkipsTurnRangeChecks() {
        assertEquals(listOf<SetupIssue>(SetupIssue.NonPositiveTurns(0)), issuesOf(turns = 0))
    }

    @Test
    fun turnOutOfRange() {
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.TurnOutOfRange("reinforcement", 5, 4),
                SetupIssue.TurnOutOfRange("special late", 0, 4),
            ),
            issuesOf(
                reinforcements = listOf(Reinforcement(5, cc, CounterCounts(land = 1))),
                specials = listOf(special("late", SpecialEffect.DeferredEntry(listOf(DeferredNation(bb, 0))))),
            ),
        )
    }

    @Test
    fun unknownNationIsReportedOnce() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.UnknownNation(zz)),
            issuesOf(
                deployment = base.deployment + Deployment(zz, territoryAa, CounterCounts(land = 1)),
                reinforcements = base.reinforcements + Reinforcement(2, zz, CounterCounts(land = 1)),
            ),
        )
    }

    @Test
    fun invalidCounts() {
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.InvalidCounts("deployment", aa, CounterCounts.NONE),
                SetupIssue.InvalidCounts("reinforcement", cc, CounterCounts(land = 2, naval = -1)),
            ),
            issuesOf(
                deployment = base.deployment + Deployment(aa, TerritoryId("AX"), CounterCounts.NONE),
                reinforcements = listOf(Reinforcement(3, cc, CounterCounts(land = 2, naval = -1))),
            ).filterIsInstance<SetupIssue.InvalidCounts>(),
        )
    }

    @Test
    fun duplicateEntries() {
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.DuplicateDeployment(bb, TerritoryId("BB")),
                SetupIssue.DuplicateReinforcement(3, cc),
                SetupIssue.DuplicateSpecial(SpecialId("key-cities")),
            ),
            issuesOf(
                deployment =
                    base.deployment.map {
                        if (it.territory ==
                            TerritoryId("BC")
                        ) {
                            it.copy(territory = TerritoryId("BB"))
                        } else {
                            it
                        }
                    },
                reinforcements =
                    listOf(
                        Reinforcement(3, cc, CounterCounts(land = 1)),
                        Reinforcement(3, cc, CounterCounts(naval = 1)),
                    ),
                specials = base.specials + base.special(SpecialId("key-cities"))!!,
            ).filter { it !is SetupIssue.ExceedsCatalog },
        )
    }

    @Test
    fun lateEntryAndOffMapNationsCannotDeploy() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.NotDeployable(cc)),
            issuesOf(
                deployment = base.deployment + Deployment(cc, territoryAa, CounterCounts(land = 1)),
                reinforcements = emptyList(),
            ),
        )
    }

    @Test
    fun neutralNationsGetNoBaseReinforcements() {
        val withNeutral =
            CatalogJson.decode(
                TestCatalogs.VALID
                    .replace(
                        """"offMap": true }""",
                        """"offMap": true },
                    { "id": "DD", "name": "Delta", "role": "neutral", "side": "neutral" }""",
                    ).replace(
                        """{ "nation": "CC", "type": "land", "count": 1 }""",
                        """{ "nation": "CC", "type": "land", "count": 1 },
                    { "nation": "DD", "type": "land", "count": 1 }""",
                    ),
            )
        val dd = NationId("DD")
        assertEquals(
            listOf<SetupIssue>(SetupIssue.NeutralReinforcement(dd)),
            issuesOf(
                reinforcements = base.reinforcements + Reinforcement(2, dd, CounterCounts(land = 1)),
                against = withNeutral,
            ),
        )
    }

    @Test
    fun exceedsCatalogInTheWorstCase() {
        // AA has 3 land: 2 deployed + 1 key-city bonus. One more land reinforcement is one too many.
        assertEquals(
            listOf<SetupIssue>(SetupIssue.ExceedsCatalog(aa, CounterType.LAND, 4L, 3)),
            issuesOf(reinforcements = base.reinforcements + Reinforcement(2, aa, CounterCounts(land = 1))),
        )
    }

    @Test
    fun exceedsCatalogForEveryType() {
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.ExceedsCatalog(cc, CounterType.NAVAL, 3L, 2),
                SetupIssue.ExceedsCatalog(cc, CounterType.AIR, 1L, 0),
            ),
            issuesOf(reinforcements = listOf(Reinforcement(3, cc, CounterCounts(land = 1, naval = 3, air = 1)))),
        )
    }

    @Test
    fun emptySpecials() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.EmptySpecial(SpecialId("a")), SetupIssue.EmptySpecial(SpecialId("b"))),
            issuesOf(
                specials =
                    listOf(
                        special("a", SpecialEffect.DeferredEntry(emptyList())),
                        special("b", SpecialEffect.KeyCityBonus(emptyList())),
                    ),
            ),
        )
    }

    @Test
    fun deferredNationMustDeploy() {
        assertEquals(
            listOf<SetupIssue>(SetupIssue.DeferredWithoutDeployment(SpecialId("late"), cc)),
            issuesOf(specials = listOf(special("late", SpecialEffect.DeferredEntry(listOf(DeferredNation(cc, 2)))))),
        )
    }

    @Test
    fun volunteersCannotExceedTheDeployment() {
        val volunteers = SpecialEffect.Volunteers(bb, Side.AXIS, 3, CounterCounts(land = 3))
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.WithheldExceedsDeployment(
                    bb,
                    CounterCounts(land = 3),
                    CounterCounts(land = 2),
                ),
            ),
            issuesOf(specials = listOf(special("v", volunteers))),
        )
    }

    @Test
    fun volunteersNeedValidCountsAndTurn() {
        val volunteers = SpecialEffect.Volunteers(bb, Side.AXIS, 9, CounterCounts.NONE)
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.TurnOutOfRange("special v", 9, 4),
                SetupIssue.InvalidCounts("special v", bb, CounterCounts.NONE),
            ),
            issuesOf(specials = listOf(special("v", volunteers))),
        )
    }

    @Test
    fun invalidSides() {
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.InvalidSide(SpecialId("v"), bb, Side.WESTERN_ALLIES),
                SetupIssue.InvalidSide(SpecialId("k"), aa, Side.USSR),
                SetupIssue.InvalidSide(SpecialId("j"), bb, Side.NEUTRAL),
            ),
            issuesOf(
                specials =
                    listOf(
                        special("v", SpecialEffect.Volunteers(bb, Side.WESTERN_ALLIES, 2, CounterCounts(land = 1))),
                        special(
                            "k",
                            SpecialEffect.KeyCityBonus(
                                listOf(
                                    CityBonus(
                                        SpecialCondition.ControlsCity(Side.USSR, "X"),
                                        aa,
                                        CounterCounts(land = 1),
                                    ),
                                ),
                            ),
                        ),
                        special(
                            "j",
                            SpecialEffect.JoinSide(bb, Side.NEUTRAL, SpecialCondition.ControlsCity(Side.AXIS, "X")),
                        ),
                    ),
            ),
        )
    }

    @Test
    fun reportsEveryIssueAtOnce() {
        val issues =
            issuesOf(
                catalogId = "other",
                reinforcements = listOf(Reinforcement(7, zz, CounterCounts(land = 1))),
            )
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.CatalogMismatch("other", "test-small"),
                SetupIssue.UnknownNation(zz),
                SetupIssue.TurnOutOfRange("reinforcement", 7, 4),
            ),
            issues,
        )
    }

    @Test
    fun volunteersAddUpAcrossSpecials() {
        // BB deploys 2 land: two specials withholding 2 each need 4.
        val twice = SpecialEffect.Volunteers(bb, Side.AXIS, 3, CounterCounts(land = 2))
        assertEquals(
            listOf<SetupIssue>(
                SetupIssue.WithheldExceedsDeployment(bb, CounterCounts(land = 4), CounterCounts(land = 2)),
            ),
            issuesOf(specials = listOf(special("v1", twice), special("v2", twice))),
        )
    }

    @Test
    fun volunteersOfAnUnknownNationOnlyReportTheNation() {
        val volunteers = SpecialEffect.Volunteers(zz, Side.AXIS, 3, CounterCounts(land = 1))
        assertEquals(
            listOf<SetupIssue>(SetupIssue.UnknownNation(zz)),
            issuesOf(specials = listOf(special("v", volunteers))),
        )
    }

    @Test
    fun aNationIsDeferredOnlyOnce() {
        val inOne = SpecialEffect.DeferredEntry(listOf(DeferredNation(bb, 2), DeferredNation(bb, 3)))
        assertEquals(
            listOf<SetupIssue>(SetupIssue.DuplicateDeferral(bb)),
            issuesOf(specials = listOf(special("one", inOne))),
        )
        val single = SpecialEffect.DeferredEntry(listOf(DeferredNation(bb, 2)))
        assertEquals(
            listOf<SetupIssue>(SetupIssue.DuplicateDeferral(bb)),
            issuesOf(specials = listOf(special("a", single), special("b", single))),
        )
    }

    @Test
    fun joinSideConditionMustBeOfTheJoinedSide() {
        val join = SpecialEffect.JoinSide(bb, Side.AXIS, SpecialCondition.ControlsCity(Side.USSR, "x"))
        assertEquals(
            listOf<SetupIssue>(SetupIssue.InvalidSide(SpecialId("j"), bb, Side.USSR)),
            issuesOf(specials = listOf(special("j", join))),
        )
    }

    @Test
    fun hugeCountsDoNotOverflowTheCatalogCheck() {
        val huge = CounterCounts(land = Int.MAX_VALUE)
        val issues =
            issuesOf(
                deployment =
                    base.deployment +
                        Deployment(aa, TerritoryId("A1"), huge) +
                        Deployment(aa, TerritoryId("A2"), huge),
            )
        assertEquals(
            listOf<SetupIssue>(SetupIssue.ExceedsCatalog(aa, CounterType.LAND, 2L * Int.MAX_VALUE + 3, 3)),
            issues,
        )
    }
}
