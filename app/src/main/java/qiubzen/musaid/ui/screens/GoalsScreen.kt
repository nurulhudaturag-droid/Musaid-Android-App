package qiubzen.musaid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import qiubzen.musaid.data.Goal
import qiubzen.musaid.viewmodel.GoalViewModel
import qiubzen.musaid.viewmodel.GoalWithStepStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalViewModel = viewModel(),
    onOpenGoalDetail: (Int) -> Unit
) {
    val goalsWithStats by viewModel.goalsWithStats.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    // Rotation-safe dialog state: target stored by id, re-derived from the list.
    var showAddGoalDialog by rememberSaveable { mutableStateOf(false) }
    var goalToDeleteId by rememberSaveable { mutableStateOf<Int?>(null) }
    val goalToDelete = goalsWithStats.firstOrNull { it.goal.id == goalToDeleteId }?.goal

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGoalDialog = true },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("নতুন টার্গেট", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Filter Row (horizontally scrollable so no chip clips on narrow screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == Goal.STATUS_IN_PROGRESS,
                    onClick = { viewModel.setStatusFilter(Goal.STATUS_IN_PROGRESS) },
                    label = { Text("চলমান") }
                )
                FilterChip(
                    selected = statusFilter == Goal.STATUS_COMPLETED,
                    onClick = { viewModel.setStatusFilter(Goal.STATUS_COMPLETED) },
                    label = { Text("সম্পূর্ণ") }
                )
                FilterChip(
                    selected = statusFilter == Goal.STATUS_PAUSED,
                    onClick = { viewModel.setStatusFilter(Goal.STATUS_PAUSED) },
                    label = { Text("স্থগিত") }
                )
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("সব") }
                )
            }

            if (goalsWithStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "কোনো টার্গেট খুঁজে পাওয়া যায়নি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আপনার দীর্ঘমেয়াদী লক্ষ্য ও পরিকল্পনা যুক্ত করতে নিচের বোতাম ব্যবহার করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goalsWithStats, key = { it.goal.id }) { item ->
                        GoalCard(
                            item = item,
                            onClick = { onOpenGoalDetail(item.goal.id) },
                            onDelete = { goalToDeleteId = item.goal.id }
                        )
                    }
                }
            }
        }
    }

    if (showAddGoalDialog) {
        AddEditGoalDialog(
            goal = null,
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, motivation, start, target, _ ->
                viewModel.addGoal(title, motivation, start, target)
                showAddGoalDialog = false
            }
        )
    }

    if (goalToDelete != null) {
        AlertDialog(
            onDismissRequest = { goalToDeleteId = null },
            title = { Text("টার্গেট মুছে ফেলতে চান?") },
            text = { Text("\"${goalToDelete.title}\" এবং এর সমস্ত ধাপ ও রিমাইন্ডার মুছে যাবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goalToDelete)
                        goalToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { goalToDeleteId = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun GoalCard(
    item: GoalWithStepStats,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = item.goal
    val targetDate = LocalDate.ofEpochDay(goal.targetDateEpochDay)
    val targetDateStr = targetDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    val todayEpochDay = LocalDate.now().toEpochDay()
    val daysRemaining = (goal.targetDateEpochDay - todayEpochDay).coerceAtLeast(0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (goal.motivationNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = goal.motivationNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(goal.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Deadline and Step stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "শেষ: $targetDateStr ($daysRemaining দিন বাকি)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${item.completedSteps}/${item.totalSteps} ধাপ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { item.workProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Behind schedule warning or completion check
            if (item.isBehind) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সময়ের তুলনায় কিছুটা পিছিয়ে আছেন",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Open",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
