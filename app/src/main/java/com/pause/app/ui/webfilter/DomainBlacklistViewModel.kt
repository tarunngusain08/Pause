package com.pause.app.ui.webfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.BlacklistedDomain
import com.pause.app.data.repository.BlacklistRepository
import com.pause.app.service.webfilter.url.URLNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DomainBlacklistUiState(
    val domains: List<BlacklistedDomain> = emptyList(),
    val pendingReview: List<BlacklistedDomain> = emptyList(),
    val addDomainInput: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class DomainBlacklistViewModel @Inject constructor(
    private val blacklistRepository: BlacklistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DomainBlacklistUiState())
    val uiState: StateFlow<DomainBlacklistUiState> = _uiState.asStateFlow()

    init {
        loadDomains()
    }

    fun loadDomains() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allDomains = blacklistRepository.getActiveDomainsAsList()
            val allPending = blacklistRepository.getPendingReview()
            val customDomains = allDomains.filter { it.source == "MANUAL" || it.source == "AUTO_KEYWORD" }
            val customPending = allPending.filter { it.source == "MANUAL" || it.source == "AUTO_KEYWORD" }
            _uiState.update {
                it.copy(domains = customDomains, pendingReview = customPending, isLoading = false)
            }
        }
    }

    fun setAddDomainInput(input: String) {
        _uiState.update { it.copy(addDomainInput = input) }
    }

    fun addDomain() {
        val input = _uiState.value.addDomainInput.trim()
        if (input.isBlank()) return
        viewModelScope.launch {
            val domain = URLNormalizer.extractDomain(input) ?: input
                .lowercase().removePrefix("www.").trim()
            if (domain.isNotBlank()) {
                blacklistRepository.addDomain(
                    BlacklistedDomain(
                        domain = domain,
                        source = "MANUAL",
                        isActive = true,
                        addedAt = System.currentTimeMillis(),
                        addedBy = "PARENT",
                        category = "CUSTOM"
                    )
                )
                _uiState.update { it.copy(addDomainInput = "") }
                loadDomains()
            }
        }
    }

    fun removeDomain(id: Long) {
        viewModelScope.launch {
            blacklistRepository.removeById(id)
            loadDomains()
        }
    }
}
