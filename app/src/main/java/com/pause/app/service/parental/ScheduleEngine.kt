package com.pause.app.service.parental

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pause.app.data.db.entity.ScheduleBandEntity
import com.pause.app.receiver.ScheduleBandChangeReceiver
import com.pause.app.data.repository.ScheduleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class BandChange(
    val newBand: ScheduleBandEntity.ScheduleBandType,
    val changeAt: Long,
    val msUntilChange: Long
)

@Singleton
class ScheduleEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleRepository: ScheduleRepository
) {

    suspend fun getCurrentBand(): ScheduleBandEntity.ScheduleBandType {
        val calendar = Calendar.getInstance()
        val javaDayOfWeek = toJavaDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        // Check today's bands
        val bandsToday = scheduleRepository.getBandsForDay(javaDayOfWeek)
        for (band in bandsToday) {
            if (isInsideBand(band, currentMinutes)) {
                return band.bandType
            }
        }
        // Check yesterday's bands (for midnight-crossing: we may be in the "end" portion)
        val prevDay = if (javaDayOfWeek == 1) 7 else javaDayOfWeek - 1
        val bandsYesterday = scheduleRepository.getBandsForDay(prevDay)
        for (band in bandsYesterday) {
            if (band.crossesMidnight() && currentMinutes < parseTimeMinutes(band.endTime)) {
                return band.bandType
            }
        }
        return ScheduleBandEntity.ScheduleBandType.FREE
    }

    private fun toJavaDayOfWeek(calendarDay: Int): Int = when (calendarDay) {
        Calendar.SUNDAY -> 7
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        else -> 1
    }

    private fun isInsideBand(band: ScheduleBandEntity, currentMinutes: Int): Boolean {
        val (startMin, _) = parseTime(band.startTime)
        val (endMin, _) = parseTime(band.endTime)
        return if (band.crossesMidnight()) {
            currentMinutes >= startMin || currentMinutes < endMin
        } else {
            currentMinutes >= startMin && currentMinutes < endMin
        }
    }

    private fun ScheduleBandEntity.crossesMidnight(): Boolean {
        val (startMin, _) = parseTime(startTime)
        val (endMin, _) = parseTime(endTime)
        return endMin <= startMin
    }

    private fun parseTime(timeStr: String): Pair<Int, Boolean> {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (hour * 60 + minute) to false
    }

    private fun parseTimeMinutes(timeStr: String): Int = parseTime(timeStr).first

    suspend fun getNextBandChange(): BandChange? {
        val allBands = scheduleRepository.getAllBands()
        if (allBands.isEmpty()) return null
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        var earliestChange: BandChange? = null
        for (band in allBands) {
            val changeAt = getNextChangeForBand(calendar, band)
            if (changeAt != null && changeAt > now) {
                val msUntil = changeAt - now
                if (earliestChange == null || msUntil < earliestChange.msUntilChange) {
                    earliestChange = BandChange(band.bandType, changeAt, msUntil)
                }
            }
        }
        return earliestChange
    }

    private fun getNextChangeForBand(calendar: Calendar, band: ScheduleBandEntity): Long? {
        val startMin = parseTimeMinutes(band.startTime)
        val endMin = parseTimeMinutes(band.endTime)
        val now = System.currentTimeMillis()

        val nextStart = getNextOccurrence(calendar, band.dayOfWeek, startMin)
        val nextEnd = if (band.crossesMidnight()) {
            // End is on the day after start; e.g. Mon 22:00-07:00 ends Tue 07:00
            val endDay = if (band.dayOfWeek == 7) 1 else band.dayOfWeek + 1
            getNextOccurrence(calendar, endDay, endMin)
        } else {
            getNextOccurrence(calendar, band.dayOfWeek, endMin)
        }

        val candidates = listOfNotNull(
            nextStart.takeIf { it > now },
            nextEnd.takeIf { it > now }
        )
        return candidates.minOrNull()
    }

    private fun getNextOccurrence(calendar: Calendar, targetDayOfWeek: Int, minutesOfDay: Int): Long {
        val cal = calendar.clone() as Calendar
        val currentJavaDay = toJavaDayOfWeek(cal.get(Calendar.DAY_OF_WEEK))
        var daysToAdd = (targetDayOfWeek - currentJavaDay + 7) % 7
        if (daysToAdd == 0) {
            val currentMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            if (currentMin >= minutesOfDay) daysToAdd = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysToAdd)
        cal.set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
        cal.set(Calendar.MINUTE, minutesOfDay % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun scheduleNextBandChangeAlarm(msUntilChange: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleBandChangeReceiver::class.java).apply {
            action = ScheduleBandChangeReceiver.ACTION_SCHEDULE_BAND_CHANGE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + msUntilChange,
            pendingIntent
        )
    }
}
