package com.pause.app.service

import android.app.usage.UsageStatsManager
import android.content.Context
import com.pause.app.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import com.pause.app.data.repository.AppRepository
import com.pause.app.util.DateUtils
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllowanceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val preferencesManager: PreferencesManager
) {

    suspend fun hasAllowanceReached(): Boolean {
        if (!com.pause.app.util.PermissionHelper.hasUsageStatsPermission(context)) {
            return false
        }
        val allowanceMinutes = preferencesManager.dailyAllowanceMinutes.first()
        if (allowanceMinutes <= 0) return false // No limit set
        val monitoredPackages =
            appRepository.getActiveMonitoredAppsSnapshot().map { it.packageName }
        if (monitoredPackages.isEmpty()) return false
        val usageMs = getTodayUsageMs(monitoredPackages)
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
        val usageMs = getTodayUsageMs(monitoredPackages)
        val usedMinutes = (usageMs / (60 * 1000)).toInt()
        return (allowanceMinutes - usedMinutes).coerceAtLeast(0)
    }

    private fun getTodayUsageMs(packageNames: List<String>): Long {
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
