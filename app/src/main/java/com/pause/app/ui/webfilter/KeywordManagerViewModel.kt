package com.pause.app.ui.webfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.KeywordEntry
import com.pause.app.data.repository.KeywordRepository
import com.pause.app.data.repository.WebFilterConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeywordManagerUiState(
    val keywords: List<KeywordEntry> = emptyList(),
    val customKeywordInput: String = "",
    val autoBlacklistOnMatch: Boolean = false,
    val bundledCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class KeywordManagerViewModel @Inject constructor(
    private val keywordRepository: KeywordRepository,
    private val webFilterConfigRepository: WebFilterConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeywordManagerUiState())
    val uiState: StateFlow<KeywordManagerUiState> = _uiState.asStateFlow()

    init {
        loadKeywords()
    }

    fun loadKeywords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val keywords = keywordRepository.getActiveKeywords()
            val config = webFilterConfigRepository.getConfig()
            val bundledCount = keywordRepository.getBundledCount()
            _uiState.update {
                it.copy(
                    keywords = keywords,
                    autoBlacklistOnMatch = config?.autoBlacklistOnKeywordMatch == true,
                    bundledCount = bundledCount,
                    isLoading = false
                )
            }
        }
    }

    fun setCustomKeywordInput(input: String) {
        _uiState.update { it.copy(customKeywordInput = input) }
    }

    fun addCustomKeyword() {
        val input = _uiState.value.customKeywordInput.trim().lowercase()
        if (input.isBlank()) return
        viewModelScope.launch {
            keywordRepository.addKeyword(
                KeywordEntry(
                    keyword = input,
                    category = "CUSTOM",
                    isActive = true,
                    isBundled = false,
                    addedAt = System.currentTimeMillis()
                )
            )
            _uiState.update { it.copy(customKeywordInput = "") }
            loadKeywords()
        }
    }

    fun removeKeyword(id: Long) {
        viewModelScope.launch {
            keywordRepository.removeById(id)
            loadKeywords()
        }
    }

    fun setAutoBlacklistOnMatch(enabled: Boolean) {
        viewModelScope.launch {
            webFilterConfigRepository.saveConfig(
                (webFilterConfigRepository.getConfig() ?: com.pause.app.data.db.entity.WebFilterConfig(id = 1))
                    .copy(autoBlacklistOnKeywordMatch = enabled)
            )
            loadKeywords()
        }
    }
}
