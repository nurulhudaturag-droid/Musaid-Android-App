package qiubzen.musaid.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import qiubzen.musaid.data.RoutineItem
import qiubzen.musaid.viewmodel.AppViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(viewModel: AppViewModel = viewModel()) {
    val routines by viewModel.routines.collectAsState()
    val todayDueSteps by viewModel.todayDueSteps.collectAsState()
    // Rotation-safe dialog state: editing target stored by id, re-derived from list.
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingRoutineId by rememberSaveable { mutableStateOf<Int?>(null) }
    val editingRoutine = routines.firstOrNull { it.id == editingRoutineId }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingRoutineId = null
                    showDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Routine")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (todayDueSteps.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Flag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "আজকের টার্গেটের শেষ সময় (${todayDueSteps.size} টি ধাপ)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            todayDueSteps.forEach { (step, goalTitle) ->
                                Text(
                                    text = "• ${step.title} (${goalTitle})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            items(routines) { routine ->
                RoutineCard(
                    routine = routine,
                    onToggle = { viewModel.toggleRoutineAlarm(routine, it) },
                    onEdit = {
                        editingRoutineId = routine.id
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteRoutine(routine) }
                )
            }
        }
    }

    if (showDialog) {
        RoutineDialog(
            routine = editingRoutine,
            onDismiss = { showDialog = false },
            onSave = { title, timeMillis, message, days ->
                if (editingRoutine == null) {
                    viewModel.addRoutine(title, timeMillis, message, days)
                } else {
                    viewModel.updateRoutine(
                        editingRoutine.copy(
                            title = title,
                            timeMillis = timeMillis,
                            alarmMessage = message,
                            daysOfWeek = days
                        )
                    )
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun RoutineCard(
    routine: RoutineItem,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val hours = (routine.timeMillis / (1000 * 60 * 60)).toInt()
                val mins = ((routine.timeMillis / (1000 * 60)) % 60).toInt()
                val amPm = if (hours >= 12) "PM" else "AM"
                val h12 = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
                
                Text(
                    text = String.format("%02d:%02d %s", h12, mins, amPm),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = routine.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (routine.alarmMessage.isNotBlank()) {
                    Text(
                        text = "বার্তা: ${routine.alarmMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val daysText = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                    .filterIndexed { index, _ -> routine.daysOfWeek.contains(index) }
                    .joinToString(", ")
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = routine.isAlarmEnabled,
                    onCheckedChange = onToggle
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineDialog(
    routine: RoutineItem?,
    onDismiss: () -> Unit,
    onSave: (String, Long, String, List<Int>) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(routine?.title ?: "") }
    var message by rememberSaveable { mutableStateOf(routine?.alarmMessage ?: "") }
    
    // Time
    val cal = Calendar.getInstance()
    if (routine != null) {
        cal.set(Calendar.HOUR_OF_DAY, (routine.timeMillis / 3600000).toInt())
        cal.set(Calendar.MINUTE, ((routine.timeMillis / 60000) % 60).toInt())
    }
    var hour by rememberSaveable { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var minute by rememberSaveable { mutableStateOf(cal.get(Calendar.MINUTE)) }
    
    // Days: seeded once on first composition; rememberSaveable then restores whatever
    // the user selected across rotation (no LaunchedEffect re-seed).
    var selectedDays by rememberSaveable {
        mutableStateOf(routine?.daysOfWeek ?: listOf(0, 1, 2, 3, 4, 5, 6))
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (routine == null) "নতুন রুটিন" else "রুটিন পরিবর্তন") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম (Title)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("অ্যালার্ম বার্তা (Spoken Message)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        hour = h
                        minute = m
                    }, hour, minute, false).show()
                }, modifier = Modifier.fillMaxWidth()) {
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val h12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
                    Text("সময় নির্বাচন: ${String.format("%02d:%02d %s", h12, minute, amPm)}")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("দিনসমূহ:", style = MaterialTheme.typography.labelMedium)
                val daysOfWeekNames = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                
                // Wrap in Row with horizontal scroll or multiple rows. Multiple rows is better.
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (i in 0..3) {
                            DayToggle(
                                day = daysOfWeekNames[i],
                                isSelected = selectedDays.contains(i),
                                onToggle = {
                                    selectedDays = if (selectedDays.contains(i)) selectedDays - i
                                    else selectedDays + i
                                }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        for (i in 4..6) {
                            DayToggle(
                                day = daysOfWeekNames[i],
                                isSelected = selectedDays.contains(i),
                                onToggle = {
                                    selectedDays = if (selectedDays.contains(i)) selectedDays - i
                                    else selectedDays + i
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val timeMillis = (hour * 60 * 60 * 1000L) + (minute * 60 * 1000L)
                    onSave(title, timeMillis, message, selectedDays)
                },
                enabled = title.isNotBlank() && selectedDays.isNotEmpty()
            ) {
                Text("সংরক্ষণ (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)")
            }
        }
    )
}

@Composable
fun DayToggle(day: String, isSelected: Boolean, onToggle: () -> Unit) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(day) }
    )
}
