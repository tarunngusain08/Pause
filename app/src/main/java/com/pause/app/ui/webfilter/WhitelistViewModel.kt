package com.pause.app.ui.webfilter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.db.entity.WhitelistedDomain
import com.pause.app.data.repository.WhitelistRepository
import com.pause.app.service.webfilter.url.URLNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WhitelistUiState(
    val domains: List<WhitelistedDomain> = emptyList(),
    val addDomainInput: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class WhitelistViewModel @Inject constructor(
    private val whitelistRepository: WhitelistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhitelistUiState())
    val uiState: StateFlow<WhitelistUiState> = _uiState.asStateFlow()

    init {
        loadDomains()
    }

    fun loadDomains() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val domains = whitelistRepository.getAllAsList()
            _uiState.update { it.copy(domains = domains, isLoading = false) }
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
                whitelistRepository.addDomain(
                    WhitelistedDomain(
                        domain = domain,
                        addedAt = System.currentTimeMillis()
                    )
                )
                _uiState.update { it.copy(addDomainInput = "") }
                loadDomains()
            }
        }
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            whitelistRepository.removeByDomain(domain)
            loadDomains()
        }
    }
}
