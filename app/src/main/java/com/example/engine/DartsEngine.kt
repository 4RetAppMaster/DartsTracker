package com.example.engine

import com.example.data.Player
import com.example.model.*
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.pow

object DartsEngine {

    /**
     * Evaluates a turn given 1 to 3 darts thrown.
     */
    fun evaluateTurn(
        playerId: Long,
        turnIndex: Int,
        scoreBefore: Int,
        darts: List<DartThrow>,
        rules: GameRules,
        hasDoubleIn: Boolean
    ): TurnScore {
        var currentScore = scoreBefore
        var doubleInAchieved = hasDoubleIn
        var doubleInUnlockedThisTurn = false
        var isBust = false
        var isWinning = false
        var totalPointsScored = 0

        for (dart in darts) {

            if (rules.doubleIn && !doubleInAchieved) {
                if (dart.isDouble && dart.points > 0) {
                    doubleInAchieved = true
                    doubleInUnlockedThisTurn = true
                    currentScore -= dart.points
                    totalPointsScored += dart.points
                } else {
                    // Missed double-in; scores 0
                    continue
                }
            } else {
                // Double in achieved or not required
                val points = dart.points
                val newScore = currentScore - points

                if (rules.doubleOut) {
                    if (newScore < 0 || newScore == 1) {
                        isBust = true
                        break
                    } else if (newScore == 0) {
                        if (dart.isDouble) {
                            currentScore = 0
                            totalPointsScored += points
                            isWinning = true
                            break
                        } else {
                            // Hit 0 without a double = Bust
                            isBust = true
                            break
                        }
                    } else {
                        currentScore = newScore
                        totalPointsScored += points
                    }
                } else {
                    // Straight out
                    if (newScore < 0) {
                        isBust = true
                        break
                    } else if (newScore == 0) {
                        currentScore = 0
                        totalPointsScored += points
                        isWinning = true
                        break
                    } else {
                        currentScore = newScore
                        totalPointsScored += points
                    }
                }
            }
        }

        return if (isBust) {
            TurnScore(
                playerId = playerId,
                turnIndex = turnIndex,
                darts = darts,
                totalPoints = 0,
                scoreBefore = scoreBefore,
                scoreAfter = scoreBefore,
                isBust = true,
                doubleInUnlockedThisTurn = false,
                isWinningTurn = false
            )
        } else {
            TurnScore(
                playerId = playerId,
                turnIndex = turnIndex,
                darts = darts,
                totalPoints = totalPointsScored,
                scoreBefore = scoreBefore,
                scoreAfter = currentScore,
                isBust = false,
                doubleInUnlockedThisTurn = doubleInUnlockedThisTurn,
                isWinningTurn = isWinning
            )
        }
    }

    // ==================== TOURNAMENT GENERATION ====================

    /**
     * Round Robin (Everyone vs Everyone): Generates full match pairing schedule.
     */
    fun generateRoundRobinMatches(players: List<Player>): List<TournamentMatch> {
        val matches = mutableListOf<TournamentMatch>()
        if (players.size < 2) return matches

        var matchCounter = 1
        for (i in 0 until players.size) {
            for (j in (i + 1) until players.size) {
                val p1 = players[i]
                val p2 = players[j]
                matches.add(
                    TournamentMatch(
                        id = "RR_M$matchCounter",
                        round = 1,
                        roundName = "League Match #$matchCounter",
                        player1Id = p1.id,
                        player1Name = p1.name,
                        player2Id = p2.id,
                        player2Name = p2.name,
                        winnerId = null,
                        winnerName = null,
                        isCompleted = false,
                        isBye = false
                    )
                )
                matchCounter++
            }
        }
        return matches
    }

