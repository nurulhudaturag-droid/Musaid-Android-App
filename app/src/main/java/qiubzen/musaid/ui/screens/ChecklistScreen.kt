package qiubzen.musaid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import qiubzen.musaid.data.ChecklistItem
import qiubzen.musaid.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(viewModel: AppViewModel = viewModel()) {
    val checklists by viewModel.checklists.collectAsState()
    // Dialog state survives rotation/process death: the editing target is stored as an
    // id and re-derived from the list (entities are not Bundle-storable).
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var editingItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    val editingItem = checklists.firstOrNull { it.id == editingItemId }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingItemId = null
                    showDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Checklist")
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
            items(checklists) { item ->
                ChecklistCard(
                    item = item,
                    onToggle = { viewModel.toggleChecklistActive(item, it) },
                    onEdit = {
                        editingItemId = item.id
                        showDialog = true
                    },
                    onDelete = { viewModel.deleteChecklist(item) }
                )
            }
        }
    }

    if (showDialog) {
        ChecklistDialog(
            item = editingItem,
            onDismiss = { showDialog = false },
            onSave = { title ->
                if (editingItem == null) {
                    viewModel.addChecklist(title)
                } else {
                    viewModel.updateChecklist(editingItem.copy(title = title))
                }
                showDialog = false
            }
        )
    }
}

@Composable
fun ChecklistCard(
    item: ChecklistItem,
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
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (item.isActive) "সক্রিয় (Active)" else "নিষ্ক্রিয় (Inactive)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Switch(
                    checked = item.isActive,
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
fun ChecklistDialog(
    item: ChecklistItem?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(item?.title ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "নতুন কাজ" else "কাজ পরিবর্তন") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("শিরোনাম (Title)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(title) },
                enabled = title.isNotBlank()
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
