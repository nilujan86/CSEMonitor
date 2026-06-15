package com.cse.monitor.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cse.monitor.model.WatchedStock
import kotlinx.coroutines.flow.Flow

// ── DAO ───────────────────────────────────────────────────────────────────────

@Dao
interface WatchedStockDao {

    @Query("SELECT * FROM watched_stocks ORDER BY displayOrder ASC, name ASC")
    fun getAllWatched(): Flow<List<WatchedStock>>

    @Query("SELECT * FROM watched_stocks WHERE isWidget = 1 LIMIT 1")
    suspend fun getWidgetStock(): WatchedStock?

    @Query("SELECT * FROM watched_stocks WHERE symbol = :symbol LIMIT 1")
    suspend fun getBySymbol(symbol: String): WatchedStock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stock: WatchedStock)

    @Delete
    suspend fun delete(stock: WatchedStock)

    @Query("DELETE FROM watched_stocks WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("UPDATE watched_stocks SET isWidget = 0")
    suspend fun clearWidgetFlag()

    @Query("UPDATE watched_stocks SET isWidget = 1 WHERE symbol = :symbol")
    suspend fun setWidgetStock(symbol: String)

    @Query("UPDATE watched_stocks SET lastTradedPrice = :price, change = :change, " +
           "changePercentage = :changePct, lastUpdated = :ts WHERE symbol = :symbol")
    suspend fun updatePrice(symbol: String, price: Double, change: Double,
                            changePct: Double, ts: Long)
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(entities = [WatchedStock::class], version = 1, exportSchema = false)
abstract class StockDatabase : RoomDatabase() {

    abstract fun watchedStockDao(): WatchedStockDao

    companion object {
        @Volatile private var INSTANCE: StockDatabase? = null

        fun getInstance(context: Context): StockDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockDatabase::class.java,
                    "cse_stocks.db"
                ).build().also { INSTANCE = it }
            }
    }
}