    /**
     * Straight Elimination (Knockout Bracket):
     * Computes the bracket size (power of 2), pairs up seeds, gives byes if needed,
     * and sets up initial and placeholder subsequent rounds.
     */
    fun generateEliminationMatches(players: List<Player>): List<TournamentMatch> {
        if (players.size < 2) return emptyList()

        val n = players.size
        // Next power of 2
        val bracketSize = 2.0.pow(ceil(log2(n.toDouble()))).toInt()
        val totalRounds = (log2(bracketSize.toDouble())).toInt()

        val matches = mutableListOf<TournamentMatch>()

        // Round 1 matches
        val round1MatchesCount = bracketSize / 2
        for (i in 0 until round1MatchesCount) {
            val p1Index = i
            val p2Index = bracketSize - 1 - i

            val p1 = if (p1Index < n) players[p1Index] else null
            val p2 = if (p2Index < n) players[p2Index] else null

            val isBye = (p1 != null && p2 == null) || (p1 == null && p2 != null)
            val byeWinner = if (p1 != null && p2 == null) p1 else if (p2 != null && p1 == null) p2 else null

            matches.add(
                TournamentMatch(
                    id = "R1_M${i + 1}",
                    round = 1,
                    roundName = getRoundName(1, totalRounds),
                    player1Id = p1?.id ?: 0L,
                    player1Name = p1?.name ?: "TBD",
                    player2Id = p2?.id,
                    player2Name = p2?.name ?: if (isBye) "BYE" else "TBD",
                    winnerId = byeWinner?.id,
                    winnerName = byeWinner?.name,
                    isCompleted = isBye,
                    isBye = isBye
                )
            )
        }

        // Generate placeholders for subsequent rounds
        var currentRoundMatches = round1MatchesCount / 2
        for (round in 2..totalRounds) {
            for (m in 1..currentRoundMatches) {
                matches.add(
                    TournamentMatch(
                        id = "R${round}_M$m",
                        round = round,
                        roundName = getRoundName(round, totalRounds),
                        player1Id = 0L,
                        player1Name = "Winner of R${round - 1}_M${(m * 2) - 1}",
                        player2Id = null,
                        player2Name = "Winner of R${round - 1}_M${m * 2}",
                        winnerId = null,
                        winnerName = null,
                        isCompleted = false,
                        isBye = false
                    )
                )
            }
            currentRoundMatches /= 2
        }

        // If any Round 1 match was a bye, propagate the winner automatically!
        var updated = matches.toList()
        for (m in matches.filter { it.round == 1 && it.isBye && it.winnerId != null }) {
            updated = propagateWinner(updated, m.id, m.winnerId!!, m.winnerName!!)
        }

        return updated
    }

    private fun getRoundName(round: Int, totalRounds: Int): String {
        return when (totalRounds - round) {
            0 -> "Final"
            1 -> "Semifinals"
            2 -> "Quarterfinals"
            3 -> "Round of 16"
            else -> "Round $round"
        }
    }

    /**
     * Updates tournament matches with the winner of matchId,
     * advancing them into the next round match.
     */
    fun propagateWinner(
        matches: List<TournamentMatch>,
        completedMatchId: String,
        winnerId: Long,
        winnerName: String
    ): List<TournamentMatch> {
        val updated = matches.map { match ->
            if (match.id == completedMatchId) {
                match.copy(
                    winnerId = winnerId,
                    winnerName = winnerName,
                    isCompleted = true
                )
            } else {
                match
            }
        }.toMutableList()

        // Find which subsequent match depends on completedMatchId
        val completedMatch = updated.find { it.id == completedMatchId } ?: return updated
        val nextRound = completedMatch.round + 1

        // In binary tree bracket: match M in round R feeds into match ((M + 1) / 2) in round R + 1
        val matchNumStr = completedMatchId.substringAfter("_M")
        val matchNum = matchNumStr.toIntOrNull() ?: 1
        val nextMatchNum = (matchNum + 1) / 2
        val nextMatchId = "R${nextRound}_M$nextMatchNum"

        val nextMatchIndex = updated.indexOfFirst { it.id == nextMatchId }
        if (nextMatchIndex != -1) {
            val targetMatch = updated[nextMatchIndex]
            val isFirstSlot = (matchNum % 2 != 0)

            val newTarget = if (isFirstSlot) {
                targetMatch.copy(
                    player1Id = winnerId,
                    player1Name = winnerName
                )
            } else {
                targetMatch.copy(
                    player2Id = winnerId,
                    player2Name = winnerName
                )
            }
            updated[nextMatchIndex] = newTarget
        }

        return updated
    }

    /**
     * Computes standings table for Round Robin in a single pass.
     */
    fun calculateRoundRobinStandings(players: List<Player>, matches: List<TournamentMatch>): List<StandingRow> {
        // [played, won]
        val statsByPlayer = HashMap<Long, IntArray>(players.size)
        for (player in players) {
            statsByPlayer[player.id] = IntArray(2)
        }

        for (m in matches) {
            if (!m.isCompleted || m.isBye) continue
            statsByPlayer[m.player1Id]?.let { stats ->
                stats[0]++
                if (m.winnerId == m.player1Id) stats[1]++
            }
            val p2 = m.player2Id ?: continue
            statsByPlayer[p2]?.let { stats ->
                stats[0]++
                if (m.winnerId == p2) stats[1]++
            }
        }

        return players.map { player ->
            val stats = statsByPlayer[player.id]
            val played = stats?.get(0) ?: 0
            val won = stats?.get(1) ?: 0
            val lost = played - won
            val points = won * 2
            val winRate = if (played > 0) ((won.toDouble() / played) * 100).toInt() else 0
            StandingRow(
                player = player,
                played = played,
                won = won,
                lost = lost,
                points = points,
                winRate = winRate
            )
        }.sortedWith(
            compareByDescending<StandingRow> { it.points }
                .thenByDescending { it.won }
                .thenByDescending { it.winRate }
        )
    }
}
