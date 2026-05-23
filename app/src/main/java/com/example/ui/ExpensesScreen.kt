package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun ExpensesScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    var activeCategoryFilter by remember { mutableStateOf("All") }
    var calendarInstance by remember { mutableStateOf(Calendar.getInstance()) }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "Food", "Transport", "Shopping", "Bills", "Health", "Fun", "Other")

    // Current Month Formatting for Query (e.g. "2026-05")
    val currentMonthKey = remember(calendarInstance) {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendarInstance.time)
    }
    val currentMonthLabel = remember(calendarInstance) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(calendarInstance.time)
    }

    // Filtered lists
    val monthlyExpenses = remember(expenses, currentMonthKey) {
        expenses.filter { it.date.startsWith(currentMonthKey) }
    }

    val finalFilteredExpenses = remember(monthlyExpenses, activeCategoryFilter) {
        if (activeCategoryFilter == "All") {
            monthlyExpenses
        } else {
            monthlyExpenses.filter { it.category.equals(activeCategoryFilter, ignoreCase = true) }
        }
    }

    val totalSpent = remember(monthlyExpenses) {
        monthlyExpenses.sumOf { it.amount }
    }
    
    val budget = settings.monthlyBudget
    val currency = settings.currencySymbol

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply { time = calendarInstance.time }
                        newCal.add(Calendar.MONTH, -1)
                        calendarInstance = newCal
                    }
                ) {
                    Text("‹", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = currentMonthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )

                IconButton(
                    onClick = {
                        val newCal = Calendar.getInstance().apply { time = calendarInstance.time }
                        newCal.add(Calendar.MONTH, 1)
                        calendarInstance = newCal
                    }
                ) {
                    Text("›", color = TextWhite, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Total Spent Outstanding Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DevilRedDim)
                    .border(1.dp, DevilRed.copy(0.25f), RoundedCornerShape(16.dp))
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$currency${formatAmount(totalSpent)}",
                        color = DevilRed,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Total spent this month · ${monthlyExpenses.size} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGrey,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Warning Progress bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val ratio = if (budget > 0) (totalSpent / budget).toFloat().coerceIn(0f, 1f) else 0f
                    val spendPercent = (ratio * 100).toInt()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$spendPercent% of budget used",
                            color = if (spendPercent >= 80) DevilRed else AppleGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$currency${formatAmount((budget - totalSpent).coerceAtLeast(0.0))} left",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGrey,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
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
                }
            }

            // Horizontally Scroll Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = activeCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) DevilRedDim else DarkSurface2)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) DevilRed else BorderColor,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { activeCategoryFilter = cat }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(getEmojiForCategory(cat), fontSize = 11.sp)
                            Text(
                                text = cat,
                                color = if (isSelected) DevilRed else TextGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Expenses List
            if (finalFilteredExpenses.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("💸", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "কোনো খরচ নেই। এখনই যোগ করুন! 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGrey,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(finalFilteredExpenses, key = { it.id }) { item ->
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
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSurface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(getEmojiForCategory(item.category), fontSize = 20.sp)
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DarkSurface3)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.category,
                                                color = TextGrey,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = formatShortDate(item.date),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDarkGrey
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "$currency${formatAmount(item.amount)}",
                                    color = DevilRed,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                // Easy delete touch target
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.deleteExpense(item) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Expense",
                                        tint = TextDarkGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = BorderColor, thickness = 1.dp)
                    }
                }
            }
        }
        }

        // Add Floating Action Button on screen
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 85.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DevilRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
            }
        }

        if (showAddDialog) {
            AddExpenseDialog(viewModel = viewModel, onDismiss = { showAddDialog = false })
        }
    }
}

fun formatShortDate(dateStr: String): String {
    return try {
        val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        SimpleDateFormat("d MMM", Locale.getDefault()).format(originalDate ?: Date())
    } catch (e: Exception) {
        dateStr
    }
}
