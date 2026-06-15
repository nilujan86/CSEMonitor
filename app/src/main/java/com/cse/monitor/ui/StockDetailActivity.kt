package com.cse.monitor.ui

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.cse.monitor.R
import com.cse.monitor.databinding.ActivityStockDetailBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class StockDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SYMBOL = "extra_symbol"
        const val EXTRA_NAME   = "extra_name"
    }

    private lateinit var binding: ActivityStockDetailBinding
    private val viewModel: StockDetailViewModel by viewModels()
    private val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }

    private lateinit var symbol: String
    private lateinit var name: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStockDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        symbol = intent.getStringExtra(EXTRA_SYMBOL) ?: return finish()
        name   = intent.getStringExtra(EXTRA_NAME) ?: symbol

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = name
            subtitle = symbol
        }

        setupChart()
        observeViewModel()
        viewModel.load(symbol)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startAutoRefresh(symbol)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopAutoRefresh()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_stock_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> { finish(); true }
            R.id.action_pin_widget -> {
                viewModel.setAsWidget(symbol)
                Toast.makeText(this, "Pinned to widget: $symbol", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_refresh -> {
                viewModel.load(symbol)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            legend.isEnabled = false

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.GRAY
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#22000000")
                textColor = Color.GRAY
            }
            axisRight.isEnabled = false

            setNoDataText("Loading chart data…")
            setNoDataTextColor(Color.GRAY)
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            state.error?.let {
                Toast.makeText(this, "Error: $it", Toast.LENGTH_LONG).show()
            }

            state.stock?.let { stock ->
                val isUp = stock.change >= 0
                val sign = if (isUp) "▲" else "▼"
                val color = if (isUp) getColor(R.color.gain_green) else getColor(R.color.loss_red)

                binding.tvPrice.text = "LKR ${fmt.format(stock.lastTradedPrice)}"
                binding.tvChange.apply {
                    text = "$sign ${fmt.format(stock.change)}  (${fmt.format(stock.changePercentage)}%)"
                    setTextColor(color)
                }
                stock.marketCap?.let { mc ->
                    binding.tvMarketCap.text = "Market Cap: LKR ${formatLargeNumber(mc)}"
                    binding.tvMarketCap.visibility = View.VISIBLE
                }
                binding.tvLastUpdated.text = "Last updated: ${formatTime(stock.lastUpdated)}"
            }

            if (state.intradayPoints.isNotEmpty()) {
                updateChart(state.intradayPoints)
            }

            state.marketSummary?.let { summary ->
                binding.tvAspi.text = "ASPI: ${fmt.format(summary.aspi ?: 0.0)}"
            }
        }
    }

    private fun updateChart(points: List<com.cse.monitor.model.IntradayPoint>) {
        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.price.toFloat())
        }

        val times = points.map { it.time }

        val dataSet = LineDataSet(entries, "Price").apply {
            color = getColor(R.color.cse_blue)
            setCircleColor(getColor(R.color.cse_blue))
            lineWidth = 2f
            circleRadius = 0f          // no dots for clean look
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillColor = getColor(R.color.cse_blue)
            fillAlpha = 30
            setDrawFilled(true)
        }

        binding.lineChart.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val i = value.toInt()
                    return if (i in times.indices) times[i].takeLast(5) else ""
                }
            }
            xAxis.setLabelCount(5, true)
            animateX(600)
            invalidate()
        }
    }

    private fun formatLargeNumber(n: Long): String = when {
        n >= 1_000_000_000 -> "${fmt.format(n / 1_000_000_000.0)}B"
        n >= 1_000_000     -> "${fmt.format(n / 1_000_000.0)}M"
        else               -> fmt.format(n)
    }

    private fun formatTime(ms: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(java.util.Date(ms))
    }
}
