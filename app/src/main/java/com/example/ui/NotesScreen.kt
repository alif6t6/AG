package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotesScreen(viewModel: MainViewModel) {
    var activeTab by remember { mutableStateOf("notes") } // "notes", "todo", "reminders"

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Tab Bar style custom selectors
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
            val tabs = listOf(
                Pair("notes", "📝 Notes"),
                Pair("todo", "✅ Todo"),
                Pair("reminders", "🔔 Reminders")
            )

            tabs.forEach { (key, label) ->
                val isSelected = activeTab == key
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { activeTab = key }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) DevilRed else TextGrey,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) DevilRed else Color.Transparent)
                    )
                }
            }
        }
        HorizontalDivider(color = BorderColor, thickness = 1.dp)
        }

        // Body screens based on active tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeTab) {
                "notes" -> TabNotes(viewModel = viewModel, onShowDialog = { showAddNoteDialog = true })
                "todo" -> TabTodo(viewModel = viewModel, onShowDialog = { showAddTaskDialog = true })
                "reminders" -> TabReminders(viewModel = viewModel, onShowDialog = { showAddReminderDialog = true })
            }
        }
    }

    // Modal dialog trigger bindings
    if (showAddNoteDialog) {
        AddNoteDialog(viewModel = viewModel, onDismiss = { showAddNoteDialog = false })
    }
    if (showAddTaskDialog) {
        AddTaskDialog(viewModel = viewModel, onDismiss = { showAddTaskDialog = false })
    }
    if (showAddReminderDialog) {
        AddReminderDialog(viewModel = viewModel, onDismiss = { showAddReminderDialog = false })
    }
}

// --- SUB TABS IMPLEMENTATION ---

@Composable
fun TabNotes(
    viewModel: MainViewModel,
    onShowDialog: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            EmptyStateView("📝", "You have no notes. Create thoughts directly!")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes, key = { it.id }) { note ->
                    val parsedColor = remember(note.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(note.colorHex))
                        } catch (e: Exception) {
                            DevilRed
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            // Color bar indicator as requested
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(parsedColor)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Note",
                                    tint = TextDarkGrey,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { viewModel.deleteNoteById(note.id) }
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = note.content,
                                color = TextWhite.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = formatShortDate(note.date),
                                color = TextDarkGrey,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onShowDialog,
            containerColor = DevilRed,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 20.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
        }
    }
}

@Composable
fun TabTodo(
    viewModel: MainViewModel,
    onShowDialog: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            EmptyStateView("✅", "You have no tasks cataloged. Create a milestone now.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                    items(tasks, key = { it.id }) { task ->
                        val isDone = task.isCompleted

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { }
                                    .padding(horizontal = 20.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Checkbox status custom drawer
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDone) AppleGreen else Color.Transparent)
                                    .border(
                                        1.5.dp,
                                        if (isDone) AppleGreen else BorderColor,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.toggleTaskCompletion(task) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Done",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            // Info details
                            Column {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (isDone) TextGrey else TextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    // Priority badge
                                    val (prioColor, prioBg) = when (task.priority.uppercase()) {
                                        "HIGH" -> Pair(DevilRed, DevilRed.copy(alpha = 0.15f))
                                        "MEDIUM" -> Pair(AppleYellow, AppleYellow.copy(alpha = 0.15f))
                                        else -> Pair(AppleGreen, AppleGreen.copy(alpha = 0.15f))
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(prioBg)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = task.priority,
                                            color = prioColor,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Text(
                                        text = "📅 Due: ${formatShortDate(task.dueDate)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextGrey,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    if (task.tag.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(DarkSurface3)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = task.tag,
                                                color = TextGrey,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task",
                            tint = TextDarkGrey,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.deleteTaskById(task.id) }
                        )
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
            }
            }
        }

        FloatingActionButton(
            onClick = onShowDialog,
            containerColor = DevilRed,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 20.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun TabReminders(
    viewModel: MainViewModel,
    onShowDialog: () -> Unit
) {
    val reminders by viewModel.reminders.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (reminders.isEmpty()) {
            EmptyStateView("🔔", "No alerts configured. Ensure you remain punctual.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                    items(reminders, key = { it.id }) { reminder ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DevilRedDim),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔔", fontSize = 16.sp)
                            }

                            Column {
                                Text(
                                    text = reminder.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "📅 ${formatShortDate(reminder.date)} @ ${reminder.time} · ${reminder.repeatType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGrey,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Switch(
                                checked = reminder.isEnabled,
                                onCheckedChange = { viewModel.updateReminder(reminder.copy(isEnabled = it)) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DevilRed,
                                    uncheckedThumbColor = TextGrey,
                                    uncheckedTrackColor = DarkSurface3
                                )
                            )

                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Reminder",
                                tint = TextDarkGrey,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.deleteReminderById(reminder.id) }
                            )
                        }
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
                }
            }
        }

        FloatingActionButton(
            onClick = onShowDialog,
            containerColor = DevilRed,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 20.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Reminder")
        }
    }
}

@Composable
fun EmptyStateView(icon: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 56.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGrey,
            textAlign = TextAlign.Center
        )
    }
}

// --- ADD REMINDER POPUP DIALOG ---

@Composable
fun AddReminderDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(getTodayDateStr()) }
    var time by remember { mutableStateOf("09:00") }
    var repeatType by remember { mutableStateOf("Once") }

    val repeats = listOf("Once", "Daily", "Weekly", "Monthly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔔 New Reminder", fontWeight = FontWeight.Bold, color = TextWhite) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder description") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DevilRed,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:MM)") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DevilRed,
                            unfocusedBorderColor = BorderColor
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Repeat Cycle", style = MaterialTheme.typography.bodySmall, color = TextGrey)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeats.forEach { r ->
                        val isSelected = repeatType == r
                        val scrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) DevilRedDim else DarkSurface2)
                                .border(1.dp, if (isSelected) DevilRed else BorderColor, RoundedCornerShape(10.dp))
                                .clickable { repeatType = r }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = r,
                                color = if (isSelected) DevilRed else TextWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        viewModel.addReminder(title, date, time, repeatType)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SCHEDULE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextGrey)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
