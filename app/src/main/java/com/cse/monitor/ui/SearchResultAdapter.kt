package com.cse.monitor.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cse.monitor.databinding.ItemSearchResultBinding

class SearchResultAdapter(
    private val onItemClick: (symbol: String, name: String) -> Unit
) : ListAdapter<Pair<String, String>, SearchResultAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemSearchResultBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pair<String, String>) {
            binding.tvResultSymbol.text = item.first.substringBefore(".")
            binding.tvResultName.text = item.second
            binding.root.setOnClickListener { onItemClick(item.first, item.second) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pair<String, String>>() {
            override fun areItemsTheSame(a: Pair<String, String>, b: Pair<String, String>) =
                a.first == b.first
            override fun areContentsTheSame(a: Pair<String, String>, b: Pair<String, String>) =
                a == b
        }
    }
}
