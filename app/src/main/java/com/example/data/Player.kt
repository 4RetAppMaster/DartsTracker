package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val wins: Int = 0,
    val losses: Int = 0,
    val matchesPlayed: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val winRatePercent: Int
        get() = if (matchesPlayed > 0) ((wins.toDouble() / matchesPlayed) * 100).toInt() else 0
}
