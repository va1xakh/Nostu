package com.example.searchnostalgia

import android.content.Context
import androidx.room.*

@Entity(
    tableName = "searches",
    indices = [Index(value = ["month", "day"])]
)
data class SearchRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long,
    val year: Int,
    val month: Int,
    val day: Int
)

@Dao
interface SearchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(records: List<SearchRecord>)

    @Query("SELECT * FROM searches WHERE month = :month AND day = :day AND year < :currentYear ORDER BY year ASC")
    suspend fun getOnThisDay(month: Int, day: Int, currentYear: Int): List<SearchRecord>

    @Query("SELECT * FROM searches ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomSearch(): SearchRecord?

    @Query("SELECT COUNT(*) FROM searches")
    suspend fun getTotalCount(): Int
}

@Database(entities = [SearchRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun searchDao(): SearchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "search_history.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
