package com.cse.monitor.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cse.monitor.R
import com.cse.monitor.data.StockRepository
import com.cse.monitor.ui.StockDetailActivity
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Home screen widget that shows the currently pinned stock's price,
 * change, and a last-updated timestamp. Tapping opens StockDetailActivity.
 */
class StockWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Refresh every widget instance
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, StockWidgetProvider::class.java)
            )
            ids.forEach { id -> updateWidget(context, manager, id) }
        }
    }

    companion object {
        private val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_stock)

            CoroutineScope(Dispatchers.IO).launch {
                val repository = StockRepository(context)
                val stock = repository.getWidgetStock()

                if (stock != null) {
                    // Refresh from network first
                    repository.refreshStock(stock.symbol)
                    val fresh = repository.getWidgetStock() ?: stock

                    val isUp = fresh.change >= 0
                    val sign = if (isUp) "▲" else "▼"
                    val priceStr = "LKR ${fmt.format(fresh.lastTradedPrice)}"
                    val changeStr = "$sign ${fmt.format(fresh.change)} (${fmt.format(fresh.changePercentage)}%)"
                    val shortName = fresh.name.take(22)
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(java.util.Date(fresh.lastUpdated))

                    withContext(Dispatchers.Main) {
                        views.setTextViewText(R.id.wSymbol, fresh.symbol.substringBefore("."))
                        views.setTextViewText(R.id.wName, shortName)
                        views.setTextViewText(R.id.wPrice, priceStr)
                        views.setTextViewText(R.id.wChange, changeStr)
                        views.setTextViewText(R.id.wUpdated, "Updated $timeStr")

                        val color = if (isUp)
                            context.getColor(R.color.gain_green)
                        else
                            context.getColor(R.color.loss_red)
                        views.setTextColor(R.id.wChange, color)
                        views.setInt(R.id.wChangeBar, "setBackgroundColor",
                            if (isUp) context.getColor(R.color.gain_green_faint)
                            else      context.getColor(R.color.loss_red_faint))

                        // Tap → open detail screen
                        val tapIntent = Intent(context, StockDetailActivity::class.java).apply {
                            putExtra(StockDetailActivity.EXTRA_SYMBOL, fresh.symbol)
                            putExtra(StockDetailActivity.EXTRA_NAME, fresh.name)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val pi = PendingIntent.getActivity(
                            context, appWidgetId, tapIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widgetRoot, pi)

                        // Tap refresh button → broadcast update
                        val refreshIntent = Intent(context, StockWidgetProvider::class.java).apply {
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        }
                        val refreshPi = PendingIntent.getBroadcast(
                            context, appWidgetId + 1000, refreshIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.wBtnRefresh, refreshPi)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } else {
                    // No stock pinned yet
                    views.setTextViewText(R.id.wSymbol, "CSE")
                    views.setTextViewText(R.id.wName, "No stock pinned")
                    views.setTextViewText(R.id.wPrice, "—")
                    views.setTextViewText(R.id.wChange, "Open app to add")
                    withContext(Dispatchers.Main) {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }
    }
}
