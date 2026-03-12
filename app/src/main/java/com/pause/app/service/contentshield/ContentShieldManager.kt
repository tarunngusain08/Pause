package com.pause.app.service.contentshield

import com.pause.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Social media app package names to block when Social Media Filter is enabled. */
private val SOCIAL_MEDIA_PACKAGES = setOf(
    "com.instagram.android",
    "com.twitter.android",
    "com.facebook.katana",
    "com.facebook.orca",
    "com.snapchat.android",
    "com.zhiliaoapp.musically", // TikTok (international)
    "com.ss.android.ugc.trill", // TikTok (regional variant)
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
    val socialMediaFilterEnabled: Flow<Boolean> = preferencesManager.socialMediaFilterEnabled

    suspend fun setSocialMediaFilterEnabled(enabled: Boolean) {
        preferencesManager.setSocialMediaFilterEnabled(enabled)
    }

    val socialMediaPackages: Flow<Set<String>> = preferencesManager.customSocialMediaPackages.map { custom ->
        SOCIAL_MEDIA_PACKAGES + custom
    }

    suspend fun isPackageBlocked(packageName: String): Boolean {
        if (!preferencesManager.socialMediaFilterEnabled.first()) return false
        val allPackages = SOCIAL_MEDIA_PACKAGES + preferencesManager.getCustomSocialMediaPackages()
        if (packageName !in allPackages) return false
        val excluded = preferencesManager.excludedSocialMediaPackages.first()
        return packageName !in excluded
    }

    suspend fun addCustomSocialMediaPackage(packageName: String) {
        preferencesManager.addCustomSocialMediaPackage(packageName)
    }

    suspend fun removeCustomSocialMediaPackage(packageName: String) {
        preferencesManager.removeCustomSocialMediaPackage(packageName)
    }

    val excludedSocialMediaPackages: Flow<Set<String>> =
        preferencesManager.excludedSocialMediaPackages

    val customSocialMediaPackages: Flow<Set<String>> =
        preferencesManager.customSocialMediaPackages

    suspend fun setExcludedSocialMediaPackages(packages: Set<String>) {
        preferencesManager.setExcludedSocialMediaPackages(packages)
    }
}
