package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }
    var date by remember { mutableStateOf(getTodayDateStr()) }
    var note by remember { mutableStateOf("") }

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Health", "Fun", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💸 New Expense", fontWeight = FontWeight.Bold, color = TextWhite) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Expense Title (e.g. Dinner)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount ৳") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.bodyMedium, color = TextGrey)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .horizontalScroll(scrollState)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category == cat
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) DevilRedDim else DarkSurface2)
                                    .border(
                                        1.dp,
                                        if (isSelected) DevilRed else BorderColor,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { category = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(getEmojiForCategory(cat), fontSize = 12.sp)
                                    Text(cat, color = if (isSelected) DevilRed else TextWhite, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Optional Memo Note") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (title.isNotEmpty() && amt > 0) {
                        viewModel.addExpense(title, amt, category, date, note)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun AddTaskDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var dueDate by remember { mutableStateOf(getTodayDateStr()) }
    var tag by remember { mutableStateOf("Personal") }

    val priorities = listOf("HIGH", "MEDIUM", "LOW")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✅ New Task", fontWeight = FontWeight.Bold, color = TextWhite) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", style = MaterialTheme.typography.bodyMedium, color = TextGrey)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    priorities.forEach { prio ->
                        val isSelected = priority == prio
                        val color = when (prio) {
                            "HIGH" -> DevilRed
                            "MEDIUM" -> AppleYellow
                            else -> AppleGreen
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) color.copy(0.15f) else DarkSurface2)
                                .border(1.dp, if (isSelected) color else BorderColor, RoundedCornerShape(12.dp))
                                .clickable { priority = prio }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prio,
                                color = if (isSelected) color else TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag / Category (e.g. Career)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        viewModel.addTask(title, priority, dueDate, tag)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ADD TASK", color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun AddNoteDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF3B30") }

    val dots = listOf("#FF3B30", "#0A84FF", "#FFD60A", "#30D158", "#BF5AF2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📝 Write Note", fontWeight = FontWeight.Bold, color = TextWhite) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Compose body, ideas, list...") },
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DevilRed,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Pick Note Color Dot", style = MaterialTheme.typography.bodySmall, color = TextGrey)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dots.forEach { hex ->
                        val isSelected = selectedColor == hex
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    2.dp,
                                    if (isSelected) TextWhite else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotEmpty()) {
                        viewModel.addNote(title, content, getTodayDateStr(), selectedColor)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("SAVE NOTE", color = Color.White, fontWeight = FontWeight.Bold)
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

fun getTodayDateStr(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
