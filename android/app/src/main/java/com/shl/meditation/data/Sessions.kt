package com.shl.meditation.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One recorded meditation. */
@Entity(tableName = "sessions")
data class MeditationSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val durationSeconds: Int,
)

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: MeditationSession): Long

    /** Used when a sit carries on past the bell and the total grows. */
    @Update
    suspend fun update(session: MeditationSession)

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM sessions")
    fun observeTotalSeconds(): Flow<Int>
}

@Database(entities = [MeditationSession::class], version = 2)
abstract class MeditationDatabase : RoomDatabase() {

    abstract fun sessions(): SessionDao

    companion object {
        @Volatile
        private var instance: MeditationDatabase? = null

        fun get(context: Context): MeditationDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeditationDatabase::class.java,
                    "meditation.db",
                )
                    // No destructive fallback on purpose. Changing the columns
                    // without writing a Migration will now crash on the next
                    // launch during development, which is a great deal better
                    // than silently deleting months of someone's practice on
                    // their phone.
                    .build()
                    .also { instance = it }
            }
    }
}
