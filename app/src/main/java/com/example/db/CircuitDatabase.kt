package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import android.content.Context
import androidx.room.Room

@Entity(tableName = "schemes")
data class CircuitScheme(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gridData: String,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SchemeDao {
    @Query("SELECT * FROM schemes ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CircuitScheme>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheme: CircuitScheme)

    @Query("DELETE FROM schemes WHERE id = :id")
    suspend fun delete(id: Int)
}

@Database(entities = [CircuitScheme::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schemeDao(): SchemeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // We use fallbackToDestructiveMigration because we added more properties to the scheme schema
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "circuit_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class CircuitRepository(private val schemeDao: SchemeDao) {
    val allSchemes: Flow<List<CircuitScheme>> = schemeDao.getAll()

    suspend fun insert(scheme: CircuitScheme) = schemeDao.insert(scheme)

    suspend fun deleteById(id: Int) = schemeDao.delete(id)
}
