package com.cse.monitor.ui

import android.app.Application
import androidx.lifecycle.*
import com.cse.monitor.data.StockRepository
import com.cse.monitor.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StockDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StockRepository(application)

    private val _uiState = MutableLiveData(StockUiState())
    val uiState: LiveData<StockUiState> = _uiState

    private var autoRefreshJob: Job? = null

    fun load(symbol: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isLoading = true)

            // Fetch stock price
            repository.refreshStock(symbol).onSuccess { stock ->
                _uiState.value = _uiState.value?.copy(stock = stock, isLoading = false, error = null)
            }.onFailure { e ->
                _uiState.value = _uiState.value?.copy(isLoading = false, error = e.message)
            }

            // Fetch intraday chart data
            repository.getIntradayData(symbol).onSuccess { points ->
                _uiState.value = _uiState.value?.copy(intradayPoints = points)
            }

            // Market summary
            repository.getMarketSummary().onSuccess { summary ->
                _uiState.value = _uiState.value?.copy(marketSummary = summary)
            }
        }
    }

    /** Start auto-refresh every 30 s while detail screen is visible */
    fun startAutoRefresh(symbol: String) {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000L)
                repository.refreshStock(symbol).onSuccess { stock ->
                    _uiState.value = _uiState.value?.copy(stock = stock)
                }
                repository.getIntradayData(symbol).onSuccess { points ->
                    _uiState.value = _uiState.value?.copy(intradayPoints = points)
                }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun setAsWidget(symbol: String) {
        viewModelScope.launch {
            repository.setWidgetStock(symbol)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
