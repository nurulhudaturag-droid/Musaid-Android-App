package qiubzen.musaid.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import qiubzen.musaid.viewmodel.PastDaySummary
import qiubzen.musaid.viewmodel.PastReportStatus
import qiubzen.musaid.viewmodel.ReportViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel = viewModel()) {
    val items by viewModel.reportItems.collectAsState(initial = emptyList())
    val selectedDate by viewModel.selectedDate.collectAsState()
    val past30Days by viewModel.past30DaysSummary.collectAsState(initial = emptyList())

    var showPastReportsDialog by rememberSaveable { mutableStateOf(false) }

    val isToday = selectedDate == LocalDate.now()
    val canGoNext = viewModel.canGoToNextDay()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

    Column(modifier = Modifier.fillMaxSize()) {
        // Date selector row & Past Reports trigger
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousDay() }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous Day")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isToday) "আজকের রিপোর্ট" else "পূর্ববর্তী রিপোর্ট",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = selectedDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextDay() },
                        enabled = canGoNext
                    ) {
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = "Next Day",
                            tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Past reports list button
                    OutlinedButton(
                        onClick = { showPastReportsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পূর্বের রিপোর্টসমূহ (৩০ দিন)", style = MaterialTheme.typography.labelMedium)
                    }

                    if (!isToday) {
                        FilledTonalButton(
                            onClick = { viewModel.resetToToday() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আজকে ফিরুন", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        if (!isToday) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "আপনি ${selectedDate.format(DateTimeFormatter.ofPattern("dd MMM"))} এর পেছনের রিপোর্ট পূরণ করছেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "এই দিনের জন্য কোনো রুটিন বা কাজ নেই।\n(No routines/tasks for this date)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { "${it.type}_${it.id}" }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (item.type == "ROUTINE") "রুটিন" else "চেকলিস্ট কাজ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (item.isCompleted) "হ্যাঁ (Yes)" else "না (No)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isCompleted) Color(0xFF1B5E20) else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = item.isCompleted,
                                    onCheckedChange = { viewModel.toggleItemCompletion(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPastReportsDialog) {
        PastReportsDialog(
            pastDays = past30Days,
            onSelectDate = { date ->
                viewModel.setDate(date)
                showPastReportsDialog = false
            },
            onDismiss = { showPastReportsDialog = false }
        )
    }
}

@Composable
fun PastReportsDialog(
    pastDays: List<PastDaySummary>,
    onSelectDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("বিগত ৩০ দিনের রিপোর্ট", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "কোনো দিনের রিপোর্ট বাদ পড়ে থাকলে তা নির্বাচন করে পূরণ করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pastDays, key = { it.date.toEpochDay() }) { item ->
                        PastDayItem(item = item, onClick = { onSelectDate(item.date) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}

@Composable
fun PastDayItem(
    item: PastDaySummary,
    onClick: () -> Unit
) {
    val isToday = item.date == LocalDate.now()
    val dateText = if (isToday) {
        "আজ (${item.date.format(DateTimeFormatter.ofPattern("dd MMM"))})"
    } else if (item.date == LocalDate.now().minusDays(1)) {
        "গতকাল (${item.date.format(DateTimeFormatter.ofPattern("dd MMM"))})"
    } else {
        item.date.format(DateTimeFormatter.ofPattern("dd MMMM, EEE"))
    }

    val (statusLabel, statusColor, statusBg) = when (item.status) {
        PastReportStatus.SUBMITTED -> Triple("সম্পূর্ণ", Color(0xFF1B5E20), Color(0xFFE8F5E9))
        PastReportStatus.PARTIAL -> Triple("আংশিক", Color(0xFFE65100), Color(0xFFFFE0B2))
        PastReportStatus.MISSED -> Triple("বাদ পড়েছে", Color(0xFFB71C1C), Color(0xFFFFEBEE))
        PastReportStatus.NOT_APPLICABLE -> Triple("প্রযোজ্য নয়", Color(0xFF616161), Color(0xFFEEEEEE))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${item.recordedCount} / ${item.totalApplicable} টি কাজ রেকর্ড হয়েছে",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = statusBg
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
