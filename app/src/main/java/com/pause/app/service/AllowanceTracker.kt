package com.pause.app.service

import android.app.usage.UsageStatsManager
import android.content.Context
import com.pause.app.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.pause.app.data.repository.AppRepository
import com.pause.app.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowanceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var cachedUsageMs: Long = 0L
    @Volatile private var cacheTimestamp: Long = 0L
    @Volatile private var cachePackages: List<String> = emptyList()

    companion object {
        private const val CACHE_TTL_MS = 60_000L
    }

    /** Returns the cached today-usage, refreshing the cache asynchronously if stale. */
    private suspend fun getUsageMs(monitoredPackages: List<String>): Long {
        val now = System.currentTimeMillis()
        if (now - cacheTimestamp > CACHE_TTL_MS || cachePackages != monitoredPackages) {
            // Refresh synchronously the first time; thereafter async so reads stay fast.
            if (cacheTimestamp == 0L) {
                cachedUsageMs = queryUsageStats(monitoredPackages)
                cacheTimestamp = System.currentTimeMillis()
                cachePackages = monitoredPackages
            } else {
                // Schedule a background refresh; use the stale value for this call.
                scope.launch {
                    cachedUsageMs = queryUsageStats(monitoredPackages)
                    cacheTimestamp = System.currentTimeMillis()
                    cachePackages = monitoredPackages
                }
            }
        }
        return cachedUsageMs
    }

    fun invalidateCache() {
        cacheTimestamp = 0L
    }

    suspend fun hasAllowanceReached(): Boolean {
        if (!com.pause.app.util.PermissionHelper.hasUsageStatsPermission(context)) {
            return false
        }
        val allowanceMinutes = preferencesManager.dailyAllowanceMinutes.first()
        if (allowanceMinutes <= 0) return false
        val monitoredPackages =
            appRepository.getActiveMonitoredAppsSnapshot().map { it.packageName }
        if (monitoredPackages.isEmpty()) return false
        val usageMs = getUsageMs(monitoredPackages)
        return usageMs >= allowanceMinutes * 60 * 1000L
    }

    suspend fun getRemainingAllowanceMinutes(): Int {
        if (!com.pause.app.util.PermissionHelper.hasUsageStatsPermission(context)) {
            return Int.MAX_VALUE
        }
        val allowanceMinutes = preferencesManager.dailyAllowanceMinutes.first()
        if (allowanceMinutes <= 0) return Int.MAX_VALUE
        val monitoredPackages =
            appRepository.getActiveMonitoredAppsSnapshot().map { it.packageName }
        if (monitoredPackages.isEmpty()) return allowanceMinutes
        val usageMs = getUsageMs(monitoredPackages)
        val usedMinutes = (usageMs / (60 * 1000)).toInt()
        return (allowanceMinutes - usedMinutes).coerceAtLeast(0)
    }

    private fun queryUsageStats(packageNames: List<String>): Long {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L
        val midnight = DateUtils.getTodayMidnight()
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            midnight,
            now
        ) ?: return 0L
        return stats
            .filter { it.packageName in packageNames }
            .sumOf { it.totalTimeInForeground }
    }
}
