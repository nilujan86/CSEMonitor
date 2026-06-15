package com.cse.monitor.data

import android.content.Context
import com.cse.monitor.model.*
import kotlinx.coroutines.flow.Flow

class StockRepository(context: Context) {

    private val api = NetworkClient.apiService
    private val dao = StockDatabase.getInstance(context).watchedStockDao()

    // ── Watch-list ────────────────────────────────────────────────────────────

    val watchedStocks: Flow<List<WatchedStock>> = dao.getAllWatched()

    suspend fun addToWatchlist(symbol: String): Result<WatchedStock> {
        return try {
            val response = api.getCompanyInfo(symbol)
            if (response.isSuccessful) {
                val info = response.body()?.symbolInfo
                    ?: return Result.failure(Exception("No data for $symbol"))
                val stock = WatchedStock(
                    symbol = info.symbol,
                    name = info.name,
                    lastTradedPrice = info.lastTradedPrice,
                    change = info.change,
                    changePercentage = info.changePercentage,
                    marketCap = info.marketCap,
                    logoPath = response.body()?.logo?.path
                )
                dao.upsert(stock)
                Result.success(stock)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromWatchlist(symbol: String) = dao.deleteBySymbol(symbol)

    // ── Live price fetch ──────────────────────────────────────────────────────

    suspend fun refreshStock(symbol: String): Result<WatchedStock> {
        return try {
            val response = api.getCompanyInfo(symbol)
            if (response.isSuccessful) {
                val info = response.body()?.symbolInfo
                    ?: return Result.failure(Exception("Empty response"))
                dao.updatePrice(
                    symbol = info.symbol,
                    price = info.lastTradedPrice,
                    change = info.change,
                    changePct = info.changePercentage,
                    ts = System.currentTimeMillis()
                )
                val updated = dao.getBySymbol(symbol)
                    ?: WatchedStock(symbol, info.name, info.lastTradedPrice,
                        info.change, info.changePercentage, info.marketCap, null)
                Result.success(updated)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Intraday chart data ───────────────────────────────────────────────────

    suspend fun getIntradayData(symbol: String): Result<List<IntradayPoint>> {
        return try {
            val response = api.getIntradayData(symbol)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Market summary ────────────────────────────────────────────────────────

    suspend fun getMarketSummary(): Result<MarketSummaryResponse> {
        return try {
            val response = api.getMarketSummary()
            if (response.isSuccessful) {
                Result.success(response.body()
                    ?: return Result.failure(Exception("Empty response")))
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Symbol search ─────────────────────────────────────────────────────────

    suspend fun searchSymbols(query: String): List<Pair<String, String>> {
        return try {
            val response = api.searchSymbols(query)
            if (response.isSuccessful) {
                response.body()?.map { map ->
                    Pair(map["symbol"] ?: "", map["name"] ?: "")
                }?.filter { it.first.isNotEmpty() } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            // Fall back to local list filtered by query
            CSE_POPULAR_STOCKS.filter {
                it.first.contains(query, ignoreCase = true) ||
                it.second.contains(query, ignoreCase = true)
            }
        }
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    suspend fun setWidgetStock(symbol: String) {
        dao.clearWidgetFlag()
        dao.setWidgetStock(symbol)
    }

    suspend fun getWidgetStock(): WatchedStock? = dao.getWidgetStock()
}
