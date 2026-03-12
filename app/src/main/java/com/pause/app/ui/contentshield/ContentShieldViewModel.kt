package com.pause.app.ui.contentshield

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.AssetImporter
import com.pause.app.data.repository.BlacklistRepository
import com.pause.app.data.repository.KeywordRepository
import com.pause.app.data.repository.WebFilterConfigRepository
import com.pause.app.service.contentshield.ContentShieldManager
import com.pause.app.service.webfilter.PauseVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ContentShieldUiState(
    val adultFilterEnabled: Boolean = false,
    val socialMediaFilterEnabled: Boolean = false,
    val blockedDomainsCount: Int = 0,
    val blockedKeywordsCount: Int = 0,
    val socialMediaApps: List<SocialMediaAppItem> = emptyList()
)

data class SocialMediaAppItem(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean
)

@HiltViewModel
class ContentShieldViewModel @Inject constructor(
    private val contentShieldManager: ContentShieldManager,
    private val webFilterConfigRepository: WebFilterConfigRepository,
    private val blacklistRepository: BlacklistRepository,
    private val keywordRepository: KeywordRepository,
    private val assetImporter: AssetImporter,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentShieldUiState())
    val uiState: StateFlow<ContentShieldUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                contentShieldManager.adultFilterEnabled,
                contentShieldManager.socialMediaFilterEnabled
            ) { adult, social ->
                Pair(adult, social)
            }.collect { (adult, social) ->
                _uiState.update {
                    it.copy(
                        adultFilterEnabled = adult,
                        socialMediaFilterEnabled = social
                    )
                }
            }
        }
        viewModelScope.launch {
            blacklistRepository.getActiveDomains().collect { domains ->
                _uiState.update { it.copy(blockedDomainsCount = domains.size) }
            }
        }
        viewModelScope.launch {
            keywordRepository.getActiveKeywordsFlow().collect { keywords ->
                _uiState.update { it.copy(blockedKeywordsCount = keywords.size) }
            }
        }
        viewModelScope.launch {
            combine(
                contentShieldManager.excludedSocialMediaPackages,
                contentShieldManager.socialMediaFilterEnabled
            ) { excluded, socialEnabled ->
                if (!socialEnabled) emptyList()
                else {
                    contentShieldManager.getSocialMediaPackages().map { pkg ->
                        val appName = try {
                            val appInfo = packageManager.getApplicationInfo(pkg, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (_: Exception) {
                            pkg
                        }
                        SocialMediaAppItem(
                            packageName = pkg,
                            appName = appName,
                            isBlocked = pkg !in excluded
                        )
                    }
                }
            }.collect { apps ->
                _uiState.update { it.copy(socialMediaApps = apps) }
            }
        }
    }

    fun toggleSocialMediaExclusion(packageName: String) {
        viewModelScope.launch {
            val excluded = contentShieldManager.excludedSocialMediaPackages.first()
            val newExcluded = if (packageName in excluded) {
                excluded - packageName
            } else {
                excluded + packageName
            }
            contentShieldManager.setExcludedSocialMediaPackages(newExcluded)
        }
    }

    fun setAdultFilterEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            contentShieldManager.setAdultFilterEnabled(enabled)
            if (enabled) {
                assetImporter.importBundledAssetsIfNeeded()
                webFilterConfigRepository.setVpnEnabled(true)
                webFilterConfigRepository.setUrlReaderEnabled(true)
                webFilterConfigRepository.setKeywordFilterEnabled(true)
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent == null) {
                    val svcIntent = Intent(context, PauseVpnService::class.java)
                        .setAction(PauseVpnService.ACTION_START)
                    context.startForegroundService(svcIntent)
                } else {
                    context.startActivity(prepareIntent)
                }
            } else {
                webFilterConfigRepository.setKeywordFilterEnabled(false)
                webFilterConfigRepository.setVpnEnabled(false)
                if (!contentShieldManager.socialMediaFilterEnabled.first()) {
                    webFilterConfigRepository.setUrlReaderEnabled(false)
                }
                val svcIntent = Intent(context, PauseVpnService::class.java)
                    .setAction(PauseVpnService.ACTION_STOP)
                context.startService(svcIntent)
            }
        }
    }

    fun setSocialMediaFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            contentShieldManager.setSocialMediaFilterEnabled(enabled)
            if (enabled) {
                assetImporter.importBundledAssetsIfNeeded()
                webFilterConfigRepository.setUrlReaderEnabled(true)
            } else if (!contentShieldManager.adultFilterEnabled.first()) {
                webFilterConfigRepository.setUrlReaderEnabled(false)
            }
        }
    }

    fun onResume(context: Context) {
        viewModelScope.launch {
            if (_uiState.value.adultFilterEnabled && VpnService.prepare(context) == null) {
                val intent = Intent(context, PauseVpnService::class.java)
                    .setAction(PauseVpnService.ACTION_START)
                context.startForegroundService(intent)
            }
        }
    }
}
