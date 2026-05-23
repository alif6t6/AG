package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToTab: (String) -> Unit,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean
) {
    val expenses by viewModel.expenses.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val chatLogs by viewModel.chatLogs.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    val chartData = remember(expenses) {
        getLast7DaysSpending(expenses)
    }

    val recentActivities = remember(expenses, tasks, settings.currencySymbol) {
        val itemsList = mutableListOf<ActivityItem>()
        val currency = settings.currencySymbol
        
        // Keep up to 5 expenses
        expenses.take(5).forEach {
            val formattedDate = try {
                val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date)
                SimpleDateFormat("d MMM", Locale.getDefault()).format(originalDate ?: Date())
            } catch (e: Exception) {
                it.date
            }
            itemsList.add(
                ActivityItem(
                    id = "exp_${it.id}",
                    icon = getEmojiForCategory(it.category),
                    title = it.title,
                    subtitle = "${it.category} · $formattedDate",
                    value = "-$currency${formatAmount(it.amount)}",
                    isExpense = true
                )
            )
        }

        // Keep up to 3 completed tasks
        tasks.filter { it.isCompleted }.take(3).forEach {
            itemsList.add(
                ActivityItem(
                    id = "task_${it.id}",
                    icon = "✅",
                    title = it.title,
                    subtitle = "Task completed Successfully",
                    value = "Done",
                    isExpense = false
                )
            )
        }
        itemsList.shuffled(Random(123)) // keep stable order
    }

    var showFabMenu by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }

    // Aggregate statistics
    val currency = settings.currencySymbol
    val budget = settings.monthlyBudget

    // Current month filter (YYYY-MM)
    val currentMonthKey = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    val spendThisMonth = remember(expenses) {
        expenses.filter { it.date.startsWith(currentMonthKey) }.sumOf { it.amount }
    }

    val budgetLeft = (budget - spendThisMonth).coerceAtLeast(0.0)

    val completedTasks = remember(tasks) { tasks.filter { it.isCompleted }.size }
    val totalTasks = tasks.size
    val notesCount = notes.size

    val todayKey = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val aiQueriesToday = remember(chatLogs) {
        chatLogs.filter { log ->
            log.sender == "ai" && log.timestamp >= getStartOfToday()
        }.size
    }

    // Dynamic greeting
    val welcomeText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Top App Bar Style Greeting
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$welcomeText, ${settings.userName} 👋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Your AI-Powered Life OS",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGrey
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // AI Badge style
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AppleGreen)
                                )
                                Text(
                                    text = "Gemini AI",
                                    color = TextWhite,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Theme Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                .clickable { onToggleTheme() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Default.BrightnessHigh else Icons.Default.Brightness4,
                                contentDescription = "Toggle Theme",
                                tint = if (darkTheme) DevilRed else TextGrey,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Budget Progress Bar
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Budget Progress",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val percent = if (budget > 0) ((spendThisMonth / budget) * 100).toInt() else 0
                            Text(
                                text = "$percent%",
                                color = if (percent >= 80) DevilRed else AppleGreen,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom progress track
                        val ratio = if (budget > 0) (spendThisMonth / budget).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(BorderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(ratio)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        when {
                                            ratio >= 0.8f -> DevilRed
                                            ratio >= 0.5f -> AppleYellow
                                            else -> AppleGreen
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: $currency${formatAmount(spendThisMonth)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGrey,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Budget: $currency${formatAmount(budget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGrey,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Overview Header
            item {
                Text(
                    text = "OVERVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
                )
            }

            // Horizontal Scroll Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard(
                        icon = "💸",
                        title = "Total Spent",
                        value = "$currency${formatAmount(spendThisMonth)}",
                        subtitle = "This Month",
                        isAccent = true
                    )
                    SummaryCard(
                        icon = "🎯",
                        title = "Budget Left",
                        value = "$currency${formatAmount(budgetLeft)}",
                        subtitle = "${(budgetLeft / (if (budget > 0) budget else 1.0) * 100).toInt()}% remains",
                        isAccent = false
                    )
                    SummaryCard(
                        icon = "✅",
                        title = "Tasks Done",
                        value = "$completedTasks/$totalTasks",
                        subtitle = "${totalTasks - completedTasks} pending",
                        isAccent = false
                    )
                    SummaryCard(
                        icon = "📝",
                        title = "Notes count",
                        value = "$notesCount",
                        subtitle = "Saved drafts",
                        isAccent = false
                    )
                    SummaryCard(
                        icon = "⚡",
                        title = "AI Queries",
                        value = "$aiQueriesToday",
                        subtitle = "Today",
                        isAccent = false
                    )
                }
            }

            // Chart Section
            item {
                Text(
                    text = "LAST 7 DAYS SPENDING",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGrey,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Daily Spending Details",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGrey,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            SpendingBarChart(chartData)
                        }
                    }
                }
            }

            // Recent Activity Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ACTIVITY",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGrey,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // recentActivities computed at top level

            if (recentActivities.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("👋", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "কোনো খরচ নেই। এখনই যোগ করুন! 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGrey,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(recentActivities, key = { it.id }) { activity ->
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(DarkSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activity.icon, fontSize = 17.sp)
                            }

                            Column {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activity.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGrey
                                )
                            }
                        }

                        Text(
                            text = activity.value,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (activity.isExpense) DevilRed else AppleGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
            }
        }
        }

        // Quick Action FAB Speed Dial in corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = { showFabMenu = !showFabMenu },
                containerColor = DevilRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Actions Menu",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Custom FAB speed-dial bottom sheet style or clean Dropdown menu
        if (showFabMenu) {
            AlertDialog(
                onDismissRequest = { showFabMenu = false },
                title = { Text("Quick Actions 😈", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                showFabMenu = false
                                showAddExpenseDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface2),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("💸 Add Expense", color = TextWhite)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                showAddTaskDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface2),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("✅ Add Task", color = TextWhite)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                showAddNoteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurface2),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("📝 Add Note", color = TextWhite)
                        }
                        Button(
                            onClick = {
                                showFabMenu = false
                                onNavigateToTab("agent")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DevilRed),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("😈 Ask AI Agent", color = Color.White)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFabMenu = false }) {
                        Text("CANCEL", color = DevilRed)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // --- Dialog Dialog Popups ---
        if (showAddExpenseDialog) {
            AddExpenseDialog(viewModel = viewModel, onDismiss = { showAddExpenseDialog = false })
        }
        if (showAddTaskDialog) {
            AddTaskDialog(viewModel = viewModel, onDismiss = { showAddTaskDialog = false })
        }
        if (showAddNoteDialog) {
            AddNoteDialog(viewModel = viewModel, onDismiss = { showAddNoteDialog = false })
        }
    }
}

