package qiubzen.musaid

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import qiubzen.musaid.ui.screens.MainScreen
import qiubzen.musaid.ui.theme.MusaidTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // The daily report reminder must exist from first launch (idempotent: same
    // PendingIntent requestCode is re-used, so re-scheduling just updates it).
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val alarmScheduler = qiubzen.musaid.alarm.AlarmScheduler(this)
    alarmScheduler.scheduleReportAlarm(prefs.getLong("report_time", 21L * 60 * 60 * 1000))

    setContent {
      MusaidTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navigateTo = intent.getStringExtra("navigate_to") ?: "routines"

            // Chain the exact-alarm prompt behind the notification dialog (never two
            // system UIs at once) and ask only once per install — otherwise Android
            // 13+ reopens Settings on every cold start.
            val maybeRequestExactAlarm = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    if (!alarmManager.canScheduleExactAlarms() &&
                        !alarmPrefs.getBoolean("exact_alarm_prompted", false)
                    ) {
                        alarmPrefs.edit().putBoolean("exact_alarm_prompted", true).apply()
                        Toast.makeText(this@MainActivity, "অ্যালার্মের জন্য অনুমতি প্রয়োজন (Exact Alarm Permission required)", Toast.LENGTH_LONG).show()
                        val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(settingsIntent)
                    }
                }
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ ->
                // Notification permission resolved: re-assert the report alarm so the
                // result is applied immediately (schedule is idempotent).
                alarmScheduler.scheduleReportAlarm(prefs.getLong("report_time", 21L * 60 * 60 * 1000))
                maybeRequestExactAlarm()
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    maybeRequestExactAlarm()
                }
            }

            MainScreen(startDestination = navigateTo)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    // The exact-alarm grant is a one-shot Settings toggle: Android does not reschedule
    // existing PendingIntents when the user flips it, so rebuild every alarm as soon as
    // the app becomes visible again with the permission now held.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
      if (alarmManager.canScheduleExactAlarms()) {
        rescheduleAllAlarms()
      }
    }
  }

  private fun rescheduleAllAlarms() {
    val scheduler = qiubzen.musaid.alarm.AlarmScheduler(this)
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val dao = qiubzen.musaid.data.AppDatabase.getDatabase(applicationContext).appDao()
    lifecycleScope.launch(Dispatchers.IO) {
      dao.getAllRoutines().first().forEach { routine ->
        if (routine.isAlarmEnabled) scheduler.scheduleRoutineAlarm(routine)
      }
      dao.getAllActiveStepReminders().forEach { scheduler.scheduleGoalStepAlarm(it) }
      scheduler.scheduleReportAlarm(prefs.getLong("report_time", 21L * 60 * 60 * 1000))
    }
  }
}
