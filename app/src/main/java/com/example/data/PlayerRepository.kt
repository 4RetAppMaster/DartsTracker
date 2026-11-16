package com.example.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    val allPlayers: Flow<List<Player>> = playerDao.getAllPlayers()

    suspend fun getAllPlayersList(): List<Player> = playerDao.getAllPlayersList()

    suspend fun getPlayerById(id: Long): Player? = playerDao.getPlayerById(id)

    suspend fun insertPlayer(player: Player): Long = playerDao.insertPlayer(player)

    suspend fun updatePlayer(player: Player) = playerDao.updatePlayer(player)

    suspend fun deletePlayer(player: Player) = playerDao.deletePlayer(player)

    suspend fun recordMatchResult(winnerId: Long, loserIds: List<Long>) {
        playerDao.recordWin(winnerId)
        for (loserId in loserIds) {
            playerDao.recordLoss(loserId)
        }
    }

    suspend fun resetPlayerStats(id: Long) = playerDao.resetStats(id)
}
