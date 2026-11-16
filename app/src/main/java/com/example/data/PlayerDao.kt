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

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)

    @Query("UPDATE players SET wins = wins + 1, matchesPlayed = matchesPlayed + 1 WHERE id = :id")
    suspend fun recordWin(id: Long)

    @Query("UPDATE players SET losses = losses + 1, matchesPlayed = matchesPlayed + 1 WHERE id = :id")
    suspend fun recordLoss(id: Long)

    @Query("UPDATE players SET wins = 0, losses = 0, matchesPlayed = 0 WHERE id = :id")
    suspend fun resetStats(id: Long)
}
