package com.pause.app.service.contentshield

import com.pause.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Social media app package names to block when Social Media Filter is enabled. */
private val SOCIAL_MEDIA_PACKAGES = setOf(
    "com.instagram.android",
    "com.twitter.android",
    "com.facebook.katana",
    "com.facebook.orca",
    "com.snapchat.android",
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.reddit.frontpage",
    "com.pinterest",
    "com.linkedin.android",
    "com.discord",
    "com.tumblr",
    "com.bereal.ft",
    "org.telegram.messenger",
    "com.whatsapp",
    "com.google.android.youtube"
)

@Singleton
class ContentShieldManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    val adultFilterEnabled: Flow<Boolean> = preferencesManager.adultFilterEnabled
    val socialMediaFilterEnabled: Flow<Boolean> = preferencesManager.socialMediaFilterEnabled

    suspend fun isAdultFilterEnabled(): Boolean = preferencesManager.adultFilterEnabled.first()
    suspend fun isSocialMediaFilterEnabled(): Boolean = preferencesManager.socialMediaFilterEnabled.first()

    suspend fun setAdultFilterEnabled(enabled: Boolean) {
        preferencesManager.setAdultFilterEnabled(enabled)
    }

    suspend fun setSocialMediaFilterEnabled(enabled: Boolean) {
        preferencesManager.setSocialMediaFilterEnabled(enabled)
    }

    suspend fun isPackageBlocked(packageName: String): Boolean {
        if (!preferencesManager.socialMediaFilterEnabled.first()) return false
        if (packageName !in SOCIAL_MEDIA_PACKAGES) return false
        val excluded = preferencesManager.excludedSocialMediaPackages.first()
        return packageName !in excluded
    }

    fun getSocialMediaPackages(): Set<String> = SOCIAL_MEDIA_PACKAGES

    fun getBlockedSocialMediaApps(): Flow<Set<String>> = combine(
        preferencesManager.socialMediaFilterEnabled,
        preferencesManager.excludedSocialMediaPackages
    ) { enabled, excluded ->
        if (!enabled) emptySet()
        else SOCIAL_MEDIA_PACKAGES - excluded
    }

    val excludedSocialMediaPackages: Flow<Set<String>> =
        preferencesManager.excludedSocialMediaPackages

    suspend fun setExcludedSocialMediaPackages(packages: Set<String>) {
        preferencesManager.setExcludedSocialMediaPackages(packages)
    }
}
