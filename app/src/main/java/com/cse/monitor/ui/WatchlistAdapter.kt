package com.cse.monitor.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cse.monitor.R
import com.cse.monitor.databinding.ItemStockBinding
import com.cse.monitor.model.WatchedStock
import java.text.NumberFormat
import java.util.Locale

class WatchlistAdapter(
    private val onItemClick: (WatchedStock) -> Unit,
    private val onItemLongClick: (WatchedStock) -> Unit
) : ListAdapter<WatchedStock, WatchlistAdapter.StockViewHolder>(DIFF) {

    private val fmt = NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 2 }

    inner class StockViewHolder(private val binding: ItemStockBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: WatchedStock) {
            binding.tvSymbol.text = stock.symbol.substringBefore(".")
            binding.tvName.text = stock.name
            binding.tvPrice.text = "LKR ${fmt.format(stock.lastTradedPrice)}"

            val isUp = stock.change >= 0
            val sign = if (isUp) "▲" else "▼"
            val color = if (isUp)
                binding.root.context.getColor(R.color.gain_green)
            else
                binding.root.context.getColor(R.color.loss_red)

            binding.tvChange.apply {
                text = "$sign ${fmt.format(stock.change)} (${fmt.format(stock.changePercentage)}%)"
                setTextColor(color)
            }

            if (stock.isWidget) {
                binding.ivWidget.visibility = android.view.View.VISIBLE
            } else {
                binding.ivWidget.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(stock) }
            binding.root.setOnLongClickListener { onItemLongClick(stock); true }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemStockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WatchedStock>() {
            override fun areItemsTheSame(a: WatchedStock, b: WatchedStock) = a.symbol == b.symbol
            override fun areContentsTheSame(a: WatchedStock, b: WatchedStock) = a == b
        }
    }
}
