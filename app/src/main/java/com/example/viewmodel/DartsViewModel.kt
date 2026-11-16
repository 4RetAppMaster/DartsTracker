package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Player
import com.example.data.PlayerRepository
import com.example.engine.DartsEngine
import com.example.model.*
import com.example.util.UpdateCheckResult
import com.example.util.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PendingSubWin(
    val title: String,
    val winnerName: String,
    val winnerPlayerId: Long,
    val isSetWin: Boolean,
    val setsWon: Int,
    val legsWon: Int,
    val nextLeg: Int,
    val nextSet: Int
)

data class ActiveMatchState(
    val rules: GameRules,
    val players: List<PlayerGameState>,
    val currentPlayerIndex: Int = 0,
    val currentLeg: Int = 1,
    val currentSet: Int = 1,
    val legStarterIndex: Int = 0,
    val isMatchOver: Boolean = false,
    val winner: PlayerGameState? = null,
    val pendingSubWin: PendingSubWin? = null,
    val tournamentMatchId: String? = null,
    val currentDarts: List<DartThrow> = emptyList(),
    val isBustFeedback: Boolean = false,
    val turnHistory: List<TurnHistorySnapshot> = emptyList()
) {
    val currentPlayer: PlayerGameState?
        get() = players.getOrNull(currentPlayerIndex)
}

data class TurnHistorySnapshot(
    val previousPlayers: List<PlayerGameState>,
    val previousCurrentPlayerIndex: Int,
    val previousLeg: Int,
    val previousSet: Int,
    val previousLegStarterIndex: Int,
    val turnScore: TurnScore
)

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class Result(val checkResult: UpdateCheckResult) : UpdateUiState()
}

class DartsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlayerRepository

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    private var hasDismissedUpdateThisSession = false
    private var lastUpdateCheckTimeMs = 0L

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PlayerRepository(db.playerDao())

        // Silent check on startup; surface dialog automatically only if a newer version exists
        checkForUpdates(userInitiated = false)
    }

    fun onAppOpened() {
        if (hasDismissedUpdateThisSession || _showUpdateDialog.value) return
        if (_updateUiState.value is UpdateUiState.Checking) return
        val lastResult = (_updateUiState.value as? UpdateUiState.Result)?.checkResult
        val now = System.currentTimeMillis()
        val cooldownMs = 60_000L
        if (lastResult is UpdateCheckResult.Error || now - lastUpdateCheckTimeMs >= cooldownMs) {
            checkForUpdates(userInitiated = false)
        }
    }

    fun checkForUpdates(userInitiated: Boolean) {
        if (userInitiated) {
            hasDismissedUpdateThisSession = false
            _showUpdateDialog.value = true
        }
        lastUpdateCheckTimeMs = System.currentTimeMillis()
        _updateUiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = Updater.checkForUpdate()
            lastUpdateCheckTimeMs = System.currentTimeMillis()
            _updateUiState.value = UpdateUiState.Result(result)
            if (!userInitiated && result is UpdateCheckResult.UpdateAvailable && !hasDismissedUpdateThisSession) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun dismissUpdateDialog() {
        val result = (_updateUiState.value as? UpdateUiState.Result)?.checkResult
        if (result is UpdateCheckResult.UpdateAvailable) {
            hasDismissedUpdateThisSession = true
        }
        _showUpdateDialog.value = false
    }

    val allPlayers: StateFlow<List<Player>> = repository.allPlayers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Setup screen selections
    private val _selectedGameType = MutableStateFlow(GameType.GAME_501)
    val selectedGameType: StateFlow<GameType> = _selectedGameType.asStateFlow()

    private val _doubleIn = MutableStateFlow(false)
    val doubleIn: StateFlow<Boolean> = _doubleIn.asStateFlow()

    private val _doubleOut = MutableStateFlow(true)
    val doubleOut: StateFlow<Boolean> = _doubleOut.asStateFlow()

    private val _setsToWin = MutableStateFlow(1)
    val setsToWin: StateFlow<Int> = _setsToWin.asStateFlow()

    private val _legsToWinSet = MutableStateFlow(1)
    val legsToWinSet: StateFlow<Int> = _legsToWinSet.asStateFlow()

    private val _selectedPlayerIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedPlayerIds: StateFlow<List<Long>> = _selectedPlayerIds.asStateFlow()

    // Active Live Scoring Match
    private val _activeMatch = MutableStateFlow<ActiveMatchState?>(null)
    val activeMatch: StateFlow<ActiveMatchState?> = _activeMatch.asStateFlow()

    // Tournament
    private val _tournamentState = MutableStateFlow<TournamentState?>(null)
    val tournamentState: StateFlow<TournamentState?> = _tournamentState.asStateFlow()

    // Player Management Actions
    fun addPlayer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertPlayer(Player(name = name.trim()))
        }
    }

    fun editPlayer(player: Player, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updatePlayer(player.copy(name = newName.trim()))
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch {
            repository.deletePlayer(player)
            _selectedPlayerIds.update { list -> list.filter { it != player.id } }
        }
    }

    fun resetPlayerStats(playerId: Long) {
        viewModelScope.launch {
            repository.resetPlayerStats(playerId)
        }
    }

    fun exportPlayersJson(players: List<Player> = allPlayers.value): String {
        val jsonArray = JSONArray()
        for (p in players) {
            jsonArray.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("wins", p.wins)
                    put("losses", p.losses)
                    put("matchesPlayed", p.matchesPlayed)
                    put("createdAt", p.createdAt)
                }
            )
        }
        return jsonArray.toString(2)
    }

    suspend fun exportPlayersJsonAsync(players: List<Player> = allPlayers.value): String =
        withContext(Dispatchers.Default) { exportPlayersJson(players) }

    suspend fun importPlayersJson(jsonStr: String, replaceAll: Boolean = false): Result<Int> {
        return try {
            val parsedPlayers = withContext(Dispatchers.Default) {
                val trimmed = jsonStr
                    .replace("\uFEFF", "")
                    .replace("\u0000", "")
                    .trim()
                val jsonArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("players") ?: JSONArray().apply { put(root) }
                } else {
                    throw IllegalArgumentException("Invalid JSON format. Expected JSON array or object.")
                }

                val list = ArrayList<Player>(jsonArray.length())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name", "").trim()
                    if (name.isNotEmpty()) {
                        val wins = obj.optInt("wins", 0)
                        val losses = obj.optInt("losses", 0)
                        val matchesPlayed = obj.optInt("matchesPlayed", wins + losses)
                        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        list.add(
                            Player(
                                name = name,
                                wins = wins,
                                losses = losses,
                                matchesPlayed = matchesPlayed,
                                createdAt = createdAt
                            )
                        )
                    }
                }
                list
            }

            if (parsedPlayers.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid player records found in JSON."))
            }

            withContext(Dispatchers.IO) {
                if (replaceAll) {
                    repository.replaceAllPlayers(parsedPlayers)
                } else {
                    val existingByName = repository.getAllPlayersList()
                        .associateBy { it.name.lowercase() }
                    val toInsert = mutableListOf<Player>()
                    val toUpdate = mutableListOf<Player>()
                    for (np in parsedPlayers) {
                        val match = existingByName[np.name.lowercase()]
                        if (match != null) {
                            toUpdate.add(
                                match.copy(
                                    wins = np.wins,
                                    losses = np.losses,
                                    matchesPlayed = np.matchesPlayed
                                )
                            )
                        } else {
                            toInsert.add(np)
                        }
                    }
                    repository.mergeImportedPlayers(toInsert, toUpdate)
                }
            }
            Result.success(parsedPlayers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Setup configuration setters
    fun setGameType(type: GameType) {
        _selectedGameType.value = type
    }

    fun setDoubleIn(enabled: Boolean) {
        _doubleIn.value = enabled
    }

    fun setDoubleOut(enabled: Boolean) {
        _doubleOut.value = enabled
    }

    fun setSetsToWin(sets: Int) {
        _setsToWin.value = sets.coerceIn(1, 10)
    }

    fun setLegsToWinSet(legs: Int) {
        _legsToWinSet.value = legs.coerceIn(1, 10)
    }

    fun setSelectedPlayers(playerIds: List<Long>) {
        _selectedPlayerIds.value = playerIds
    }

    fun removeSelectedPlayer(playerId: Long) {
        _selectedPlayerIds.update { list -> list.filter { it != playerId } }
    }

    fun reorderSelectedPlayers(fromIndex: Int, toIndex: Int) {
        _selectedPlayerIds.update { list ->
            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                val mutable = list.toMutableList()
                val item = mutable.removeAt(fromIndex)
                mutable.add(toIndex, item)
                mutable
            } else {
                list
            }
        }
    }

    fun clearPlayerSelection() {
        _selectedPlayerIds.value = emptyList()
    }

    // ==================== MATCH INITIALIZATION ====================

    fun startMatch(
        customPlayerIds: List<Long>? = null,
        tournamentMatchId: String? = null,
        rulesOverride: GameRules? = null
    ) {
        val playerIds = customPlayerIds ?: _selectedPlayerIds.value
        val playersById = allPlayers.value.associateBy { it.id }
        val sortedPlayers = playerIds.mapNotNull { playersById[it] }
        if (sortedPlayers.isEmpty()) return

        val rules = rulesOverride ?: GameRules(
            gameType = _selectedGameType.value,
            doubleIn = _doubleIn.value,
            doubleOut = _doubleOut.value,
            setsToWin = _setsToWin.value,
            legsToWinSet = _legsToWinSet.value
        )

        val playerStates = sortedPlayers.map { player ->
            PlayerGameState(
                player = player,
                currentScore = rules.gameType.targetScore,
                hasDoubleInAchieved = !rules.doubleIn
            )
        }

        _activeMatch.value = ActiveMatchState(
            rules = rules,
            players = playerStates,
            tournamentMatchId = tournamentMatchId
        )
    }

    // ==================== LIVE SCORING ACTIONS ====================

    fun addDartThrow(dart: DartThrow) {
        val match = _activeMatch.value ?: return
        if (match.isMatchOver || match.pendingSubWin != null || match.currentDarts.size >= 3) return
        val currentP = match.currentPlayer ?: return

        val newDarts = (match.currentDarts + dart).take(3)

        // Evaluate if this turn should auto-complete (3 darts thrown, bust, or winning turn)
        val turnScore = DartsEngine.evaluateTurn(
            playerId = currentP.player.id,
            turnIndex = currentP.turns.size + 1,
            scoreBefore = currentP.currentScore,
            darts = newDarts,
            rules = match.rules,
            hasDoubleIn = currentP.hasDoubleInAchieved
        )

        if (turnScore.isWinningTurn || turnScore.isBust || newDarts.size == 3) {
            applyTurnScore(turnScore)
        } else {
            _activeMatch.value = match.copy(currentDarts = newDarts, isBustFeedback = false)
        }
    }

    fun removeLastDartThrow() {
        val match = _activeMatch.value ?: return
        if (match.currentDarts.isNotEmpty()) {
            _activeMatch.value = match.copy(
                currentDarts = match.currentDarts.dropLast(1),
                isBustFeedback = false
            )
        } else if (match.turnHistory.isNotEmpty()) {
            val lastSnapshot = match.turnHistory.last()
            val previousDarts = lastSnapshot.turnScore.darts.dropLast(1)
            _activeMatch.value = match.copy(
                players = lastSnapshot.previousPlayers,
                currentPlayerIndex = lastSnapshot.previousCurrentPlayerIndex,
                currentLeg = lastSnapshot.previousLeg,
                currentSet = lastSnapshot.previousSet,
                legStarterIndex = lastSnapshot.previousLegStarterIndex,
                isMatchOver = false,
                winner = null,
                currentDarts = previousDarts,
                isBustFeedback = false,
                turnHistory = match.turnHistory.dropLast(1)
            )
        }
    }

    private fun applyTurnScore(turnScore: TurnScore) {
        val match = _activeMatch.value ?: return
        val playerIdx = match.currentPlayerIndex
        val currentP = match.players[playerIdx]

        val snapshot = TurnHistorySnapshot(
            previousPlayers = match.players,
            previousCurrentPlayerIndex = match.currentPlayerIndex,
            previousLeg = match.currentLeg,
            previousSet = match.currentSet,
            previousLegStarterIndex = match.legStarterIndex,
            turnScore = turnScore
        )

        val updatedTurns = currentP.turns + turnScore

        if (turnScore.isWinningTurn) {
            val newLegsWon = currentP.legsWon + 1
            val setsToWin = match.rules.setsToWin.coerceAtLeast(1)
            val legsToWinSet = match.rules.legsToWinSet.coerceAtLeast(1)

            if (newLegsWon >= legsToWinSet) {
                val newSetsWon = currentP.setsWon + 1
                if (newSetsWon >= setsToWin) {
                    val finalWinnerP = currentP.copy(
                        currentScore = 0,
                        hasDoubleInAchieved = currentP.hasDoubleInAchieved || turnScore.doubleInUnlockedThisTurn,
                        turns = updatedTurns,
                        legsWon = newLegsWon,
                        setsWon = newSetsWon,
                        isWinner = true
                    )
                    val updatedPlayers = match.players.toMutableList()
                    updatedPlayers[playerIdx] = finalWinnerP

                    _activeMatch.value = match.copy(
                        players = updatedPlayers,
                        isMatchOver = true,
                        winner = finalWinnerP,
                        pendingSubWin = null,
                        currentDarts = emptyList(),
                        isBustFeedback = false,
                        turnHistory = match.turnHistory + snapshot
                    )
                } else {
                    val updatedWinnerP = currentP.copy(
                        currentScore = 0,
                        hasDoubleInAchieved = currentP.hasDoubleInAchieved || turnScore.doubleInUnlockedThisTurn,
                        turns = updatedTurns,
                        legsWon = newLegsWon,
                        setsWon = newSetsWon,
                        isWinner = false
                    )
                    val updatedPlayers = match.players.toMutableList()
                    updatedPlayers[playerIdx] = updatedWinnerP

                    _activeMatch.value = match.copy(
                        players = updatedPlayers,
                        pendingSubWin = PendingSubWin(
                            title = "SET ${match.currentSet} WON!",
                            winnerName = currentP.player.name,
                            winnerPlayerId = currentP.player.id,
                            isSetWin = true,
                            setsWon = newSetsWon,
                            legsWon = newLegsWon,
                            nextLeg = 1,
                            nextSet = match.currentSet + 1
                        ),
                        isMatchOver = false,
                        winner = null,
                        currentDarts = emptyList(),
                        isBustFeedback = false,
                        turnHistory = match.turnHistory + snapshot
                    )
                }
            } else {
                val updatedWinnerP = currentP.copy(
                    currentScore = 0,
                    hasDoubleInAchieved = currentP.hasDoubleInAchieved || turnScore.doubleInUnlockedThisTurn,
                    turns = updatedTurns,
                    legsWon = newLegsWon,
                    isWinner = false
                )
                val updatedPlayers = match.players.toMutableList()
                updatedPlayers[playerIdx] = updatedWinnerP

                _activeMatch.value = match.copy(
                    players = updatedPlayers,
                    pendingSubWin = PendingSubWin(
                        title = "LEG ${match.currentLeg} WON!",
                        winnerName = currentP.player.name,
                        winnerPlayerId = currentP.player.id,
                        isSetWin = false,
                        setsWon = currentP.setsWon,
                        legsWon = newLegsWon,
                        nextLeg = match.currentLeg + 1,
                        nextSet = match.currentSet
                    ),
                    isMatchOver = false,
                    winner = null,
                    currentDarts = emptyList(),
                    isBustFeedback = false,
                    turnHistory = match.turnHistory + snapshot
                )
            }
        } else {
            val updatedP = currentP.copy(
                currentScore = turnScore.scoreAfter,
                hasDoubleInAchieved = currentP.hasDoubleInAchieved || turnScore.doubleInUnlockedThisTurn,
                turns = updatedTurns,
                isWinner = false
            )
            val updatedPlayers = match.players.toMutableList()
            updatedPlayers[playerIdx] = updatedP

            val nextIndex = (playerIdx + 1) % match.players.size
            _activeMatch.value = match.copy(
                players = updatedPlayers,
                currentPlayerIndex = nextIndex,
                isMatchOver = false,
                winner = null,
                pendingSubWin = null,
                currentDarts = emptyList(),
                isBustFeedback = turnScore.isBust,
                turnHistory = match.turnHistory + snapshot
            )
        }
    }

    fun confirmSubWinAndAdvance() {
        val match = _activeMatch.value ?: return
        val subWin = match.pendingSubWin ?: return
        val nextLegStarter = (match.legStarterIndex + 1) % match.players.size

        val resetPlayers = match.players.map { p ->
            p.copy(
                currentScore = match.rules.gameType.targetScore,
                hasDoubleInAchieved = !match.rules.doubleIn,
                archivedTurns = p.archivedTurns + p.turns,
                turns = emptyList(),
                legsWon = if (subWin.isSetWin) 0 else p.legsWon,
                isWinner = false
            )
        }

        _activeMatch.value = match.copy(
            players = resetPlayers,
            currentSet = subWin.nextSet,
            currentLeg = subWin.nextLeg,
            legStarterIndex = nextLegStarter,
            currentPlayerIndex = nextLegStarter,
            pendingSubWin = null,
            isMatchOver = false,
            winner = null,
            currentDarts = emptyList(),
            isBustFeedback = false
        )
    }

    fun revertFalseWin() {
        val match = _activeMatch.value ?: return
        if (match.turnHistory.isEmpty()) return
        val lastSnapshot = match.turnHistory.last()
        val previousDarts = lastSnapshot.turnScore.darts.dropLast(1)
        _activeMatch.value = match.copy(
            players = lastSnapshot.previousPlayers,
            currentPlayerIndex = lastSnapshot.previousCurrentPlayerIndex,
            currentLeg = lastSnapshot.previousLeg,
            currentSet = lastSnapshot.previousSet,
            legStarterIndex = lastSnapshot.previousLegStarterIndex,
            isMatchOver = false,
            winner = null,
            pendingSubWin = null,
            currentDarts = previousDarts,
            isBustFeedback = false,
            turnHistory = match.turnHistory.dropLast(1)
        )
    }

    fun hasNextTournamentMatch(match: ActiveMatchState): Boolean {
        val tMatchId = match.tournamentMatchId ?: return false
        val winner = match.winner ?: return false
        val t = _tournamentState.value ?: return false
        val simulatedMatches = if (t.tournamentType == TournamentType.STRAIGHT_ELIMINATION) {
            DartsEngine.propagateWinner(t.matches, tMatchId, winner.player.id, winner.player.name)
        } else {
            t.matches.map { m ->
                if (m.id == tMatchId) {
                    m.copy(winnerId = winner.player.id, winnerName = winner.player.name, isCompleted = true)
                } else m
            }
        }
        return simulatedMatches.any {
            !it.isCompleted && it.player1Id != 0L && it.player2Id != null && it.player2Id != 0L
        }
    }

    private fun commitCompletedMatchResult(match: ActiveMatchState) {
        val winner = match.winner ?: return
        val winnerId = winner.player.id
        val loserIds = match.players.filter { it.player.id != winnerId }.map { it.player.id }
        viewModelScope.launch {
            repository.recordMatchResult(winnerId, loserIds)
        }
        match.tournamentMatchId?.let { tMatchId ->
            updateTournamentMatchResult(tMatchId, winnerId, winner.player.name)
        }
    }

    fun confirmMatchWinAndExit() {
        val match = _activeMatch.value ?: return
        commitCompletedMatchResult(match)
        _activeMatch.value = null
    }

    fun exitMatch() {
        _activeMatch.value = null
    }

    // ==================== TOURNAMENT MANAGEMENT ====================

    fun startTournament(type: TournamentType, rules: GameRules, playerIds: List<Long>) {
        val playersById = allPlayers.value.associateBy { it.id }
        val sortedPlayers = playerIds.mapNotNull { playersById[it] }
        if (sortedPlayers.size < 2) return

        val matches = when (type) {
            TournamentType.ROUND_ROBIN -> DartsEngine.generateRoundRobinMatches(sortedPlayers)
            TournamentType.STRAIGHT_ELIMINATION -> DartsEngine.generateEliminationMatches(sortedPlayers)
        }

        _tournamentState.value = TournamentState(
            tournamentType = type,
            rules = rules,
            originalPlayers = sortedPlayers,
            matches = matches,
            currentMatchId = matches.firstOrNull { !it.isCompleted }?.id,
            isFinished = false,
            champion = null
        )
    }

    private fun updateTournamentMatchResult(matchId: String, winnerId: Long, winnerName: String) {
        _tournamentState.update { tournament ->
            tournament?.let { t ->
                val updatedMatches = if (t.tournamentType == TournamentType.STRAIGHT_ELIMINATION) {
                    DartsEngine.propagateWinner(t.matches, matchId, winnerId, winnerName)
                } else {
                    t.matches.map { m ->
                        if (m.id == matchId) {
                            m.copy(winnerId = winnerId, winnerName = winnerName, isCompleted = true)
                        } else m
                    }
                }

                var isFinished = false
                var champion: Player? = null

                if (t.tournamentType == TournamentType.STRAIGHT_ELIMINATION) {
                    val finalMatch = updatedMatches.maxByOrNull { it.round }
                    if (finalMatch != null && finalMatch.isCompleted && finalMatch.winnerId != null) {
                        isFinished = true
                        champion = t.originalPlayers.find { it.id == finalMatch.winnerId }
                    }
                } else {
                    val allCompleted = updatedMatches.all { it.isCompleted }
                    if (allCompleted) {
                        isFinished = true
                        val standings = DartsEngine.calculateRoundRobinStandings(t.originalPlayers, updatedMatches)
                        champion = standings.firstOrNull()?.player
                    }
                }

                val nextMatch = updatedMatches.firstOrNull { !it.isCompleted && it.player1Id != 0L && it.player2Id != null && it.player2Id != 0L }

                t.copy(
                    matches = updatedMatches,
                    currentMatchId = nextMatch?.id,
                    isFinished = isFinished,
                    champion = champion
                )
            }
        }
    }

    fun launchTournamentMatch(matchId: String) {
        val t = _tournamentState.value ?: return
        val match = t.matches.find { it.id == matchId } ?: return
        if (match.isCompleted || match.player2Id == null || match.player1Id == 0L || match.player2Id == 0L) return

        _selectedGameType.value = t.rules.gameType
        _doubleIn.value = t.rules.doubleIn
        _doubleOut.value = t.rules.doubleOut
        _setsToWin.value = t.rules.setsToWin
        _legsToWinSet.value = t.rules.legsToWinSet

        startMatch(
            customPlayerIds = listOf(match.player1Id, match.player2Id),
            tournamentMatchId = matchId,
            rulesOverride = t.rules
        )
    }

    fun shuffleEliminationPlayers() {
        _tournamentState.update { tournament ->
            if (tournament != null && tournament.tournamentType == TournamentType.STRAIGHT_ELIMINATION) {
                val shuffledPlayers = tournament.originalPlayers.shuffled()
                val newMatches = DartsEngine.generateEliminationMatches(shuffledPlayers)
                tournament.copy(
                    originalPlayers = shuffledPlayers,
                    matches = newMatches,
                    currentMatchId = newMatches.firstOrNull { !it.isCompleted }?.id,
                    isFinished = false,
                    champion = null
                )
            } else {
                tournament
            }
        }
    }

    fun endTournament() {
        _tournamentState.value = null
        _activeMatch.value = null
    }
}
