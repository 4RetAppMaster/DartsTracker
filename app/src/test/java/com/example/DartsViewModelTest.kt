package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.DartThrow
import com.example.model.GameRules
import com.example.model.GameType
import com.example.model.Multiplier
import com.example.model.TournamentType
import com.example.viewmodel.DartsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DartsViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun versionName_isIncrementedTo2_3() {
        assertEquals("2.3", BuildConfig.VERSION_NAME)
    }

    @Test
    fun importPlayersJson_importsValidJsonWithBomAndReplaceAll() = runBlocking(Dispatchers.Default) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val db = AppDatabase.getDatabase(app)
        val viewModel = DartsViewModel(app)

        val jsonWithBom = "\uFEFF" + """
            [
              {"name": "Alice", "wins": 3, "losses": 1, "matchesPlayed": 4},
              {"name": "Bob", "wins": 1, "losses": 3, "matchesPlayed": 4}
            ]
        """.trimIndent()

        val result = viewModel.importPlayersJson(jsonWithBom, replaceAll = true)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())

        val stored = db.playerDao().getAllPlayersList()
        assertEquals(2, stored.size)
        assertEquals("Alice", stored[0].name)
        assertEquals(3, stored[0].wins)
        assertEquals("Bob", stored[1].name)
    }

    @Test
    fun tournamentMatchWin_returnsToMatchSelectorWithoutStartingNextMatch() = runBlocking(Dispatchers.Default) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DartsViewModel(app)

        val playersJson = """
            [
              {"name": "P1", "wins": 0, "losses": 0, "matchesPlayed": 0},
              {"name": "P2", "wins": 0, "losses": 0, "matchesPlayed": 0},
              {"name": "P3", "wins": 0, "losses": 0, "matchesPlayed": 0}
            ]
        """.trimIndent()
        val importRes = viewModel.importPlayersJson(playersJson, replaceAll = true)
        assertTrue(importRes.isSuccess)

        val players = withTimeout(5000) {
            viewModel.allPlayers.first { it.size == 3 }
        }
        val rules = GameRules(gameType = GameType.GAME_301, doubleIn = false, doubleOut = false)
        viewModel.startTournament(
            type = TournamentType.ROUND_ROBIN,
            rules = rules,
            playerIds = players.map { it.id }
        )

        val tournament = viewModel.tournamentState.value
        assertNotNull(tournament)
        val firstMatchId = tournament!!.currentMatchId
        assertNotNull(firstMatchId)

        viewModel.launchTournamentMatch(firstMatchId!!)
        val startedMatch = viewModel.activeMatch.value
        assertNotNull(startedMatch)

        // Score 301 quickly (T20, T20, T20 = 180 -> 121 remaining; then T20, T20, 1 = 121 -> 0 win)
        repeat(3) { viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE)) } // P1 turns over to P2
        repeat(3) { viewModel.addDartThrow(DartThrow(1, Multiplier.SINGLE)) }  // P2 turns over to P1
        viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE))
        viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE))
        viewModel.addDartThrow(DartThrow(1, Multiplier.SINGLE))

        val wonMatch = viewModel.activeMatch.value
        assertNotNull(wonMatch)
        assertTrue(wonMatch!!.isMatchOver)

        // Confirming win exits back to tournament match selector (activeMatch == null)
        viewModel.confirmMatchWinAndExit()
        assertNull(viewModel.activeMatch.value)

        val updatedTournament = viewModel.tournamentState.value
        assertNotNull(updatedTournament)
        assertFalse(updatedTournament!!.isFinished)
        assertTrue(updatedTournament.matches.first { it.id == firstMatchId }.isCompleted)
    }

    @Test
    fun multiSetAndLegMatch_clearsDartsBetweenLegsAndEndsWhenTargetSetsReached() = runBlocking(Dispatchers.Default) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = DartsViewModel(app)

        val playersJson = """
            [
              {"name": "P1", "wins": 0, "losses": 0, "matchesPlayed": 0},
              {"name": "P2", "wins": 0, "losses": 0, "matchesPlayed": 0}
            ]
        """.trimIndent()
        assertTrue(viewModel.importPlayersJson(playersJson, replaceAll = true).isSuccess)

        val players = withTimeout(5000) { viewModel.allPlayers.first { it.size == 2 } }
        val rules = GameRules(
            gameType = GameType.GAME_301,
            doubleIn = false,
            doubleOut = false,
            setsToWin = 2,
            legsToWinSet = 2
        )
        viewModel.startMatch(customPlayerIds = players.map { it.id }, rulesOverride = rules)

        fun play301WinForCurrentPlayer() {
            repeat(3) { viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE)) } // 180
            repeat(3) { viewModel.addDartThrow(DartThrow(1, Multiplier.SINGLE)) }  // opponent 3
            viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE))
            viewModel.addDartThrow(DartThrow(20, Multiplier.TRIPLE))
            viewModel.addDartThrow(DartThrow(1, Multiplier.SINGLE)) // 121 -> 0 win
        }

        // Leg 1 of Set 1 won by P1
        play301WinForCurrentPlayer()
        var m = viewModel.activeMatch.value!!
        assertFalse(m.isMatchOver)
        assertNotNull(m.pendingSubWin)
        assertFalse(m.pendingSubWin!!.isSetWin)
        viewModel.confirmSubWinAndAdvance()

        // New leg starts with empty turns / darts for all players while keeping P1's 1 leg win
        m = viewModel.activeMatch.value!!
        assertTrue(m.currentDarts.isEmpty())
        assertTrue(m.players.all { it.turns.isEmpty() })
        assertEquals(1, m.players[0].legsWon)
        assertEquals(0, m.players[0].setsWon)
        assertTrue(m.players[0].threeDartAverage > 0.0)

        // Leg 2 of Set 1 won by P2 (starter) -> Set 1 still not over (1-1 legs)
        play301WinForCurrentPlayer()
        m = viewModel.activeMatch.value!!
        assertFalse(m.isMatchOver)
        assertNotNull(m.pendingSubWin)
        assertFalse(m.pendingSubWin!!.isSetWin)
        viewModel.confirmSubWinAndAdvance()

        // Leg 3 of Set 1 won by P1 -> P1 wins Set 1 (2 legs won)
        play301WinForCurrentPlayer()
        m = viewModel.activeMatch.value!!
        assertFalse(m.isMatchOver)
        assertNotNull(m.pendingSubWin)
        assertTrue(m.pendingSubWin!!.isSetWin)
        viewModel.confirmSubWinAndAdvance()

        m = viewModel.activeMatch.value!!
        assertEquals(1, m.players[0].setsWon)
        assertEquals(0, m.players[0].legsWon)
        assertEquals(0, m.players[1].legsWon)
        assertTrue(m.players.all { it.turns.isEmpty() })

        // Set 2 Leg 1 won by P2
        play301WinForCurrentPlayer()
        viewModel.confirmSubWinAndAdvance()
        // Set 2 Leg 2 won by P1
        play301WinForCurrentPlayer()
        viewModel.confirmSubWinAndAdvance()
        // Set 2 Leg 3 won by P2 -> P2 wins Set 2 (now 1-1 sets)
        play301WinForCurrentPlayer()
        m = viewModel.activeMatch.value!!
        assertFalse(m.isMatchOver)
        assertTrue(m.pendingSubWin!!.isSetWin)
        viewModel.confirmSubWinAndAdvance()

        // Set 3 Leg 1 won by P1
        play301WinForCurrentPlayer()
        viewModel.confirmSubWinAndAdvance()
        // Set 3 Leg 2 won by P2
        play301WinForCurrentPlayer()
        viewModel.confirmSubWinAndAdvance()
        // Set 3 Leg 3 won by P1 -> P1 reaches 2 sets -> MATCH OVER!
        play301WinForCurrentPlayer()
        m = viewModel.activeMatch.value!!
        assertTrue(m.isMatchOver)
        assertNull(m.pendingSubWin)
        assertEquals(2, m.winner?.setsWon)
    }
}
