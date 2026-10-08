package qiubzen.musaid.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import qiubzen.musaid.alarm.AlarmScheduler
import qiubzen.musaid.data.AppDatabase
import qiubzen.musaid.data.AppRepository
import qiubzen.musaid.data.BackupManager
import androidx.glance.appwidget.updateAll
import qiubzen.musaid.widget.RoutineWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    
    var reportTimeMillis by rememberSaveable {
        mutableStateOf(sharedPrefs.getLong("report_time", 21L * 60 * 60 * 1000)) // 9 PM
    }

    val dao = AppDatabase.getDatabase(context).appDao()
    val repository = AppRepository(dao)
    val backupManager = BackupManager(context, repository)
    val coroutineScope = rememberCoroutineScope()
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val success = backupManager.exportData(uri)
                if (success) {
                    Toast.makeText(context, "এক্সপোর্ট সফল হয়েছে", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "এক্সপোর্ট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
                coroutineScope.launch {
                    try {
                        val backupData = backupManager.importData(uri)
                        if (backupData != null) {
                            val scheduler = AlarmScheduler(context)

                            // Cancel alarms belonging to the data being replaced, otherwise
                            // stale PendingIntents keep firing for deleted/re-keyed items.
                            repository.allRoutines.first().forEach {
                                scheduler.cancelRoutineAlarm(it.id)
                            }
                            repository.getAllGoalStepsOnce().forEach {
                                scheduler.cancelGoalStepAlarm(it.id)
                            }

                            repository.replaceAllData(
                                backupData.routines,
                                backupData.checklists,
                                backupData.goals,
                                backupData.goalSteps,
                                backupData.completionRecords
                            )

                            // Reschedule from the persisted rows: Room may re-key imported
                            // ids (id=0 / duplicates), so JSON ids can differ from real ones.
                            repository.allRoutines.first().forEach {
                                if (it.isAlarmEnabled) {
                                    scheduler.scheduleRoutineAlarm(it)
                                }
                            }
                            repository.getAllGoalStepsOnce().forEach { step ->
                                if (step.reminderEnabled && !step.isCompleted) {
                                    scheduler.scheduleGoalStepAlarm(step)
                                }
                            }
                            RoutineWidget().updateAll(context)
                            Toast.makeText(context, "ইমপোর্ট সফল হয়েছে", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "ইমপোর্ট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Any DB/constraint failure surfaces as a toast instead of
                        // crashing the app; the @Transaction replace rolls back.
                        e.printStackTrace()
                        Toast.makeText(context, "ইমপোর্ট ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("সেটিংস (Settings)") },
        text = {
            Column {
                Text("রিপোর্টের সময় (Daily Report Time)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val hour = (reportTimeMillis / (1000 * 60 * 60)).toInt()
                    val min = ((reportTimeMillis / (1000 * 60)) % 60).toInt()
                    
                    TimePickerDialog(context, { _, h, m ->
                        val newTime = (h * 60 * 60 * 1000L) + (m * 60 * 1000L)
                        reportTimeMillis = newTime
                        sharedPrefs.edit().putLong("report_time", newTime).apply()
                        AlarmScheduler(context).scheduleReportAlarm(newTime)
                        Toast.makeText(context, "রিপোর্টের সময় আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
                    }, hour, min, false).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    val hour = (reportTimeMillis / (1000 * 60 * 60)).toInt()
                    val min = ((reportTimeMillis / (1000 * 60)) % 60).toInt()
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    Text("${String.format("%02d:%02d %s", h12, min, amPm)} পরিবর্তন করুন")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text("ব্যাকআপ (Backup & Restore)", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { exportLauncher.launch("daily_routine_backup.json") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("এক্সপোর্ট করুন (Export to JSON)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ইমপোর্ট করুন (Import from JSON)")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন (Close)")
            }
        }
    )
}
