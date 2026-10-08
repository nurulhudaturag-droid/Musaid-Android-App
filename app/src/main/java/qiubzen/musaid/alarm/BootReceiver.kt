package qiubzen.musaid.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import qiubzen.musaid.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.getDatabase(context).appDao()
                    val routines = dao.getAllRoutines().first()
                    val scheduler = AlarmScheduler(context)

                    routines.forEach { routine ->
                        if (routine.isAlarmEnabled) {
                            scheduler.scheduleRoutineAlarm(routine)
                        }
                    }

                    // Reschedule active goal step reminders
                    val activeSteps = dao.getAllActiveStepReminders()
                    activeSteps.forEach { step ->
                        scheduler.scheduleGoalStepAlarm(step)
                    }

                    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val reportTimeMillis = sharedPrefs.getLong("report_time", 21L * 60 * 60 * 1000)
                    scheduler.scheduleReportAlarm(reportTimeMillis)

                    pendingResult.finish()
                }
            }
        }
    }
}
