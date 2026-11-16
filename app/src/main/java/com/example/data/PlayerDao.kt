package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllPlayersList(): List<Player>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<Player>)

    @Update
    suspend fun updatePlayer(player: Player)

    @Update
    suspend fun updatePlayers(players: List<Player>)

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("DELETE FROM players")
    suspend fun deleteAllPlayers()

    @androidx.room.Transaction
    suspend fun replaceAllPlayers(players: List<Player>) {
        deleteAllPlayers()
        if (players.isNotEmpty()) {
            insertPlayers(players)
        }
    }

    @androidx.room.Transaction
    suspend fun mergeImportedPlayers(toInsert: List<Player>, toUpdate: List<Player>) {
        if (toUpdate.isNotEmpty()) updatePlayers(toUpdate)
        if (toInsert.isNotEmpty()) insertPlayers(toInsert)
    }

    @Query("UPDATE players SET wins = wins + 1, matchesPlayed = matchesPlayed + 1 WHERE id = :id")
    suspend fun recordWin(id: Long)

    @Query("UPDATE players SET losses = losses + 1, matchesPlayed = matchesPlayed + 1 WHERE id IN (:ids)")
    suspend fun recordLosses(ids: List<Long>)

    @androidx.room.Transaction
    suspend fun recordMatchResult(winnerId: Long, loserIds: List<Long>) {
        recordWin(winnerId)
        if (loserIds.isNotEmpty()) {
            recordLosses(loserIds)
        }
    }

    @Query("UPDATE players SET wins = 0, losses = 0, matchesPlayed = 0 WHERE id = :id")
    suspend fun resetStats(id: Long)
}
