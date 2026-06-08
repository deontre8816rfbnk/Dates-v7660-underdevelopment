package com.example

import android.content.Context
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_dates")
data class SavedDate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
)

@Dao
interface SavedDateDao {
    @Query("SELECT * FROM saved_dates ORDER BY id DESC")
    fun getAllSavedDates(): Flow<List<SavedDate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedDate: SavedDate)

    @Query("DELETE FROM saved_dates WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Database(entities = [SavedDate::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedDateDao(): SavedDateDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chronos_vector_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SavedDateRepository(private val savedDateDao: SavedDateDao) {
    val allSavedDates: Flow<List<SavedDate>> = savedDateDao.getAllSavedDates()

    suspend fun insert(savedDate: SavedDate) {
        savedDateDao.insert(savedDate)
    }

    suspend fun deleteById(id: Int) {
        savedDateDao.deleteById(id)
    }
}
