package com.pause.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pause.app.data.repository.AppRepository
import com.pause.app.data.repository.InsightsRepository
import com.pause.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyInsight(
    val mostOpenedApp: String?,
    val mostOpenedCount: Int,
    val totalLaunches: Int,
    val mostCommonTrigger: String?,
    val mostCommonTriggerCount: Int
)

@HiltViewModel
class WeeklyInsightsViewModel @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _insights = MutableStateFlow<WeeklyInsight?>(null)
    val insights: StateFlow<WeeklyInsight?> = _insights.asStateFlow()

    init {
        viewModelScope.launch {
            loadInsights()
        }
    }

    private suspend fun loadInsights() {
        val since = DateUtils.getWeekAgoMidnight()
        val launches = insightsRepository.getLaunchEventsSince(since).first().filter { !it.wasCancelled }
        val reasons = insightsRepository.getReflectionReasonCountsSince(since)
        val apps = appRepository.getActiveMonitoredAppsSnapshot()

        val byPackage = launches.groupBy { it.packageName }
        val mostOpened = byPackage.maxByOrNull { it.value.size }
        val packageToName = apps.associate { it.packageName to it.appName }
        val mostOpenedApp = mostOpened?.let {
            packageToName[it.key] ?: it.key
        }
        val mostOpenedCount = mostOpened?.value?.size ?: 0
        val totalLaunches = launches.size
        val mostCommonTrigger = reasons.maxByOrNull { it.count }?.let {
            formatReason(it.reasonCode) to it.count
        }
        _insights.value = WeeklyInsight(
            mostOpenedApp = mostOpenedApp,
            mostOpenedCount = mostOpenedCount,
            totalLaunches = totalLaunches,
            mostCommonTrigger = mostCommonTrigger?.first,
            mostCommonTriggerCount = mostCommonTrigger?.second ?: 0
        )
    }

    private fun formatReason(code: String): String = when (code) {
        "BORED" -> "I'm bored"
        "HABIT" -> "Just habit"
        "REPLYING" -> "Replying to someone"
        "INTENTIONAL" -> "I have a reason"
        else -> code
    }
}