@Composable
fun SummaryCard(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    isAccent: Boolean
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .border(
                1.dp,
                if (isAccent) DevilRed.copy(0.4f) else BorderColor,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isAccent) DevilRedDim else DarkSurface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(icon, fontSize = 20.sp, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                text = value,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                color = TextGrey,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = AppleGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun SpendingBarChart(data: List<DaySpending>) {
    val maxVal = remember(data) { (data.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { day ->
            val ratio = (day.amount / maxVal).toFloat().coerceIn(0.05f, 1f)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                // Label
                if (day.amount > 0) {
                    Text(
                        text = "${day.amount.toInt()}",
                        color = DevilRed,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Physical bar drawn
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight(0.75f * ratio)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (day.isToday) DevilRed else DarkSurface2)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Day name
                Text(
                    text = day.dayLabel,
                    color = if (day.isToday) DevilRed else TextGrey,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// Helpers for data analysis
data class DaySpending(val dayLabel: String, val amount: Double, val isToday: Boolean)
data class ActivityItem(val id: String, val icon: String, val title: String, val subtitle: String, val value: String, val isExpense: Boolean)

fun formatAmount(amount: Double): String {
    return if (amount % 1 == 0.0) {
        String.format(Locale.US, "%,.0f", amount)
    } else {
        String.format(Locale.US, "%,.2f", amount)
    }
}

fun getStartOfToday(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

fun getLast7DaysSpending(expenses: List<Expense>): List<DaySpending> {
    val result = mutableListOf<DaySpending>()
    val sdfDay = SimpleDateFormat("EEE", Locale.US)
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val dateString = sdfDate.format(cal.time)
        val dayLabel = sdfDay.format(cal.time)

        val totalForDay = expenses
            .filter { it.date == dateString }
            .sumOf { it.amount }

        result.add(DaySpending(dayLabel, totalForDay, i == 0))
    }
    return result
}

fun getEmojiForCategory(category: String): String {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> "🍔"
        "transport" -> "🚗"
        "shopping" -> "🛍️"
        "bills" -> "⚡"
        "health" -> "🏥"
        "fun" -> "🎮"
        else -> "💰"
    }
}
