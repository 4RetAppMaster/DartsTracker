package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Player::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "darts_counter_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default players on first run
                        scope?.launch(Dispatchers.IO) {
                            val dao = INSTANCE?.playerDao() ?: return@launch
                            dao.insertPlayer(Player(name = "Luke Littler", wins = 12, losses = 2, matchesPlayed = 14))
                            dao.insertPlayer(Player(name = "Michael van Gerwen", wins = 10, losses = 4, matchesPlayed = 14))
                            dao.insertPlayer(Player(name = "Gerwyn Price", wins = 8, losses = 5, matchesPlayed = 13))
                            dao.insertPlayer(Player(name = "Peter Wright", wins = 7, losses = 6, matchesPlayed = 13))
                            dao.insertPlayer(Player(name = "Michael Smith", wins = 6, losses = 6, matchesPlayed = 12))
                            dao.insertPlayer(Player(name = "Rob Cross", wins = 5, losses = 6, matchesPlayed = 11))
                            dao.insertPlayer(Player(name = "Nathan Aspinall", wins = 5, losses = 5, matchesPlayed = 10))
                            dao.insertPlayer(Player(name = "Jonny Clayton", wins = 4, losses = 5, matchesPlayed = 9))
                            dao.insertPlayer(Player(name = "Dimitri Van den Bergh", wins = 4, losses = 6, matchesPlayed = 10))
                            dao.insertPlayer(Player(name = "Dave Chisnall", wins = 3, losses = 6, matchesPlayed = 9))
                            dao.insertPlayer(Player(name = "Player 1", wins = 3, losses = 1, matchesPlayed = 4))
                            dao.insertPlayer(Player(name = "Player 2", wins = 2, losses = 2, matchesPlayed = 4))
                            dao.insertPlayer(Player(name = "Player 3", wins = 1, losses = 3, matchesPlayed = 4))
                            dao.insertPlayer(Player(name = "Player 4", wins = 0, losses = 0, matchesPlayed = 0))
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
