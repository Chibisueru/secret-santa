package io.github.chibisueru.secretsanta.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SecretSantaDrawTest {

    private fun people(n: Int) = (1..n).map { DrawPerson(it.toLong(), "P$it") }

    private fun assertValid(
        people: List<DrawPerson>,
        rules: List<DrawRule>,
        result: DrawResult,
    ) {
        assertTrue("Expected success but got $result", result is DrawResult.Success)
        val a = (result as DrawResult.Success).assignments
        val ids = people.map { it.id }.toSet()
        assertEquals(ids, a.keys)
        assertEquals(ids, a.values.toSet())
        a.forEach { (giver, receiver) ->
            assertFalse("$giver drew themselves", giver == receiver)
            assertFalse("$giver -> $receiver breaks a rule", DrawRule(giver, receiver) in rules)
        }
    }

    @Test
    fun tooFewPeopleFails() {
        assertTrue(SecretSantaDraw.draw(people(2), emptyList()) is DrawResult.Failure)
    }

    @Test
    fun simpleDrawIsValid() {
        val p = people(10)
        repeat(200) { seed ->
            assertValid(p, emptyList(), SecretSantaDraw.draw(p, emptyList(), Random(seed)))
        }
    }

    @Test
    fun respectsCouplesRules() {
        val p = people(6)
        val rules = listOf(1L to 2L, 3L to 4L, 5L to 6L).flatMap { (a, b) ->
            listOf(DrawRule(a, b), DrawRule(b, a))
        }
        repeat(200) { seed -> assertValid(p, rules, SecretSantaDraw.draw(p, rules, Random(seed))) }
    }

    @Test
    fun heavilyConstrainedStillSolvedByBacktracking() {
        // Each person may only draw the next one: exactly one valid cycle.
        val p = people(12)
        val rules = p.flatMap { giver ->
            p.filter { it.id != giver.id && it.id != giver.id % 12 + 1 }
                .map { DrawRule(giver.id, it.id) }
        }
        val result = SecretSantaDraw.draw(p, rules, Random(1))
        assertValid(p, rules, result)
    }

    @Test
    fun personWithNoOptionsFailsWithName() {
        val p = people(3)
        val rules = listOf(DrawRule(1, 2), DrawRule(1, 3))
        val result = SecretSantaDraw.draw(p, rules)
        assertTrue(result is DrawResult.Failure)
        assertTrue((result as DrawResult.Failure).reason.contains("P1"))
    }

    @Test
    fun impossibleCombinationFails() {
        // Nobody is allowed to draw P1.
        val p = people(3)
        val rules = listOf(DrawRule(1, 2), DrawRule(2, 1), DrawRule(3, 1))
        assertTrue(SecretSantaDraw.draw(p, rules) is DrawResult.Failure)
    }
}
