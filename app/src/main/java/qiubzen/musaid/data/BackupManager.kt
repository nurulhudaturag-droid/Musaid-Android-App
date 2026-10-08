package qiubzen.musaid.data

import android.content.Context
import android.net.Uri
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader

@JsonClass(generateAdapter = true)
data class BackupData(
    val routines: List<RoutineItem>,
    val checklists: List<ChecklistItem>,
    val goals: List<Goal> = emptyList(),
    val goalSteps: List<GoalStep> = emptyList(),
    // Default empty so backups made before this field still import.
    val completionRecords: List<CompletionRecord> = emptyList()
)

class BackupManager(private val context: Context, private val repository: AppRepository) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(BackupData::class.java)

    suspend fun exportData(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val routines = repository.allRoutines.first()
            val checklists = repository.allChecklists.first()
            val goals = repository.allGoals.first()
            val goalSteps = repository.getAllGoalStepsOnce()
            val completionRecords = repository.getAllCompletionRecordsOnce()

            val backupData = BackupData(routines, checklists, goals, goalSteps, completionRecords)
            val json = adapter.toJson(backupData)

            // "wt" truncates first so overwriting an existing longer document cannot
            // leave a stale tail that later fails to parse. A null stream means the
            // export did not happen — report failure instead of a false success.
            val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                ?: return@withContext false
            withContext(NonCancellable) {
                outputStream.use { it.write(json.toByteArray()) }
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importData(uri: Uri): BackupData? = withContext(Dispatchers.IO) {
        try {
            val stringBuilder = java.lang.StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            val json = stringBuilder.toString()
            val backupData = adapter.fromJson(json)
            backupData
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
