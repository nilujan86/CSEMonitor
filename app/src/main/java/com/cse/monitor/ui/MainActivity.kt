package com.cse.monitor.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.cse.monitor.R
import com.cse.monitor.data.StockUpdateService
import com.cse.monitor.databinding.ActivityMainBinding
import com.cse.monitor.model.CSE_POPULAR_STOCKS
import com.cse.monitor.model.WatchedStock
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var watchlistAdapter: WatchlistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupSearch()
        setupFab()
        observeViewModel()

        // Start background refresh service
        startService(Intent(this, StockUpdateService::class.java))
    }

    private fun setupRecyclerView() {
        watchlistAdapter = WatchlistAdapter(
            onItemClick = { stock -> openDetail(stock) },
            onItemLongClick = { stock -> showRemoveDialog(stock) }
        )
        binding.rvWatchlist.apply {
            adapter = watchlistAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupSearch() {
        // Preload popular stocks in dropdown
        viewModel.searchSymbols("")

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.searchSymbols(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener { showAddStockDialog() }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshAll()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.watchedStocks.collect { stocks ->
                    watchlistAdapter.submitList(stocks)
                    binding.tvEmptyState.visibility =
                        if (stocks.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.uiMessage.observe(this) { msg ->
            msg?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        viewModel.marketSummary.observe(this) { summary ->
            summary?.let {
                val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }
                binding.tvAspi.text = "ASPI: ${fmt.format(it.aspi ?: 0.0)}"
                val sign = if ((it.aspiChange ?: 0.0) >= 0) "▲" else "▼"
                val color = if ((it.aspiChange ?: 0.0) >= 0)
                    getColor(R.color.gain_green) else getColor(R.color.loss_red)
                binding.tvAspiChange.apply {
                    text = "$sign ${fmt.format(it.aspiChange ?: 0.0)} (${fmt.format(it.aspiChangePercentage ?: 0.0)}%)"
                    setTextColor(color)
                }
            }
        }
    }

    private fun showAddStockDialog() {
        val symbols = CSE_POPULAR_STOCKS.map { "${it.first} — ${it.second}" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Add Stock to Watchlist")
            .setItems(symbols) { _, idx ->
                val symbol = CSE_POPULAR_STOCKS[idx].first
                viewModel.addToWatchlist(symbol)
            }
            .setNeutralButton("Enter symbol manually") { _, _ ->
                showManualSymbolDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showManualSymbolDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "e.g. LOLC.N0000"
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Enter CSE Symbol")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val sym = input.text.toString().trim().uppercase()
                if (sym.isNotEmpty()) viewModel.addToWatchlist(sym)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showRemoveDialog(stock: WatchedStock) {
        AlertDialog.Builder(this)
            .setTitle("Remove ${stock.symbol}?")
            .setMessage("Remove ${stock.name} from watchlist?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeFromWatchlist(stock.symbol)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openDetail(stock: WatchedStock) {
        val intent = Intent(this, StockDetailActivity::class.java).apply {
            putExtra(StockDetailActivity.EXTRA_SYMBOL, stock.symbol)
            putExtra(StockDetailActivity.EXTRA_NAME, stock.name)
        }
        startActivity(intent)
    }
}
