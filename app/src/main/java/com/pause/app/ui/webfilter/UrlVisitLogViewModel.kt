package com.pause.app.ui.webfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.UrlVisitLog
import com.pause.app.data.repository.UrlVisitLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UrlVisitLogUiState(
    val logs: List<UrlVisitLog> = emptyList(),
    val filter: String = "all", // all | blocked | keyword | pending
    val isLoading: Boolean = false
)

@HiltViewModel
class UrlVisitLogViewModel @Inject constructor(
    private val urlVisitLogRepository: UrlVisitLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlVisitLogUiState())
    val uiState: StateFlow<UrlVisitLogUiState> = _uiState.asStateFlow()

    init {
        loadLogs()
    }

    fun loadLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val logs = urlVisitLogRepository.getRecent(days = 7)
            _uiState.update { it.copy(logs = logs, isLoading = false) }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun markReviewed(id: Long) {
        viewModelScope.launch {
            urlVisitLogRepository.markReviewed(id)
            loadLogs()
        }
    }

    fun filteredLogs(): List<UrlVisitLog> {
        val logs = _uiState.value.logs
        return when (_uiState.value.filter) {
            "blocked" -> logs.filter { it.wasBlocked }
            "keyword" -> logs.filter { it.classification == "KEYWORD_MATCH" }
            "pending" -> logs.filter { !it.parentReviewed }
            else -> logs
        }
    }
}
