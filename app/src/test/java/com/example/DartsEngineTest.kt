package com.example

import com.example.data.Player
import com.example.engine.CheckoutCalculator
import com.example.engine.DartsEngine
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test

class DartsEngineTest {

    @Test
    fun testCheckoutSuggestions() {
        assertEquals("T20 • T20 • Bull", CheckoutCalculator.getSuggestedCheckout(170, true))
        assertEquals("T20 • D20", CheckoutCalculator.getSuggestedCheckout(100, true))
        assertEquals("D20", CheckoutCalculator.getSuggestedCheckout(40, true))
        assertEquals("D16", CheckoutCalculator.getSuggestedCheckout(32, true))
    }

    @Test
    fun testEvaluateTurnNormal() {
        val rules = GameRules(gameType = GameType.GAME_501, doubleIn = false, doubleOut = true)
        val darts = listOf(
            DartThrow(20, Multiplier.TRIPLE), // 60
            DartThrow(20, Multiplier.TRIPLE), // 60
            DartThrow(20, Multiplier.TRIPLE)  // 60 = 180 total
        )

        val turn = DartsEngine.evaluateTurn(
            playerId = 1L,
            turnIndex = 1,
            scoreBefore = 501,
            darts = darts,
            rules = rules,
            hasDoubleIn = true
        )

        assertFalse(turn.isBust)
        assertEquals(180, turn.totalPoints)
        assertEquals(321, turn.scoreAfter)
        assertFalse(turn.isWinningTurn)
    }

    @Test
    fun testEvaluateTurnDoubleOutWinning() {
        val rules = GameRules(gameType = GameType.GAME_301, doubleIn = false, doubleOut = true)
        val darts = listOf(
            DartThrow(16, Multiplier.DOUBLE) // D16 = 32 points
        )

        val turn = DartsEngine.evaluateTurn(
            playerId = 1L,
            turnIndex = 5,
            scoreBefore = 32,
            darts = darts,
            rules = rules,
            hasDoubleIn = true
        )

        assertFalse(turn.isBust)
        assertEquals(0, turn.scoreAfter)
        assertTrue(turn.isWinningTurn)
    }

    @Test
    fun testEvaluateTurnBustOnSingleOutWhenDoubleRequired() {
        val rules = GameRules(gameType = GameType.GAME_301, doubleIn = false, doubleOut = true)
        val darts = listOf(
            DartThrow(20, Multiplier.SINGLE) // 20 pts from score of 20 = 0 but NOT a double
        )

        val turn = DartsEngine.evaluateTurn(
            playerId = 1L,
            turnIndex = 5,
            scoreBefore = 20,
            darts = darts,
            rules = rules,
            hasDoubleIn = true
        )

        assertTrue(turn.isBust)
        assertEquals(20, turn.scoreAfter) // Reverted to score before
        assertFalse(turn.isWinningTurn)
    }

    @Test
    fun testEvaluateTurnBustOnLeavingOneWithDoubleOut() {
        val rules = GameRules(gameType = GameType.GAME_301, doubleIn = false, doubleOut = true)
        val darts = listOf(
            DartThrow(19, Multiplier.SINGLE) // 19 pts from 20 = 1 remaining (impossible to double out)
        )

        val turn = DartsEngine.evaluateTurn(
            playerId = 1L,
            turnIndex = 5,
            scoreBefore = 20,
            darts = darts,
            rules = rules,
            hasDoubleIn = true
        )

        assertTrue(turn.isBust)
        assertEquals(20, turn.scoreAfter)
    }

    @Test
    fun testRoundRobinSchedule() {
        val players = listOf(
            Player(id = 1L, name = "P1"),
            Player(id = 2L, name = "P2"),
            Player(id = 3L, name = "P3")
        )
        val matches = DartsEngine.generateRoundRobinMatches(players)
        // 3 players -> 3 matches (P1 vs P2, P1 vs P3, P2 vs P3)
        assertEquals(3, matches.size)
    }

    @Test
    fun testEliminationBracket() {
        val players = listOf(
            Player(id = 1L, name = "P1"),
            Player(id = 2L, name = "P2"),
            Player(id = 3L, name = "P3"),
            Player(id = 4L, name = "P4")
        )
        val matches = DartsEngine.generateEliminationMatches(players)
        // 4 players -> 2 semifinals (Round 1) + 1 final (Round 2) = 3 matches
        assertEquals(3, matches.size)
        assertEquals(2, matches.count { it.round == 1 })
        assertEquals(1, matches.count { it.round == 2 })
    }
}
