package qiubzen.musaid.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qiubzen.musaid.data.Goal
import qiubzen.musaid.data.GoalStep
import qiubzen.musaid.viewmodel.GoalViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Int,
    viewModel: GoalViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val goal by viewModel.getGoalFlow(goalId).collectAsState(initial = null)
    val steps by viewModel.getStepsFlow(goalId).collectAsState(initial = emptyList())

    // Rotation-safe dialog state: editing target stored as id and re-derived from
    // the live steps list (entities are not Bundle-storable).
    var showAddStepDialog by rememberSaveable { mutableStateOf(false) }
    var editingStepId by rememberSaveable { mutableStateOf<Int?>(null) }
    var showEditGoalDialog by rememberSaveable { mutableStateOf(false) }
    var congratulationsGoalId by rememberSaveable { mutableStateOf<Int?>(null) }
    val editingStep = steps.firstOrNull { it.id == editingStepId }

    LaunchedEffect(Unit) {
        viewModel.congratulationsEvent.collect {
            if (it.id == goalId) {
                congratulationsGoalId = it.id
            }
        }
    }

    if (goal == null) {
        // Loading state — but always offer a way out: goalId may not resolve
        // (deleted goal / bad deep link), and a bare spinner would trap the user.
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("টার্গেট") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("টার্গেট পাওয়া যায়নি", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        return
    }

    val currentGoal = goal!!
    val todayEpochDay = LocalDate.now().toEpochDay()
    val totalSteps = steps.size
    val completedSteps = steps.count { it.isCompleted }
    val workProgress = if (totalSteps > 0) completedSteps.toFloat() / totalSteps else 0f

    val totalDays = (currentGoal.targetDateEpochDay - currentGoal.startDateEpochDay).coerceAtLeast(1)
    val elapsedDays = (todayEpochDay - currentGoal.startDateEpochDay).coerceAtLeast(0)
    val timeProgress = (elapsedDays.toFloat() / totalDays).coerceIn(0f, 1f)
    val isFallingBehind = totalSteps > 0 && workProgress < timeProgress && currentGoal.status == Goal.STATUS_IN_PROGRESS

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val startDateStr = LocalDate.ofEpochDay(currentGoal.startDateEpochDay).format(dateFormatter)
    val targetDateStr = LocalDate.ofEpochDay(currentGoal.targetDateEpochDay).format(dateFormatter)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentGoal.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditGoalDialog = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Goal")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStepId = null
                    showAddStepDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ধাপ যোগ করুন", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Motivation and Date Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.DateRange,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$startDateStr — $targetDateStr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(currentGoal.status)
                        }

                        if (currentGoal.motivationNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp)) {
                                    Icon(
                                        Icons.Filled.Lightbulb,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = currentGoal.motivationNote,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dual Progress Indicators Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "অগ্রগতি পর্যবেক্ষণ (Progress Tracking)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Work Progress
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "কাজের অগ্রগতি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(workProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { workProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "$completedSteps / $totalSteps ধাপ সম্পন্ন",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Time Progress
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "সময়ের অগ্রগতি",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${(timeProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFallingBehind) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { timeProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isFallingBehind) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val remainingDays = (currentGoal.targetDateEpochDay - todayEpochDay).coerceAtLeast(0)
                                Text(
                                    "$remainingDays দিন বাকি",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Visual cue
                        if (isFallingBehind) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "সতর্কতা: আপনি সময়ের তুলনায় পিছিয়ে আছেন!",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        } else if (totalSteps > 0 && workProgress >= timeProgress && currentGoal.status == Goal.STATUS_IN_PROGRESS) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "দারুণ! আপনি সঠিক গতিতে এগোচ্ছেন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Steps Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "টার্গেটের ধাপসমূহ (Milestone Steps)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$totalSteps টি ধাপ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (steps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.FormatListNumbered,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "এখনও কোনো ধাপ যোগ করা হয়নি।\nনিচের বোতাম চেপে প্রথম ধাপ যুক্ত করুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(steps, key = { _, s -> s.id }) { index, step ->
                    val isLast = index == steps.size - 1
                    StepTimelineItem(
                        step = step,
                        index = index + 1,
                        isLast = isLast,
                        onToggle = { viewModel.toggleStepCompletion(step, it) },
                        onEdit = {
                            editingStepId = step.id
                            showAddStepDialog = true
                        },
                        onDelete = { viewModel.deleteStep(step) },
                        onMoveUp = { viewModel.moveStepUp(step, steps) },
                        onMoveDown = { viewModel.moveStepDown(step, steps) }
                    )
                }
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showAddStepDialog) {
        AddEditStepDialog(
            goalId = goalId,
            step = editingStep,
            onDismiss = { showAddStepDialog = false },
            onSave = { title, targetDate, reminderEnabled, reminderTimeMillis ->
                if (editingStep == null) {
                    viewModel.addStep(goalId, title, targetDate, reminderEnabled, reminderTimeMillis)
                } else {
                    viewModel.updateStep(
                        editingStep.copy(
                            title = title,
                            targetDateEpochDay = targetDate,
                            reminderEnabled = reminderEnabled,
                            reminderTimeMillis = reminderTimeMillis
                        )
                    )
                }
                showAddStepDialog = false
            }
        )
    }

    if (showEditGoalDialog) {
        AddEditGoalDialog(
            goal = currentGoal,
            onDismiss = { showEditGoalDialog = false },
            onSave = { title, motivation, start, target, status ->
                viewModel.updateGoal(
                    currentGoal.copy(
                        title = title,
                        motivationNote = motivation,
                        startDateEpochDay = start,
                        targetDateEpochDay = target,
                        status = status
                    )
                )
                showEditGoalDialog = false
            }
        )
    }

    // Congratulations Dialog
    if (congratulationsGoalId != null) {
        AlertDialog(
            onDismissRequest = { congratulationsGoalId = null },
            icon = {
                Text("🎉", fontSize = 48.sp)
            },
            title = {
                Text(
                    "অভিনন্দন!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    "আপনি আপনার টার্গেট \"${currentGoal.title}\" এর সব ধাপ সফলভাবে সম্পন্ন করেছেন! আপনি একটি দারুণ মাইলফলক অর্জন করলেন।",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { congratulationsGoalId = null }) {
                    Text("ধন্যবাদ!")
                }
            }
        )
    }
}

@Composable
fun StepTimelineItem(
    step: GoalStep,
    index: Int,
    isLast: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val todayEpochDay = LocalDate.now().toEpochDay()
    val isOverdue = !step.isCompleted && step.targetDateEpochDay < todayEpochDay
    val isDueToday = !step.isCompleted && step.targetDateEpochDay == todayEpochDay

    val stepDate = LocalDate.ofEpochDay(step.targetDateEpochDay)
    val dateText = stepDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))

    // Colors
    val circleBgColor = when {
        step.isCompleted -> Color(0xFF2E7D32) // Green
        isOverdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val circleTextColor = when {
        step.isCompleted || isOverdue -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Stepper circle & line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(circleBgColor),
                contentAlignment = Alignment.Center
            ) {
                if (step.isCompleted) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (isOverdue) {
                    Icon(
                        Icons.Filled.PriorityHigh,
                        contentDescription = "Overdue",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = circleTextColor
                    )
                }
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(70.dp)
                        .background(
                            if (step.isCompleted) Color(0xFF2E7D32).copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Step Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    step.isCompleted -> Color(0xFFE8F5E9) // Light green
                    isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (step.isCompleted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "শেষ তারিখ: $dateText",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isOverdue) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = "দেরি হয়েছে",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isDueToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "আজকের টার্গেট",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (step.reminderEnabled && step.reminderTimeMillis != null) {
                            val hours = (step.reminderTimeMillis / (1000 * 60 * 60)).toInt()
                            val mins = ((step.reminderTimeMillis / (1000 * 60)) % 60).toInt()
                            val amPm = if (hours >= 12) "PM" else "AM"
                            val h12 = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
                            val timeStr = String.format("%02d:%02d %s", h12, mins, amPm)

                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = "Reminder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "রিমাইন্ডার: $timeStr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Checkbox(
                        checked = step.isCompleted,
                        onCheckedChange = onToggle
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Step", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Step", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (text, color, bg) = when (status) {
        Goal.STATUS_COMPLETED -> Triple("সম্পূর্ণ", Color(0xFF1B5E20), Color(0xFFC8E6C9))
        Goal.STATUS_PAUSED -> Triple("স্থগিত", Color(0xFFE65100), Color(0xFFFFE0B2))
        else -> Triple("চলমান", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AddEditStepDialog(
    goalId: Int,
    step: GoalStep?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetDate: Long, reminderEnabled: Boolean, reminderTimeMillis: Long?) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(step?.title ?: "") }
    var targetDateEpochDay by rememberSaveable {
        mutableStateOf(step?.targetDateEpochDay ?: LocalDate.now().plusDays(7).toEpochDay())
    }
    var reminderEnabled by rememberSaveable { mutableStateOf(step?.reminderEnabled ?: false) }
    var reminderTimeMillis by rememberSaveable {
        mutableStateOf(step?.reminderTimeMillis ?: (9L * 60 * 60 * 1000)) // default 9 AM
    }

    val selectedDate = LocalDate.ofEpochDay(targetDateEpochDay)
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (step == null) "নতুন ধাপ যোগ করুন" else "ধাপ পরিবর্তন করুন") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ধাপের নাম / কাজ (Step Title)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Date Picker Button
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.YEAR, selectedDate.year)
                        cal.set(Calendar.MONTH, selectedDate.monthValue - 1)
                        cal.set(Calendar.DAY_OF_MONTH, selectedDate.dayOfMonth)

                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                            targetDateEpochDay = pickedDate.toEpochDay()
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("শেষ তারিখ: ${selectedDate.format(dateFormatter)}")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reminder switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("ভয়েস রিমাইন্ডার", style = MaterialTheme.typography.bodyMedium)
                        Text("নির্দিষ্ট দিনে অ্যালার্ম দেবে", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { reminderEnabled = it }
                    )
                }

                if (reminderEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val hours = (reminderTimeMillis / (1000 * 60 * 60)).toInt()
                    val mins = ((reminderTimeMillis / (1000 * 60)) % 60).toInt()
                    val amPm = if (hours >= 12) "PM" else "AM"
                    val h12 = if (hours == 0) 12 else if (hours > 12) hours - 12 else hours
                    val timeStr = String.format("%02d:%02d %s", h12, mins, amPm)

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                reminderTimeMillis = (h * 60 * 60 * 1000L) + (m * 60 * 1000L)
                            }, hours, mins, false).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("রিমাইন্ডারের সময়: $timeStr")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, targetDateEpochDay, reminderEnabled, if (reminderEnabled) reminderTimeMillis else null)
                },
                enabled = title.isNotBlank()
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
fun AddEditGoalDialog(
    goal: Goal? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, motivation: String, startDate: Long, targetDate: Long, status: String) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(goal?.title ?: "") }
    var motivation by rememberSaveable { mutableStateOf(goal?.motivationNote ?: "") }
    var startDateEpochDay by rememberSaveable {
        mutableStateOf(goal?.startDateEpochDay ?: LocalDate.now().toEpochDay())
    }
    var targetDateEpochDay by rememberSaveable {
        mutableStateOf(goal?.targetDateEpochDay ?: LocalDate.now().plusMonths(1).toEpochDay())
    }
    var status by rememberSaveable { mutableStateOf(goal?.status ?: Goal.STATUS_IN_PROGRESS) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val startDate = LocalDate.ofEpochDay(startDateEpochDay)
    val targetDate = LocalDate.ofEpochDay(targetDateEpochDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (goal == null) "নতুন টার্গেট / গোল" else "টার্গেট পরিবর্তন") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("টার্গেটের নাম (Goal Title)") },
                    placeholder = { Text("যেমন: ২ মাসে ৫ কেজি কমানো") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = motivation,
                    onValueChange = { motivation = it },
                    label = { Text("অনুপ্রেরণা / উদ্দেশ্য (Why Note)") },
                    placeholder = { Text("কেন এই টার্গেটটি গুরুত্বপূর্ণ?") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Start Date Picker
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.YEAR, startDate.year)
                        cal.set(Calendar.MONTH, startDate.monthValue - 1)
                        cal.set(Calendar.DAY_OF_MONTH, startDate.dayOfMonth)

                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                            startDateEpochDay = picked.toEpochDay()
                            if (targetDateEpochDay < startDateEpochDay) {
                                targetDateEpochDay = startDateEpochDay
                            }
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("শুরুর তারিখ: ${startDate.format(dateFormatter)}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Target Date Picker
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        cal.set(Calendar.YEAR, targetDate.year)
                        cal.set(Calendar.MONTH, targetDate.monthValue - 1)
                        cal.set(Calendar.DAY_OF_MONTH, targetDate.dayOfMonth)

                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                            targetDateEpochDay = picked.toEpochDay()
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("শেষ তারিখ: ${targetDate.format(dateFormatter)}")
                }

                if (goal != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("অবস্থা (Status):", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = status == Goal.STATUS_IN_PROGRESS,
                            onClick = { status = Goal.STATUS_IN_PROGRESS },
                            label = { Text("চলমান") }
                        )
                        FilterChip(
                            selected = status == Goal.STATUS_COMPLETED,
                            onClick = { status = Goal.STATUS_COMPLETED },
                            label = { Text("সম্পূর্ণ") }
                        )
                        FilterChip(
                            selected = status == Goal.STATUS_PAUSED,
                            onClick = { status = Goal.STATUS_PAUSED },
                            label = { Text("স্থগিত") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, motivation, startDateEpochDay, targetDateEpochDay.coerceAtLeast(startDateEpochDay), status)
                },
                enabled = title.isNotBlank()
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
