package com.cse.monitor.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cse.monitor.R
import com.cse.monitor.data.StockRepository
import com.cse.monitor.databinding.ActivityWidgetConfigBinding
import com.cse.monitor.model.WatchedStock
import com.cse.monitor.ui.WatchlistAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Shown when user adds the widget to their home screen.
 * Lets them choose which watched stock to display.
 */
class StockWidgetConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWidgetConfigBinding
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var repository: StockRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWidgetConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get widget ID from intent
        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        repository = StockRepository(this)

        // Default result = cancelled (user might back out)
        setResult(RESULT_CANCELED)

        loadWatchlist()
    }

    private fun loadWatchlist() {
        lifecycleScope.launch {
            val stocks = repository.watchedStocks.first()
            if (stocks.isEmpty()) {
                Toast.makeText(this@StockWidgetConfigActivity,
                    "Add stocks in the app first!", Toast.LENGTH_LONG).show()
                finish()
                return@launch
            }

            val adapter = WatchlistAdapter(
                onItemClick = { stock -> pinAndFinish(stock) },
                onItemLongClick = {}
            )
            binding.rvStocks.layoutManager = LinearLayoutManager(this@StockWidgetConfigActivity)
            binding.rvStocks.adapter = adapter
            adapter.submitList(stocks)

            binding.tvTitle.text = "Choose stock for widget"
        }
    }

    private fun pinAndFinish(stock: WatchedStock) {
        lifecycleScope.launch {
            repository.setWidgetStock(stock.symbol)

            val manager = AppWidgetManager.getInstance(this@StockWidgetConfigActivity)
            StockWidgetProvider.updateWidget(this@StockWidgetConfigActivity, manager, appWidgetId)

            val resultIntent = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}
