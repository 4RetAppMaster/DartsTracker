package com.example.model

import com.example.data.Player

enum class GameType(val targetScore: Int, val displayName: String) {
    GAME_301(301, "301"),
    GAME_401(401, "401"),
    GAME_501(501, "501")
}

data class GameRules(
    val gameType: GameType = GameType.GAME_501,
    val doubleIn: Boolean = false,
    val doubleOut: Boolean = true,
    val setsToWin: Int = 1,
    val legsToWinSet: Int = 1
)

enum class Multiplier(val factor: Int, val prefix: String) {
    SINGLE(1, "S"),
    DOUBLE(2, "D"),
    TRIPLE(3, "T"),
    MISS(0, "")
}

data class DartThrow(
    val segment: Int, // 0 to 20, 25 for Bull
    val multiplier: Multiplier = Multiplier.SINGLE
) {
    val points: Int
        get() = when {
            multiplier == Multiplier.MISS -> 0
            segment == 25 && multiplier == Multiplier.SINGLE -> 25
            segment == 25 && multiplier == Multiplier.DOUBLE -> 50
            segment == 25 -> 25
            else -> segment * multiplier.factor
        }

    val isDouble: Boolean
        get() = (multiplier == Multiplier.DOUBLE) || (segment == 25 && multiplier == Multiplier.DOUBLE)

    val label: String
        get() = when {
            multiplier == Multiplier.MISS || (segment == 0 && multiplier == Multiplier.SINGLE) -> "MISS"
            segment == 25 && multiplier == Multiplier.DOUBLE -> "BULL"
            segment == 25 -> "25"
            multiplier == Multiplier.SINGLE -> "$segment"
            multiplier == Multiplier.DOUBLE -> "D$segment"
            multiplier == Multiplier.TRIPLE -> "T$segment"
            else -> "$points"
        }
}

data class TurnScore(
    val playerId: Long,
    val turnIndex: Int,
    val darts: List<DartThrow>,
    val totalPoints: Int,
    val scoreBefore: Int,
    val scoreAfter: Int,
    val isBust: Boolean,
    val doubleInUnlockedThisTurn: Boolean = false,
    val isWinningTurn: Boolean = false
)

data class PlayerGameState(
    val player: Player,
    val currentScore: Int,
    val hasDoubleInAchieved: Boolean = false,
    val turns: List<TurnScore> = emptyList(),
    val legsWon: Int = 0,
    val setsWon: Int = 0,
    val isWinner: Boolean = false
) {
    val totalDartsThrown: Int
        get() = turns.sumOf { it.darts.size }

    val totalPointsScored: Int
        get() = turns.filter { !it.isBust }.sumOf { it.totalPoints }

    val threeDartAverage: Double
        get() {
            val darts = totalDartsThrown
            return if (darts > 0) (totalPointsScored.toDouble() / darts) * 3 else 0.0
        }

    val highestTurnScore: Int
        get() = turns.filter { !it.isBust }.maxOfOrNull { it.totalPoints } ?: 0
}

enum class TournamentType(val title: String, val subtitle: String) {
    ROUND_ROBIN("Everyone vs Everyone", "Round-Robin league where all players face off"),
    STRAIGHT_ELIMINATION("Straight Elimination", "Knockout bracket where winners advance to the finals")
}

data class TournamentMatch(
    val id: String,
    val round: Int,
    val roundName: String,
    val player1Id: Long,
    val player1Name: String,
    val player2Id: Long?,
    val player2Name: String?,
    val winnerId: Long? = null,
    val winnerName: String? = null,
    val isCompleted: Boolean = false,
    val isBye: Boolean = false
)

data class StandingRow(
    val player: Player,
    val played: Int,
    val won: Int,
    val lost: Int,
    val points: Int,
    val winRate: Int
)

data class TournamentState(
    val tournamentType: TournamentType,
    val rules: GameRules,
    val originalPlayers: List<Player>,
    val matches: List<TournamentMatch>,
    val currentMatchId: String? = null,
    val isFinished: Boolean = false,
    val champion: Player? = null
)
