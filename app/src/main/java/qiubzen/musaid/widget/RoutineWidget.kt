package qiubzen.musaid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import qiubzen.musaid.data.AppDatabase
import qiubzen.musaid.data.RoutineItem
import kotlinx.coroutines.flow.first
import java.util.Calendar

class RoutineWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.getDatabase(context).appDao()
        val routines = dao.getAllRoutines().first()
        
        val nextRoutine = findNextRoutine(routines)
        
        provideContent {
            GlanceTheme {
                WidgetContent(nextRoutine)
            }
        }
    }

    private fun findNextRoutine(routines: List<RoutineItem>): RoutineItem? {
        val now = Calendar.getInstance()
        val currentDay = now.get(Calendar.DAY_OF_WEEK) - 1
        val currentMillis = (now.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000) + 
                            (now.get(Calendar.MINUTE) * 60 * 1000) + 
                            (now.get(Calendar.SECOND) * 1000)
                            
        var next: RoutineItem? = null
        var minTimeDiff = Long.MAX_VALUE
        
        routines.filter { it.isAlarmEnabled }.forEach { routine ->
            // Check today
            if (routine.daysOfWeek.contains(currentDay) && routine.timeMillis > currentMillis) {
                val diff = routine.timeMillis - currentMillis
                if (diff < minTimeDiff) {
                    minTimeDiff = diff
                    next = routine
                }
            } else {
                // Check future days (up to 7 days ahead so a single weekly
                // routine whose time passed today still shows as next)
                var addDays = 1
                while (addDays <= 7) {
                    val checkDay = (currentDay + addDays) % 7
                    if (routine.daysOfWeek.contains(checkDay)) {
                        val diff = (addDays * 24 * 60 * 60 * 1000L) + routine.timeMillis - currentMillis
                        if (diff < minTimeDiff) {
                            minTimeDiff = diff
                            next = routine
                        }
                        break
                    }
                    addDays++
                }
            }
        }
        return next
    }

    @Composable
    fun WidgetContent(nextRoutine: RoutineItem?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (nextRoutine != null) {
                Text(
                    text = "পরবর্তী রুটিন",
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
                )
                
                val hours = (nextRoutine.timeMillis / (1000 * 60 * 60)).toInt()
                val mins = ((nextRoutine.timeMillis / (1000 * 60)) % 60).toInt()
                val amPm = if (hours >= 12) "PM" else "AM"
                val h12 = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
                val timeStr = String.format("%02d:%02d %s", h12, mins, amPm)
                
                Text(
                    text = timeStr,
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = nextRoutine.title,
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp)
                )
            } else {
                Text(
                    text = "কোনো রুটিন নেই",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp)
                )
            }
        }
    }
}
