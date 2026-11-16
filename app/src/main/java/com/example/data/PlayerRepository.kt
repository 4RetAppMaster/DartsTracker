package com.example.data

import kotlinx.coroutines.flow.Flow

class PlayerRepository(private val playerDao: PlayerDao) {
    val allPlayers: Flow<List<Player>> = playerDao.getAllPlayers()

    suspend fun getAllPlayersList(): List<Player> = playerDao.getAllPlayersList()

    suspend fun insertPlayer(player: Player): Long = playerDao.insertPlayer(player)

    suspend fun updatePlayer(player: Player) = playerDao.updatePlayer(player)

    suspend fun deletePlayer(player: Player) = playerDao.deletePlayer(player)

    suspend fun replaceAllPlayers(players: List<Player>) = playerDao.replaceAllPlayers(players)

    suspend fun mergeImportedPlayers(toInsert: List<Player>, toUpdate: List<Player>) =
        playerDao.mergeImportedPlayers(toInsert, toUpdate)

    suspend fun recordMatchResult(winnerId: Long, loserIds: List<Long>) =
        playerDao.recordMatchResult(winnerId, loserIds)

    suspend fun resetPlayerStats(id: Long) = playerDao.resetStats(id)
}
