package qiubzen.musaid.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.updateAll
import qiubzen.musaid.MainActivity
import qiubzen.musaid.data.AppDatabase
import qiubzen.musaid.widget.RoutineWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ROUTINE_ALARM = "qiubzen.musaid.action.ROUTINE_ALARM"
        const val ACTION_REPORT_ALARM = "qiubzen.musaid.action.REPORT_ALARM"
        const val ACTION_GOAL_STEP_ALARM = "qiubzen.musaid.action.GOAL_STEP_ALARM"

        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_ROUTINE_TITLE = "extra_routine_title"
        const val EXTRA_ROUTINE_MESSAGE = "extra_routine_message"

        const val EXTRA_GOAL_STEP_ID = "extra_goal_step_id"
        const val EXTRA_GOAL_STEP_TITLE = "extra_goal_step_title"

        private const val CHANNEL_ID = "daily_routine_channel"
        private const val NOTIFICATION_ID_BASE = 1000
        const val GOAL_STEP_ALARM_BASE = 500_000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        createNotificationChannel(context)

        when (intent.action) {
            ACTION_ROUTINE_ALARM -> {
                val id = intent.getIntExtra(EXTRA_ROUTINE_ID, 0)
                val title = intent.getStringExtra(EXTRA_ROUTINE_TITLE) ?: "রুটিন"
                val message = intent.getStringExtra(EXTRA_ROUTINE_MESSAGE) ?: ""

                showNotification(context, id + NOTIFICATION_ID_BASE, title, message, "routines")

                // Reschedule the next occurrence independently of TTS: a missing or
                // failing speech engine must never stop a recurring routine, and
                // pendingResult must be finished even if the DB work throws.
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val dao = AppDatabase.getDatabase(context).appDao()
                        val routine = dao.getRoutineById(id)
                        if (routine != null && routine.isAlarmEnabled) {
                            AlarmScheduler(context).scheduleRoutineAlarm(routine)
                        }
                        RoutineWidget().updateAll(context)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }

                if (message.isNotBlank()) {
                    speakMessage(context, message)
                }
            }
            ACTION_GOAL_STEP_ALARM -> {
                val stepId = intent.getIntExtra(EXTRA_GOAL_STEP_ID, 0)
                val stepTitle = intent.getStringExtra(EXTRA_GOAL_STEP_TITLE) ?: "টার্গেট"
                val spokenMessage = "আজ আপনার টার্গেট — $stepTitle।"

                showNotification(context, GOAL_STEP_ALARM_BASE + stepId, "টার্গেট রিমাইন্ডার", spokenMessage, "target")
                speakMessage(context, spokenMessage)
                pendingResult.finish()
            }
            ACTION_REPORT_ALARM -> {
                showNotification(context, 999, "দৈনিক রিপোর্ট", "আজকের রিপোর্ট পূরণ করুন", "report")
                // Reschedule report alarm for next day
                val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val reportTimeMillis = sharedPrefs.getLong("report_time", 21L * 60 * 60 * 1000) // Default 9 PM
                AlarmScheduler(context).scheduleReportAlarm(reportTimeMillis)
                pendingResult.finish()
            }
            else -> {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(context: Context, id: Int, title: String, content: String, targetScreen: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", targetScreen)
        }
        val pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            // On Android 7.x (minSdk 24) a builder without defaults plays nothing;
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(id, builder.build())
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "অ্যালার্ম"
            val descriptionText = "রুটিন ও টার্গেটের অ্যালার্ম"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Fire-and-forget Bangla read-aloud. Uses the application context and never feeds
     * back into the receiver lifecycle, so a dead/missing TTS engine cannot delay
     * pendingResult.finish() or stop the next alarm from being scheduled.
     */
    private fun speakMessage(context: Context, message: String) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("bn", "BD")
                val result = tts?.setLanguage(locale)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to default if Bangla not supported
                    tts?.setLanguage(Locale.getDefault())
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        tts?.shutdown()
                    }
                    override fun onError(utteranceId: String?) {
                        tts?.shutdown()
                    }
                })

                val speakResult = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "alarm_${System.currentTimeMillis()}")
                if (speakResult != TextToSpeech.SUCCESS) {
                    tts?.shutdown()
                }
            } else {
                // Engine failed to initialise — release the connection (no callback will come).
                tts?.shutdown()
            }
        }
    }
}
