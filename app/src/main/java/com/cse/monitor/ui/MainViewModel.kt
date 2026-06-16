package com.cse.monitor.ui

import android.app.Application
import androidx.lifecycle.*
import com.cse.monitor.data.StockRepository
import com.cse.monitor.model.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StockRepository(application)

    // All watched stocks – reactive Room flow
    val watchedStocks = repository.watchedStocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiMessage = MutableLiveData<String?>()
    val uiMessage: LiveData<String?> = _uiMessage

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _marketSummary = MutableLiveData<MarketSummaryResponse?>()
    val marketSummary: LiveData<MarketSummaryResponse?> = _marketSummary

    private val _searchResults = MutableLiveData<List<Pair<String, String>>>()
    val searchResults: LiveData<List<Pair<String, String>>> = _searchResults

    init {
        loadMarketSummary()
        searchSymbols("")  
    }

    fun addToWatchlist(symbol: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addToWatchlist(symbol)
            result.onSuccess { stock ->
                _uiMessage.value = "${stock.name} added to watchlist"
            }.onFailure { e ->
                _uiMessage.value = "Failed to add $symbol: ${e.message}"
            }
            _isLoading.value = false
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
            _uiMessage.value = "$symbol removed"
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            watchedStocks.value.forEach { stock ->
                repository.refreshStock(stock.symbol)
            }
            loadMarketSummary()
            _isLoading.value = false
        }
    }

   fun searchSymbols(query: String) {
    viewModelScope.launch {
        _isLoading.value = true
        _searchResults.value = repository.searchSymbols(query)
        _isLoading.value = false
    }
}

    private fun loadMarketSummary() {
        viewModelScope.launch {
            repository.getMarketSummary().onSuccess { summary ->
                _marketSummary.value = summary
            }
        }
    }

    fun clearMessage() { _uiMessage.value = null }
}
