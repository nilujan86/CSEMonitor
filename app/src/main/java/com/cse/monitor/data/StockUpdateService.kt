package com.cse.monitor.data

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.cse.monitor.widget.StockWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground-lite service that periodically refreshes watched stocks
 * and triggers widget updates. CSE trades Mon–Fri 09:30–14:30 IST.
 * We refresh every 60 s during market hours, 5 min otherwise.
 */
class StockUpdateService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: StockRepository

    override fun onCreate() {
        super.onCreate()
        repository = StockRepository(applicationContext)
        startRefreshLoop()
    }

    private fun startRefreshLoop() {
        scope.launch {
            while (isActive) {
                refreshAll()
                val delayMs = if (isMarketHours()) 60_000L else 300_000L
                delay(delayMs)
            }
        }
    }

    private suspend fun refreshAll() {
        val stocks = repository.watchedStocks.first()
        stocks.forEach { stock ->
            repository.refreshStock(stock.symbol)
        }
        notifyWidgets()
    }

    private fun notifyWidgets() {
        val manager = AppWidgetManager.getInstance(this)
        val ids = manager.getAppWidgetIds(
            ComponentName(this, StockWidgetProvider::class.java)
        )
        if (ids.isNotEmpty()) {
            val intent = Intent(this, StockWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
    }

    private fun isMarketHours(): Boolean {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Colombo"))
        val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val min = cal.get(java.util.Calendar.MINUTE)
        val timeMinutes = hour * 60 + min
        val isWeekday = day in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
        return isWeekday && timeMinutes in (9 * 60 + 30)..(14 * 60 + 30)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
