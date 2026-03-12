package com.pause.app.ui.contentshield

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.service.contentshield.ContentShieldManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ContentShieldUiState(
    val socialMediaFilterEnabled: Boolean = false,
    val socialMediaApps: List<SocialMediaAppItem> = emptyList(),
    val isLoadingApps: Boolean = true
)

data class SocialMediaAppItem(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean,
    /** True for both built-in social media defaults and user-added custom social apps. */
    val isSocial: Boolean
)

@HiltViewModel
class ContentShieldViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentShieldManager: ContentShieldManager,
    private val packageManager: PackageManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentShieldUiState())
    val uiState: StateFlow<ContentShieldUiState> = _uiState.asStateFlow()

    private val exclusionMutex = Mutex()

    /**
     * Frozen snapshot of the social-media package set, captured on first emission.
     * Used for UI section classification so apps don't jump between "Social" and "Other"
     * mid-session. Cleared by [refreshSnapshot] when the user navigates to the screen.
     */
    @Volatile
    private var snapshotSocialPackages: Set<String>? = null

    private val systemExcludePackages = setOf(
        context.packageName,
        "com.android.systemui",
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher"
    )

    init {
        viewModelScope.launch {
            combine(
                contentShieldManager.socialMediaFilterEnabled,
                contentShieldManager.excludedSocialMediaPackages,
                contentShieldManager.socialMediaPackages,
                contentShieldManager.customSocialMediaPackages
            ) { socialEnabled, excluded, defaultPlusCustom, _ ->
                // On first emission (or after refreshSnapshot), lock the section classification
                if (snapshotSocialPackages == null) {
                    snapshotSocialPackages = defaultPlusCustom
                }
                val apps = withContext(Dispatchers.IO) {
                    buildUnifiedAppList(excluded, defaultPlusCustom, snapshotSocialPackages!!)
                }
                ContentShieldUiState(
                    socialMediaFilterEnabled = socialEnabled,
                    socialMediaApps = apps,
                    isLoadingApps = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /**
     * Clears the frozen snapshot so the next flow emission picks up current membership.
     * Should be called when the user navigates to the Social Media Filter screen.
     */
    fun refreshSnapshot() {
        snapshotSocialPackages = null
    }

    /**
     * Builds a unified list of ALL installed launcher apps.
     * [sectionClassification] determines which section an app appears in (frozen snapshot).
     * [liveDefaultPlusCustom] is used only to compute [SocialMediaAppItem.isBlocked] for
     * newly-toggled apps that are in the live set but not yet in the snapshot.
     */
    private fun buildUnifiedAppList(
        excluded: Set<String>,
        liveDefaultPlusCustom: Set<String>,
        sectionClassification: Set<String>
    ): List<SocialMediaAppItem> {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val installedPkgs = packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .filter { it !in systemExcludePackages }
            .toSet()

        fun resolveAppName(pkg: String): String? = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

        // Section placement comes from the snapshot; blocked state from the live data.
        val socialItems = sectionClassification
            .filter { it in installedPkgs }
            .mapNotNull { pkg ->
                val name = resolveAppName(pkg) ?: return@mapNotNull null
                SocialMediaAppItem(
                    packageName = pkg,
                    appName = name,
                    isBlocked = pkg in liveDefaultPlusCustom && pkg !in excluded,
                    isSocial = true
                )
            }
            .sortedBy { it.appName.lowercase() }

        val otherItems = installedPkgs
            .filter { it !in sectionClassification }
            .mapNotNull { pkg ->
                val name = resolveAppName(pkg) ?: return@mapNotNull null
                val inLiveSet = pkg in liveDefaultPlusCustom
                SocialMediaAppItem(
                    packageName = pkg,
                    appName = name,
                    isBlocked = inLiveSet && pkg !in excluded,
                    isSocial = false
                )
            }
            .sortedBy { it.appName.lowercase() }

        return socialItems + otherItems
    }

    /**
     * Toggles blocking for any app:
     * - Built-in default: toggle via excluded set
     * - Custom app currently blocked: add to excluded (unblock) — keeps it in custom so it stays in social section
     * - Custom app currently excluded: remove from excluded (re-block)
     * - Non-social app not tracked: add to custom (block)
     */
    fun toggleAppBlocking(packageName: String) {
        viewModelScope.launch {
            exclusionMutex.withLock {
                val excluded = contentShieldManager.excludedSocialMediaPackages.first()
                val custom = contentShieldManager.customSocialMediaPackages.first()
                val defaultPlusCustom = contentShieldManager.socialMediaPackages.first()
                val isBuiltInDefault = packageName in defaultPlusCustom && packageName !in custom

                when {
                    isBuiltInDefault -> {
                        val newExcluded = if (packageName in excluded) {
                            excluded - packageName
                        } else {
                            excluded + packageName
                        }
                        contentShieldManager.setExcludedSocialMediaPackages(newExcluded)
                    }
                    packageName in custom -> {
                        // Toggle blocked/unblocked via excluded set; keep in custom so it stays
                        // in the social section until the snapshot refreshes.
                        val newExcluded = if (packageName in excluded) {
                            excluded - packageName
                        } else {
                            excluded + packageName
                        }
                        contentShieldManager.setExcludedSocialMediaPackages(newExcluded)
                    }
                    else -> {
                        contentShieldManager.addCustomSocialMediaPackage(packageName)
                    }
                }
            }
        }
    }

    fun setSocialMediaFilterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            contentShieldManager.setSocialMediaFilterEnabled(enabled)
        }
    }
}
