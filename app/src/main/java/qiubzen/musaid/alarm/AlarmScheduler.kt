package qiubzen.musaid.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import qiubzen.musaid.data.RoutineItem
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleRoutineAlarm(routine: RoutineItem) {
        if (!routine.isAlarmEnabled) return
        if (routine.daysOfWeek.isEmpty()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ROUTINE_ALARM
            putExtra(AlarmReceiver.EXTRA_ROUTINE_ID, routine.id)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_TITLE, routine.title)
            putExtra(AlarmReceiver.EXTRA_ROUTINE_MESSAGE, routine.alarmMessage)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routine.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Find next occurrence
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sunday
        
        // Time of day from timeMillis (since midnight)
        val hours = (routine.timeMillis / (1000 * 60 * 60)).toInt()
        val mins = ((routine.timeMillis / (1000 * 60)) % 60).toInt()
        
        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, mins)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If time has passed for today, or today is not in daysOfWeek, find next day
        val now = Calendar.getInstance()
        var addDays = 0
        
        if (calendar.before(now)) {
            // Already passed today, start checking from tomorrow
            addDays = 1
        }
        
        // Find next day in daysOfWeek
        var dayMatched = false
        while (addDays < 8) { // Check up to a week
            val checkDay = (currentDayOfWeek + addDays) % 7
            if (routine.daysOfWeek.contains(checkDay)) {
                calendar.add(Calendar.DAY_OF_YEAR, addDays)
                dayMatched = true
                break
            }
            addDays++
        }

        // Out-of-range day values (possible via imported JSON) would otherwise fall
        // through to today's (possibly already past) time and fire immediately.
        if (!dayMatched) return

        scheduleWakeUp(calendar.timeInMillis, pendingIntent)
    }

    /**
     * Schedules an alarm, preferring an exact alarm. If the exact-alarm permission is
     * denied (Android 12+), falls back to an inexact but Doze-aware alarm instead of
     * silently dropping it. Neither path throws for a missing permission.
     */
    private fun scheduleWakeUp(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            val canExact = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms()
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelRoutineAlarm(routineId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ROUTINE_ALARM
        }
        // FLAG_NO_CREATE: cancelling must not fabricate a PendingIntent when none exists.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routineId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleGoalStepAlarm(step: qiubzen.musaid.data.GoalStep) {
        if (!step.reminderEnabled || step.reminderTimeMillis == null || step.isCompleted) return

        val localDate = java.time.LocalDate.ofEpochDay(step.targetDateEpochDay)
        val hours = (step.reminderTimeMillis / (1000 * 60 * 60)).toInt()
        val mins = ((step.reminderTimeMillis / (1000 * 60)) % 60).toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, localDate.year)
            set(Calendar.MONTH, localDate.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, localDate.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, mins)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_GOAL_STEP_ALARM
            putExtra(AlarmReceiver.EXTRA_GOAL_STEP_ID, step.id)
            putExtra(AlarmReceiver.EXTRA_GOAL_STEP_TITLE, step.title)
        }

        val requestCode = AlarmReceiver.GOAL_STEP_ALARM_BASE + step.id
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleWakeUp(calendar.timeInMillis, pendingIntent)
    }

    fun cancelGoalStepAlarm(stepId: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_GOAL_STEP_ALARM
        }
        val requestCode = AlarmReceiver.GOAL_STEP_ALARM_BASE + stepId
        // FLAG_NO_CREATE: cancelling must not fabricate a PendingIntent when none exists.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun scheduleReportAlarm(timeMillisSinceMidnight: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_REPORT_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            -1, // constant id for report
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        val hours = (timeMillisSinceMidnight / (1000 * 60 * 60)).toInt()
        val mins = ((timeMillisSinceMidnight / (1000 * 60)) % 60).toInt()

        calendar.set(Calendar.HOUR_OF_DAY, hours)
        calendar.set(Calendar.MINUTE, mins)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        scheduleWakeUp(calendar.timeInMillis, pendingIntent)
    }
}
